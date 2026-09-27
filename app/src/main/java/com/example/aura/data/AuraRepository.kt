package com.example.aura.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuraRepository(
    private val actionHistoryDao: ActionHistoryDao,
    private val pairedDeviceDao: PairedDeviceDao,
    private val chatMessageDao: ChatMessageDao,
    private val rememberedPermissionDao: RememberedPermissionDao
) {
    val allHistory: Flow<List<ActionHistoryEntity>> = actionHistoryDao.getAllHistory()
    val allDevices: Flow<List<PairedDeviceEntity>> = pairedDeviceDao.getAllDevices()
    val allChatMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()
        .map { list -> list.map { it.toChatMessage() } }
    val allRememberedPermissions: Flow<List<RememberedPermissionEntity>> = rememberedPermissionDao.getAllPermissions()

    suspend fun saveChatMessage(message: ChatMessage) {
        chatMessageDao.insertMessage(message.toEntity())
    }

    suspend fun updateChatMessageStatus(planId: String, status: ActionStatus) {
        chatMessageDao.updateStatusByPlanId(planId, status.name)
    }

    suspend fun clearChatMessages() {
        chatMessageDao.clearAllMessages()
    }

    suspend fun isPermissionRemembered(key: String): Boolean {
        return rememberedPermissionDao.isPermissionGranted(key)
    }

    suspend fun rememberPermission(key: String, actionType: String, target: String, label: String) {
        rememberedPermissionDao.grantPermission(
            RememberedPermissionEntity(
                permissionKey = key,
                actionType = actionType,
                target = target,
                label = label
            )
        )
    }

    suspend fun revokeRememberedPermission(key: String) {
        rememberedPermissionDao.revokePermission(key)
    }

    suspend fun clearAllRememberedPermissions() {
        rememberedPermissionDao.clearAll()
    }

    suspend fun recordAction(
        command: String,
        actionType: ActionType,
        target: String,
        targetDevice: String,
        riskLevel: RiskLevel,
        status: ActionStatus,
        resultMessage: String,
        durationMs: Long = 0L
    ): Long {
        val entity = ActionHistoryEntity(
            command = command,
            actionType = actionType.name,
            target = target,
            targetDevice = targetDevice,
            riskLevel = riskLevel.name,
            status = status.name,
            resultMessage = resultMessage,
            durationMs = durationMs
        )
        return actionHistoryDao.insert(entity)
    }

    suspend fun clearHistory() {
        actionHistoryDao.clearAll()
    }

    suspend fun insertDevice(device: PairedDeviceEntity) {
        pairedDeviceDao.insert(device)
    }

    suspend fun updateDevice(device: PairedDeviceEntity) {
        pairedDeviceDao.update(device)
    }

    suspend fun deleteDevice(id: String) {
        pairedDeviceDao.deleteById(id)
    }

    suspend fun setDeviceOnline(id: String, isOnline: Boolean) {
        pairedDeviceDao.updateStatus(id, isOnline, System.currentTimeMillis())
    }

    suspend fun initializeDefaultDevicesIfEmpty(deviceList: List<PairedDeviceEntity>) {
        if (deviceList.isEmpty()) {
            pairedDeviceDao.insert(
                PairedDeviceEntity(
                    id = "dev_this_phone",
                    name = "This Phone (Host)",
                    type = "PHONE",
                    isOnline = true,
                    ipOrHost = "127.0.0.1 (Local)",
                    isTrusted = true,
                    pairedAtTimestamp = System.currentTimeMillis() - 86400000L
                )
            )
            pairedDeviceDao.insert(
                PairedDeviceEntity(
                    id = "dev_laptop_primary",
                    name = "My Laptop",
                    type = "LAPTOP",
                    isOnline = true,
                    ipOrHost = "192.168.1.104 (TLS 1.3)",
                    isTrusted = true,
                    pairedAtTimestamp = System.currentTimeMillis() - 3600000L
                )
            )
        }
    }
}
