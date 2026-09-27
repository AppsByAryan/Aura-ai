package com.example.aura.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

class WakeWordEngine(
    private val context: Context,
    private val onWakeWordDetected: (wakePhrase: String, extraCommand: String?) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    private var isEnabled = false
    private var isRunning = false
    private var isSuspended = false

    var wakeWordPhrase: String = "Hey AURA"
        private set

    var sensitivity: Float = 0.7f
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var restartRunnable: Runnable? = null
    private var isCurrentlyListening = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun configure(phrase: String, sens: Float, enabled: Boolean) {
        this.wakeWordPhrase = if (phrase.isNotBlank()) phrase.trim() else "Hey AURA"
        this.sensitivity = sens.coerceIn(0.1f, 1.0f)
        this.isEnabled = enabled

        mainHandler.post {
            if (isEnabled && !isRunning) {
                start()
            } else if (!isEnabled && isRunning) {
                stop()
            }
        }
    }

    fun start() {
        if (!isEnabled || isRunning) return

        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            onError("Microphone permission required for wake word detection.")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition service unavailable on this device.")
            return
        }

        isRunning = true
        onListeningStateChanged(true)
        initAndListen()
    }

    fun stop() {
        isRunning = false
        cancelScheduledRestart()
        destroyRecognizer()
        onListeningStateChanged(false)
    }

    fun setSuspended(suspended: Boolean) {
        if (isSuspended == suspended) return
        isSuspended = suspended
        mainHandler.post {
            if (suspended) {
                cancelScheduledRestart()
                destroyRecognizer()
                onListeningStateChanged(false)
            } else if (isRunning && isEnabled) {
                onListeningStateChanged(true)
                initAndListen()
            }
        }
    }

    private fun initAndListen() {
        if (!isRunning || isSuspended) return

        cancelScheduledRestart()

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }

            isCurrentlyListening = true
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            destroyRecognizer()
            scheduleRestart(1200L)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isCurrentlyListening = false
            }

            override fun onError(error: Int) {
                isCurrentlyListening = false
                val delay = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 200L
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_CLIENT -> {
                        destroyRecognizer()
                        800L
                    }
                    else -> 500L
                }
                scheduleRestart(delay)
            }

            override fun onResults(results: Bundle?) {
                isCurrentlyListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val wakeMatch = findWakeMatch(matches)

                if (wakeMatch != null) {
                    triggerWake(wakeMatch.first, wakeMatch.second)
                } else {
                    scheduleRestart(150L)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val wakeMatch = findWakeMatch(partials)
                if (wakeMatch != null) {
                    triggerWake(wakeMatch.first, wakeMatch.second)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun findWakeMatch(phrases: List<String>?): Pair<String, String?>? {
        if (phrases.isNullOrEmpty()) return null

        val targetPhrase = wakeWordPhrase.lowercase().trim()
        val targetTokens = targetPhrase.split(" ").filter { it.isNotBlank() }

        for (phrase in phrases) {
            val clean = phrase.lowercase().trim()

            // 1. Direct contains target phrase: e.g. "hey aura what is the time"
            val indexInPhrase = clean.indexOf(targetPhrase)
            if (indexInPhrase >= 0) {
                val after = clean.substring(indexInPhrase + targetPhrase.length).trim()
                return Pair(wakeWordPhrase, if (after.isNotBlank()) after else null)
            }

            // 2. Contains "aura" or "ora" keyword
            val words = clean.split(" ")
            for (i in words.indices) {
                val w = words[i].replace(Regex("[^a-z]"), "")
                if (w == "aura" || w == "ora" || w == "ayra" || w == "aurora") {
                    val afterWords = words.subList(i + 1, words.size).joinToString(" ").trim()
                    return Pair(wakeWordPhrase, if (afterWords.isNotBlank()) afterWords else null)
                }
            }

            // 3. Normalized stripped match
            val stripped = clean.replace(Regex("[^a-z0-9]"), "")
            if (stripped.contains("aura") || stripped.contains("ora")) {
                return Pair(wakeWordPhrase, null)
            }
        }
        return null
    }

    private fun triggerWake(phrase: String, extraCommand: String?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120L)
            }
        } catch (_: Exception) {}

        cancelScheduledRestart()
        destroyRecognizer()
        onWakeWordDetected(phrase, extraCommand)
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!isRunning || isSuspended) return

        cancelScheduledRestart()
        restartRunnable = Runnable {
            if (isRunning && !isSuspended) {
                initAndListen()
            }
        }
        val adjusted = (delayMs * (1.1f - (sensitivity * 0.3f))).toLong().coerceAtLeast(100L)
        mainHandler.postDelayed(restartRunnable!!, adjusted)
    }

    private fun cancelScheduledRestart() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isCurrentlyListening = false
    }

    fun release() {
        stop()
        destroyRecognizer()
    }
}
