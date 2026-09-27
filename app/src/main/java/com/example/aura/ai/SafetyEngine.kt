package com.example.aura.ai

import com.example.aura.data.ActionType
import com.example.aura.data.AuraActionPlan
import com.example.aura.data.RiskLevel

object SafetyEngine {

    private val cancelPhrases = setOf(
        "cancel",
        "stop",
        "abort",
        "never mind",
        "nevermind",
        "don't do that",
        "dont do that",
        "halt",
        "exit",
        "disregard"
    )

    fun isCancellationCommand(input: String): Boolean {
        val clean = input.lowercase().trim().removeSuffix(".")
        return cancelPhrases.any { clean == it || clean.startsWith("$it ") }
    }

    fun assessRisk(actionType: ActionType, device: String): Pair<RiskLevel, String> {
        val isRemoteDevice = device.lowercase() != "this phone" && !device.lowercase().contains("phone")

        if (isRemoteDevice) {
            return Pair(
                RiskLevel.HIGH,
                "Cross-device dispatch requires mutual authentication and remote host confirmation."
            )
        }

        return when (actionType) {
            ActionType.GET_BATTERY_INFO,
            ActionType.GET_STORAGE_INFO,
            ActionType.GET_MEMORY_INFO,
            ActionType.GET_NETWORK_INFO -> Pair(
                RiskLevel.LOW,
                "Read-only system status query using standard Android APIs."
            )

            ActionType.OPEN_APP -> Pair(
                RiskLevel.LOW,
                "Launches installed application intent with explicit user consent."
            )

            ActionType.OPEN_URL -> Pair(
                RiskLevel.MEDIUM,
                "Navigates to external web URL in default secure browser."
            )

            ActionType.OPEN_SETTINGS -> Pair(
                RiskLevel.MEDIUM,
                "Opens Android system configuration pane."
            )

            ActionType.SET_VOLUME -> Pair(
                RiskLevel.MEDIUM,
                "Modifies device audio stream level."
            )

            ActionType.MEDIA_CONTROL -> Pair(
                RiskLevel.LOW,
                "Dispatches media playback transport key event."
            )

            ActionType.TAKE_SCREENSHOT -> Pair(
                RiskLevel.HIGH,
                "Accesses screen display surface via Assistive Service."
            )

            ActionType.COMMUNICATION -> Pair(
                RiskLevel.MEDIUM,
                "Opens telephony or messaging dialer for user review."
            )

            ActionType.CROSS_DEVICE_COMMAND -> Pair(
                RiskLevel.HIGH,
                "Executes authenticated remote command on linked computer."
            )

            ActionType.STOP_CANCEL -> Pair(
                RiskLevel.LOW,
                "Aborts pending action execution immediately."
            )

            ActionType.CONVERSATIONAL_RESPONSE,
            ActionType.UNSUPPORTED_ACTION -> Pair(
                RiskLevel.LOW,
                "Conversational response with zero device side-effects."
            )
        }
    }

    fun validatePlan(plan: AuraActionPlan): AuraActionPlan {
        val (risk, explanation) = assessRisk(plan.actionType, plan.device)
        return plan.copy(
            risk = risk,
            explanation = explanation
        )
    }
}
