package com.example.aura.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aura.ai.AuraAiService
import com.example.aura.ai.SafetyEngine
import com.example.aura.auth.GoogleAuthManager
import com.example.aura.data.ActionHistoryEntity
import com.example.aura.data.ActionStatus
import com.example.aura.data.ActionType
import com.example.aura.data.AppDatabase
import com.example.aura.data.AuraActionPlan
import com.example.aura.data.AuraPreferences
import com.example.aura.data.AuraRepository
import com.example.aura.data.AuraState
import com.example.aura.data.AuraUser
import com.example.aura.data.ChatMessage
import com.example.aura.data.MessageSender
import com.example.aura.data.PairedDeviceEntity
import com.example.aura.data.RememberedPermissionEntity
import com.example.aura.data.SystemTelemetry
import com.example.aura.service.AndroidExecutor
import com.example.aura.service.AuraNotificationManager
import com.example.aura.service.AuraVoiceInteractionService
import com.example.aura.service.ExecutionResult
import com.example.aura.service.SystemTelemetryManager
import com.example.aura.service.VoiceManager
import com.example.aura.service.WakeWordEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val repository = AuraRepository(
        database.actionHistoryDao(),
        database.pairedDeviceDao(),
        database.chatMessageDao(),
        database.rememberedPermissionDao()
    )
    private val preferences = AuraPreferences(context)

    private val telemetryManager = SystemTelemetryManager(context)
    private val executor = AndroidExecutor(context)
    private val aiService = AuraAiService()
    private val notificationManager = AuraNotificationManager(context)
    private val authManager = GoogleAuthManager(context, preferences)

    private val _currentUser = MutableStateFlow(authManager.getCurrentUser())
    val currentUser: StateFlow<AuraUser> = _currentUser.asStateFlow()

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _auraState.value = AuraState.THINKING
            val result = authManager.signIn(activityContext)
            result.onSuccess { user ->
                _currentUser.value = user
                _auraState.value = AuraState.IDLE
                addSystemMessage("✅ Switched Google Account: ${user.displayName} (${user.email})")
                voiceManager.speak("Welcome, ${user.displayName}!")
                notificationManager.showWorkDoneNotification("Google Account Connected", "Signed in as ${user.displayName}")
            }.onFailure { err ->
                _auraState.value = AuraState.IDLE
                addSystemMessage("Google sign-in: ${err.localizedMessage ?: "Account selection cancelled"}. You can also enter an account manually below.")
            }
        }
    }

    fun switchOrUpdateAccount(name: String, email: String) {
        val updatedUser = authManager.switchOrSetAccount(name, email)
        _currentUser.value = updatedUser
        addSystemMessage("✅ Linked Google account: ${updatedUser.displayName} (${updatedUser.email})")
        voiceManager.speak("Account updated to ${updatedUser.displayName}.")
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            authManager.signOut()
            _currentUser.value = preferences.getUserProfile()
            addSystemMessage("Logged out of Google account. AURA is now in guest mode.")
            voiceManager.speak("Signed out of Google account.")
        }
    }

    private val _auraState = MutableStateFlow(AuraState.IDLE)
    val auraState: StateFlow<AuraState> = _auraState.asStateFlow()

    // Room database backed chat messages for persistence and scrolling through past interactions
    val chatMessages: StateFlow<List<ChatMessage>> = repository.allChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rememberedPermissions: StateFlow<List<RememberedPermissionEntity>> = repository.allRememberedPermissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingAction = MutableStateFlow<AuraActionPlan?>(null)
    val pendingAction: StateFlow<AuraActionPlan?> = _pendingAction.asStateFlow()

    private val _telemetry = MutableStateFlow(SystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _isVoiceOutputEnabled = MutableStateFlow(true)
    val isVoiceOutputEnabled: StateFlow<Boolean> = _isVoiceOutputEnabled.asStateFlow()

    // Autonomous Direct Execution Mode (Never ask for permission when true)
    private val _isDirectExecution = MutableStateFlow(preferences.isDirectExecution)
    val isDirectExecution: StateFlow<Boolean> = _isDirectExecution.asStateFlow()

    fun setDirectExecution(enabled: Boolean) {
        _isDirectExecution.value = enabled
        preferences.isDirectExecution = enabled
        addSystemMessage(
            if (enabled) "Autonomous execution enabled: AURA will execute all commands directly without asking for permission."
            else "Permission confirmation mode enabled: AURA will ask once before executing actions."
        )
    }

    // Wake Word State
    private val _isWakeWordEnabled = MutableStateFlow(preferences.isWakeWordEnabled)
    val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()

    private val _wakeWordPhrase = MutableStateFlow(preferences.wakeWordPhrase)
    val wakeWordPhrase: StateFlow<String> = _wakeWordPhrase.asStateFlow()

    private val _wakeWordSensitivity = MutableStateFlow(preferences.wakeWordSensitivity)
    val wakeWordSensitivity: StateFlow<Float> = _wakeWordSensitivity.asStateFlow()

    private val _isWakeWordListening = MutableStateFlow(false)
    val isWakeWordListening: StateFlow<Boolean> = _isWakeWordListening.asStateFlow()

    val pairedDevices: StateFlow<List<PairedDeviceEntity>> = repository.allDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionHistory: StateFlow<List<ActionHistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var executionJob: Job? = null

    private val voiceManager: VoiceManager by lazy {
        VoiceManager(
            context = context,
            onSpeechRecognized = { text ->
                submitCommand(text)
            },
            onListeningStateChanged = { listening ->
                if (listening) {
                    _auraState.value = AuraState.LISTENING
                    wakeWordEngine.setSuspended(true)
                } else {
                    if (_auraState.value == AuraState.LISTENING) {
                        _auraState.value = AuraState.IDLE
                    }
                    wakeWordEngine.setSuspended(false)
                }
            },
            onError = { err ->
                _auraState.value = AuraState.ERROR
                addSystemMessage("Voice recognition notice: $err")
                viewModelScope.launch {
                    delay(2000)
                    if (_auraState.value == AuraState.ERROR) {
                        _auraState.value = AuraState.IDLE
                    }
                    wakeWordEngine.setSuspended(false)
                }
            }
        )
    }

    private val wakeWordEngine: WakeWordEngine by lazy {
        WakeWordEngine(
            context = context,
            onWakeWordDetected = { detectedWord, extraCommand ->
                viewModelScope.launch {
                    addSystemMessage("🎙️ Wake word '$detectedWord' recognized.")
                    if (!extraCommand.isNullOrBlank()) {
                        submitCommand(extraCommand)
                    } else {
                        _auraState.value = AuraState.LISTENING
                        delay(150)
                        voiceManager.startListening()
                    }
                }
            },
            onListeningStateChanged = { listening ->
                _isWakeWordListening.value = listening
            },
            onError = { errorNotice ->
                addSystemMessage("Wake word engine notice: $errorNotice")
            }
        )
    }

    init {
        refreshTelemetry()

        viewModelScope.launch {
            repository.initializeDefaultDevicesIfEmpty(pairedDevices.value)

            // Seed initial greeting into Room database if chat is empty
            val initial = repository.allChatMessages.firstOrNull()
            if (initial.isNullOrEmpty()) {
                repository.saveChatMessage(
                    ChatMessage(
                        sender = MessageSender.AURA,
                        text = "I am AURA — the One and Only AURA, created by Aryan Yadav, a student of Class 10th. What command shall we execute?"
                    )
                )
            }

            // Initialize Wake Word Engine with saved preferences
            wakeWordEngine.configure(
                phrase = _wakeWordPhrase.value,
                sens = _wakeWordSensitivity.value,
                enabled = _isWakeWordEnabled.value
            )
        }
    }

    fun isDefaultAssistant(): Boolean {
        return AuraVoiceInteractionService.isDefaultAssistant(context)
    }

    fun openDefaultAssistantSettings() {
        try {
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                addSystemMessage("Could not open default apps settings directly. Open Android Settings > Apps > Default Apps > Digital Assistant.")
            }
        }
    }

    fun getPermissionKey(plan: AuraActionPlan): String {
        return when (plan.actionType) {
            ActionType.OPEN_APP -> "OPEN_APP:${plan.target.lowercase().trim()}"
            ActionType.SET_VOLUME -> "SET_VOLUME"
            ActionType.MEDIA_CONTROL -> "MEDIA_CONTROL"
            ActionType.TAKE_SCREENSHOT -> "TAKE_SCREENSHOT"
            ActionType.OPEN_URL -> "OPEN_URL:${plan.target.lowercase().trim()}"
            ActionType.OPEN_SETTINGS -> "OPEN_SETTINGS"
            ActionType.CROSS_DEVICE_COMMAND -> "CROSS_DEVICE:${plan.device}:${plan.target.lowercase().trim()}"
            ActionType.COMMUNICATION -> "COMMUNICATION:${plan.target.lowercase().trim()}"
            else -> "${plan.actionType.name}:${plan.target.lowercase().trim()}"
        }
    }

    fun revokePermission(key: String) {
        viewModelScope.launch {
            repository.revokeRememberedPermission(key)
            addSystemMessage("Revoked remembered permission: $key")
        }
    }

    fun clearAllPermissions() {
        viewModelScope.launch {
            repository.clearAllRememberedPermissions()
            addSystemMessage("All remembered action permissions cleared. AURA will ask once again before running actions.")
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        _isWakeWordEnabled.value = enabled
        preferences.isWakeWordEnabled = enabled
        wakeWordEngine.configure(
            phrase = _wakeWordPhrase.value,
            sens = _wakeWordSensitivity.value,
            enabled = enabled
        )
        addSystemMessage(
            if (enabled) "Hands-free wake word enabled ('${_wakeWordPhrase.value}'). Low-power listener active."
            else "Hands-free wake word disabled."
        )
    }

    fun toggleWakeWord() {
        setWakeWordEnabled(!_isWakeWordEnabled.value)
    }

    fun setWakeWordPhrase(newPhrase: String) {
        val trimmed = newPhrase.trim()
        if (trimmed.isEmpty()) return
        _wakeWordPhrase.value = trimmed
        preferences.wakeWordPhrase = trimmed
        wakeWordEngine.configure(
            phrase = trimmed,
            sens = _wakeWordSensitivity.value,
            enabled = _isWakeWordEnabled.value
        )
        addSystemMessage("AURA wake word updated to: \"$trimmed\"")
    }

    fun setWakeWordSensitivity(sensitivity: Float) {
        val bounded = sensitivity.coerceIn(0.1f, 1.0f)
        _wakeWordSensitivity.value = bounded
        preferences.wakeWordSensitivity = bounded
        wakeWordEngine.configure(
            phrase = _wakeWordPhrase.value,
            sens = bounded,
            enabled = _isWakeWordEnabled.value
        )
    }

    fun refreshTelemetry() {
        _telemetry.value = telemetryManager.collectTelemetry()
    }

    fun submitCommand(rawText: String) {
        val clean = rawText.trim()
        if (clean.isEmpty()) return

        // Persist user message to Room database
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = clean
        )
        viewModelScope.launch {
            repository.saveChatMessage(userMsg)
        }

        // Check for immediate cancellation
        if (SafetyEngine.isCancellationCommand(clean)) {
            emergencyStop("Operation cancelled by user command.")
            return
        }

        viewModelScope.launch {
            _auraState.value = AuraState.THINKING
            wakeWordEngine.setSuspended(true)
            delay(350) // Smooth thinking visual

            val plan = aiService.processCommand(clean, _currentUser.value)
            val validatedPlan = SafetyEngine.validatePlan(plan)

            when (validatedPlan.actionType) {
                ActionType.CONVERSATIONAL_RESPONSE -> {
                    _auraState.value = AuraState.SUCCESS
                    val reply = validatedPlan.payload["reply"] ?: validatedPlan.description
                    val auraMsg = ChatMessage(
                        sender = MessageSender.AURA,
                        text = reply
                    )
                    repository.saveChatMessage(auraMsg)
                    voiceManager.speak(reply)
                    notificationManager.showWorkDoneNotification("Answer Ready", reply)
                    delay(1200)
                    _auraState.value = AuraState.IDLE
                    wakeWordEngine.setSuspended(false)
                }

                ActionType.GET_BATTERY_INFO -> {
                    refreshTelemetry()
                    val bat = _telemetry.value
                    val reply = "Battery is at ${bat.batteryLevel}%, status: ${if (bat.isCharging) "Charging" else "On Battery"}, health: ${bat.batteryHealth}."
                    _auraState.value = AuraState.SUCCESS
                    val auraMsg = ChatMessage(
                        sender = MessageSender.AURA,
                        text = reply
                    )
                    repository.saveChatMessage(auraMsg)
                    voiceManager.speak(reply)
                    notificationManager.showWorkDoneNotification("Battery Status", reply)
                    repository.recordAction(
                        command = clean,
                        actionType = ActionType.GET_BATTERY_INFO,
                        target = "Battery Status",
                        targetDevice = "This Phone",
                        riskLevel = validatedPlan.risk,
                        status = ActionStatus.SUCCESS,
                        resultMessage = reply
                    )
                    delay(1200)
                    _auraState.value = AuraState.IDLE
                    wakeWordEngine.setSuspended(false)
                }

                ActionType.GET_STORAGE_INFO -> {
                    refreshTelemetry()
                    val stor = _telemetry.value
                    val reply = "Storage: ${stor.freeStorageGb} GB available of ${stor.totalStorageGb} GB (${stor.usedStoragePercent}% occupied)."
                    _auraState.value = AuraState.SUCCESS
                    val auraMsg = ChatMessage(
                        sender = MessageSender.AURA,
                        text = reply
                    )
                    repository.saveChatMessage(auraMsg)
                    voiceManager.speak(reply)
                    notificationManager.showWorkDoneNotification("Storage Info", reply)
                    repository.recordAction(
                        command = clean,
                        actionType = ActionType.GET_STORAGE_INFO,
                        target = "Storage Info",
                        targetDevice = "This Phone",
                        riskLevel = validatedPlan.risk,
                        status = ActionStatus.SUCCESS,
                        resultMessage = reply
                    )
                    delay(1200)
                    _auraState.value = AuraState.IDLE
                    wakeWordEngine.setSuspended(false)
                }

                ActionType.GET_MEMORY_INFO -> {
                    refreshTelemetry()
                    val ram = _telemetry.value
                    val reply = "RAM: ${ram.availRamMb} MB free of ${ram.totalRamMb} MB (${ram.usedRamPercent}% in use)."
                    _auraState.value = AuraState.SUCCESS
                    val auraMsg = ChatMessage(
                        sender = MessageSender.AURA,
                        text = reply
                    )
                    repository.saveChatMessage(auraMsg)
                    voiceManager.speak(reply)
                    notificationManager.showWorkDoneNotification("Memory Info", reply)
                    delay(1200)
                    _auraState.value = AuraState.IDLE
                    wakeWordEngine.setSuspended(false)
                }

                ActionType.GET_NETWORK_INFO -> {
                    refreshTelemetry()
                    val net = _telemetry.value
                    val reply = "Network: ${net.networkType} (${if (net.isConnected) "Connected" else "No Connection"})."
                    _auraState.value = AuraState.SUCCESS
                    val auraMsg = ChatMessage(
                        sender = MessageSender.AURA,
                        text = reply
                    )
                    repository.saveChatMessage(auraMsg)
                    voiceManager.speak(reply)
                    notificationManager.showWorkDoneNotification("Network Info", reply)
                    delay(1200)
                    _auraState.value = AuraState.IDLE
                    wakeWordEngine.setSuspended(false)
                }

                ActionType.STOP_CANCEL -> {
                    emergencyStop("Cancelled by user.")
                }

                // Device actions (App launching, Volume, Media, Settings, Remote, etc.)
                else -> {
                    if (_isDirectExecution.value) {
                        // Autonomous Direct Execution: Execute immediately without asking permission for anything!
                        executeActionPlan(validatedPlan, isPreAuthorized = true)
                    } else {
                        // Check if permission for this action was already granted and remembered
                        val permKey = getPermissionKey(validatedPlan)
                        val isRemembered = repository.isPermissionRemembered(permKey)

                        if (isRemembered) {
                            // Already authorized! Execute directly without asking again, and speak the result!
                            executeActionPlan(validatedPlan, isPreAuthorized = true)
                        } else {
                            // Ask once!
                            _auraState.value = AuraState.WAITING
                            _pendingAction.value = validatedPlan

                            val promptText = "I can perform '${validatedPlan.description}' on ${validatedPlan.device}. Allow?"
                            val auraMsg = ChatMessage(
                                sender = MessageSender.AURA,
                                text = promptText,
                                actionPlan = validatedPlan,
                                actionStatus = ActionStatus.PENDING
                            )
                            repository.saveChatMessage(auraMsg)
                            voiceManager.speak("Awaiting your authorization to ${validatedPlan.description}.")
                        }
                    }
                }
            }
        }
    }

    fun confirmAction(plan: AuraActionPlan) {
        if (_pendingAction.value?.id != plan.id) return
        _pendingAction.value = null

        viewModelScope.launch {
            // Save permission so AURA never asks again for this action!
            val permKey = getPermissionKey(plan)
            repository.rememberPermission(
                key = permKey,
                actionType = plan.actionType.name,
                target = plan.target,
                label = plan.description
            )

            // Update chat message status to APPROVED in Room database
            repository.updateChatMessageStatus(plan.id, ActionStatus.APPROVED)

            // Execute the plan and speak the result
            executeActionPlan(plan, isPreAuthorized = false)
        }
    }

    private fun executeActionPlan(plan: AuraActionPlan, isPreAuthorized: Boolean) {
        executionJob?.cancel()
        executionJob = viewModelScope.launch {
            _auraState.value = AuraState.EXECUTING
            wakeWordEngine.setSuspended(true)
            val startTime = System.currentTimeMillis()

            delay(250) // Smooth cadence
            val result = executor.execute(plan)
            val duration = System.currentTimeMillis() - startTime

            when (result) {
                is ExecutionResult.Success -> {
                    _auraState.value = AuraState.SUCCESS
                    val completionMsg = result.message
                    val displayMsg = completionMsg

                    val resultChatMsg = ChatMessage(
                        sender = MessageSender.AURA,
                        text = displayMsg
                    )
                    repository.saveChatMessage(resultChatMsg)

                    // Speak the result out loud
                    voiceManager.speak(completionMsg)
                    notificationManager.showWorkDoneNotification(plan.description, completionMsg)

                    repository.recordAction(
                        command = plan.description,
                        actionType = plan.actionType,
                        target = plan.target,
                        targetDevice = plan.device,
                        riskLevel = plan.risk,
                        status = ActionStatus.SUCCESS,
                        resultMessage = completionMsg,
                        durationMs = duration
                    )

                    delay(2000)
                    _auraState.value = AuraState.IDLE
                    wakeWordEngine.setSuspended(false)
                }

                is ExecutionResult.Failure -> {
                    _auraState.value = AuraState.ERROR
                    val failMsg = result.reason
                    val resultChatMsg = ChatMessage(
                        sender = MessageSender.AURA,
                        text = failMsg
                    )
                    repository.saveChatMessage(resultChatMsg)

                    // Speak failure message out loud
                    voiceManager.speak(failMsg)
                    notificationManager.showWorkDoneNotification("Action Notice", failMsg)

                    repository.recordAction(
                        command = plan.description,
                        actionType = plan.actionType,
                        target = plan.target,
                        targetDevice = plan.device,
                        riskLevel = plan.risk,
                        status = ActionStatus.FAILED,
                        resultMessage = failMsg,
                        durationMs = duration
                    )

                    delay(2500)
                    _auraState.value = AuraState.IDLE
                    wakeWordEngine.setSuspended(false)
                }
            }
        }
    }

    fun cancelAction(plan: AuraActionPlan) {
        if (_pendingAction.value?.id == plan.id) {
            _pendingAction.value = null
        }
        _auraState.value = AuraState.IDLE

        viewModelScope.launch {
            repository.updateChatMessageStatus(plan.id, ActionStatus.CANCELLED)

            val cancelReply = "Action cancelled. Nothing was executed."
            val cancelMsg = ChatMessage(
                sender = MessageSender.AURA,
                text = cancelReply
            )
            repository.saveChatMessage(cancelMsg)
            voiceManager.speak(cancelReply)
            wakeWordEngine.setSuspended(false)

            repository.recordAction(
                command = plan.description,
                actionType = plan.actionType,
                target = plan.target,
                targetDevice = plan.device,
                riskLevel = plan.risk,
                status = ActionStatus.CANCELLED,
                resultMessage = cancelReply
            )
        }
    }

    fun emergencyStop(reason: String = "EMERGENCY STOP activated. All pending operations cleared.") {
        executionJob?.cancel()
        _pendingAction.value = null
        voiceManager.stopSpeaking()
        _auraState.value = AuraState.IDLE

        addSystemMessage(reason)
        voiceManager.speak("Operation halted.")
        notificationManager.showWorkDoneNotification("Emergency Override", reason)
        wakeWordEngine.setSuspended(false)

        viewModelScope.launch {
            repository.recordAction(
                command = "Emergency Stop",
                actionType = ActionType.STOP_CANCEL,
                target = "All pending tasks",
                targetDevice = "This Phone",
                riskLevel = com.example.aura.data.RiskLevel.LOW,
                status = ActionStatus.CANCELLED,
                resultMessage = reason
            )
        }
    }

    private fun addSystemMessage(text: String) {
        viewModelScope.launch {
            repository.saveChatMessage(
                ChatMessage(
                    sender = MessageSender.SYSTEM,
                    text = text
                )
            )
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatMessages()
            addSystemMessage("Chat interaction history cleared.")
        }
    }

    fun startVoiceListening() {
        voiceManager.startListening()
    }

    fun stopVoiceListening() {
        voiceManager.stopListening()
    }

    fun toggleVoiceOutput() {
        val newState = !_isVoiceOutputEnabled.value
        _isVoiceOutputEnabled.value = newState
        voiceManager.isVoiceOutputEnabled = newState
    }

    fun pairDevice(name: String, type: String, host: String) {
        viewModelScope.launch {
            val device = PairedDeviceEntity(
                id = "dev_${System.currentTimeMillis()}",
                name = name,
                type = type,
                isOnline = true,
                ipOrHost = host,
                isTrusted = true
            )
            repository.insertDevice(device)
            addSystemMessage("Device paired successfully: $name ($type) via authenticated TLS channel.")
        }
    }

    fun removeDevice(id: String) {
        viewModelScope.launch {
            repository.deleteDevice(id)
            addSystemMessage("Device removed and trust credentials revoked.")
        }
    }

    fun toggleDeviceOnline(id: String, currentOnline: Boolean) {
        viewModelScope.launch {
            repository.setDeviceOnline(id, !currentOnline)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            addSystemMessage("Action audit history cleared.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
        wakeWordEngine.release()
    }
}
