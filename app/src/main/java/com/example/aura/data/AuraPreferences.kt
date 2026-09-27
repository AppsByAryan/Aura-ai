package com.example.aura.data

import android.content.Context
import android.content.SharedPreferences

class AuraPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("aura_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WAKE_WORD_ENABLED = "key_wake_word_enabled"
        private const val KEY_WAKE_WORD_PHRASE = "key_wake_word_phrase"
        private const val KEY_WAKE_WORD_SENSITIVITY = "key_wake_word_sensitivity"
        private const val KEY_DIRECT_EXECUTION = "key_direct_execution"
        private const val KEY_GOOGLE_SIGNED_IN = "key_google_signed_in"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_PHOTO_URL = "key_user_photo_url"
        private const val KEY_USER_GOOGLE_ID = "key_user_google_id"
        const val DEFAULT_WAKE_WORD = "Hey AURA"
        const val DEFAULT_SENSITIVITY = 0.7f
    }

    var isGoogleSignedIn: Boolean
        get() = prefs.getBoolean(KEY_GOOGLE_SIGNED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_GOOGLE_SIGNED_IN, value).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userPhotoUrl: String
        get() = prefs.getString(KEY_USER_PHOTO_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_PHOTO_URL, value).apply()

    var userGoogleId: String
        get() = prefs.getString(KEY_USER_GOOGLE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_GOOGLE_ID, value).apply()

    fun getUserProfile(): AuraUser {
        val signedIn = isGoogleSignedIn && userEmail.isNotBlank()
        return AuraUser(
            isSignedIn = signedIn,
            id = userGoogleId,
            email = userEmail,
            displayName = userName,
            photoUrl = userPhotoUrl
        )
    }

    fun saveUserProfile(user: AuraUser) {
        prefs.edit()
            .putBoolean(KEY_GOOGLE_SIGNED_IN, user.isSignedIn)
            .putString(KEY_USER_GOOGLE_ID, user.id)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.displayName)
            .putString(KEY_USER_PHOTO_URL, user.photoUrl)
            .apply()
    }

    fun clearUserProfile() {
        prefs.edit()
            .putBoolean(KEY_GOOGLE_SIGNED_IN, false)
            .remove(KEY_USER_GOOGLE_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_PHOTO_URL)
            .apply()
    }

    var isDirectExecution: Boolean
        get() = prefs.getBoolean(KEY_DIRECT_EXECUTION, true)
        set(value) = prefs.edit().putBoolean(KEY_DIRECT_EXECUTION, value).apply()

    var isWakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()

    var wakeWordPhrase: String
        get() = prefs.getString(KEY_WAKE_WORD_PHRASE, DEFAULT_WAKE_WORD) ?: DEFAULT_WAKE_WORD
        set(value) = prefs.edit().putString(KEY_WAKE_WORD_PHRASE, value.trim()).apply()

    var wakeWordSensitivity: Float
        get() = prefs.getFloat(KEY_WAKE_WORD_SENSITIVITY, DEFAULT_SENSITIVITY)
        set(value) = prefs.edit().putFloat(KEY_WAKE_WORD_SENSITIVITY, value).apply()
}
