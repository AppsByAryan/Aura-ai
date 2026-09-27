package com.example.aura.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.voice.VoiceInteractionService

class AuraVoiceInteractionService : VoiceInteractionService() {

    companion object {
        fun isDefaultAssistant(context: Context): Boolean {
            return try {
                val assistantSetting = Settings.Secure.getString(
                    context.contentResolver,
                    "assistant"
                )
                if (assistantSetting != null) {
                    val component = ComponentName.unflattenFromString(assistantSetting)
                    component?.packageName == context.packageName
                } else {
                    false
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}
