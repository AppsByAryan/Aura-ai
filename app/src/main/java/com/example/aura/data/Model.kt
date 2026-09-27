package com.example.aura.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class AuraState {
    IDLE,
    LISTENING,
    THINKING,
    WAITING,
    EXECUTING,
    SUCCESS,
    ERROR
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class ActionType {
    OPEN_APP,
    OPEN_URL,
    OPEN_SETTINGS,
    GET_BATTERY_INFO,
    GET_STORAGE_INFO,
    GET_MEMORY_INFO,
    GET_NETWORK_INFO,
    SET_VOLUME,
    MEDIA_CONTROL,
    TAKE_SCREENSHOT,
    CROSS_DEVICE_COMMAND,
    COMMUNICATION,
    STOP_CANCEL,
    CONVERSATIONAL_RESPONSE,
    UNSUPPORTED_ACTION
}

enum class ActionStatus {
    PENDING,
    APPROVED,
    CANCELLED,
    DENIED,
    SUCCESS,
    FAILED
}

enum class MessageSender {
    USER,
    AURA,
    SYSTEM
}

data class AuraActionPlan(
    val id: String = UUID.randomUUID().toString(),
    val actionType: ActionType,
    val target: String,
    val description: String,
    val device: String = "This Phone",
    val risk: RiskLevel = RiskLevel.LOW,
    val explanation: String,
    val requiresPermission: String? = null,
    val needsAccessibility: Boolean = false,
    val payload: Map<String, String> = emptyMap()
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionPlan: AuraActionPlan? = null,
    val actionStatus: ActionStatus? = null
)

@Entity(tableName = "action_history")
data class ActionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val actionType: String,
    val target: String,
    val targetDevice: String,
    val riskLevel: String,
    val status: String,
    val resultMessage: String,
    val durationMs: Long = 0L
)

@Entity(tableName = "paired_devices")
data class PairedDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // LAPTOP, DESKTOP, PHONE, TABLET
    val isOnline: Boolean,
    val ipOrHost: String,
    val isTrusted: Boolean = true,
    val pairedAtTimestamp: Long = System.currentTimeMillis(),
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

data class SystemTelemetry(
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val batteryHealth: String = "Good",
    val batteryTempC: Float = 25.0f,
    val totalStorageGb: Float = 0f,
    val freeStorageGb: Float = 0f,
    val usedStoragePercent: Int = 0,
    val totalRamMb: Long = 0L,
    val availRamMb: Long = 0L,
    val usedRamPercent: Int = 0,
    val networkType: String = "Disconnected",
    val isConnected: Boolean = false,
    val deviceModel: String = "",
    val androidVersion: String = "",
    val musicVolumePercent: Int = 0,
    val maxMusicVolume: Int = 15,
    val isAccessibilityActive: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sender: String, // USER, AURA, SYSTEM
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val planId: String? = null,
    val planActionType: String? = null,
    val planTarget: String? = null,
    val planDescription: String? = null,
    val planDevice: String? = null,
    val planRisk: String? = null,
    val planExplanation: String? = null,
    val actionStatus: String? = null
) {
    fun toChatMessage(): ChatMessage {
        val plan = if (planId != null && planActionType != null) {
            AuraActionPlan(
                id = planId,
                actionType = try { ActionType.valueOf(planActionType) } catch (_: Exception) { ActionType.CONVERSATIONAL_RESPONSE },
                target = planTarget ?: "",
                description = planDescription ?: "",
                device = planDevice ?: "This Phone",
                risk = try { RiskLevel.valueOf(planRisk ?: "LOW") } catch (_: Exception) { RiskLevel.LOW },
                explanation = planExplanation ?: ""
            )
        } else null

        return ChatMessage(
            id = id,
            sender = try { MessageSender.valueOf(sender) } catch (_: Exception) { MessageSender.AURA },
            text = text,
            timestamp = timestamp,
            actionPlan = plan,
            actionStatus = actionStatus?.let {
                try { ActionStatus.valueOf(it) } catch (_: Exception) { null }
            }
        )
    }
}

fun ChatMessage.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        sender = sender.name,
        text = text,
        timestamp = timestamp,
        planId = actionPlan?.id,
        planActionType = actionPlan?.actionType?.name,
        planTarget = actionPlan?.target,
        planDescription = actionPlan?.description,
        planDevice = actionPlan?.device,
        planRisk = actionPlan?.risk?.name,
        planExplanation = actionPlan?.explanation,
        actionStatus = actionStatus?.name
    )
}

@Entity(tableName = "remembered_permissions")
data class RememberedPermissionEntity(
    @PrimaryKey val permissionKey: String,
    val actionType: String,
    val target: String,
    val label: String,
    val grantedAtTimestamp: Long = System.currentTimeMillis()
)


