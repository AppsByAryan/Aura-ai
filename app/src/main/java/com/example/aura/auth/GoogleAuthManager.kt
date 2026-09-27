package com.example.aura.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.aura.data.AuraPreferences
import com.example.aura.data.AuraUser
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAuthManager(
    private val context: Context,
    private val preferences: AuraPreferences
) {
    private val credentialManager = CredentialManager.create(context)

    // Google Cloud OAuth project: gen-lang-client-0868525895 (Project Number: 1020527569133)
    private val serverClientId = "1020527569133-android.apps.googleusercontent.com"

    suspend fun signIn(activityContext: Context): Result<AuraUser> = withContext(Dispatchers.IO) {
        try {
            // Clear any cached credentials so the system Google Account Chooser is shown
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {}

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            handleSignInResponse(response)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Sign in was cancelled by user."))
        } catch (e: Exception) {
            // Do not silently force hardcoded email - report real reason so user can pick/switch or enter account
            Result.failure(e)
        }
    }

    private fun handleSignInResponse(response: GetCredentialResponse): Result<AuraUser> {
        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val user = AuraUser(
                    isSignedIn = true,
                    id = googleIdTokenCredential.id,
                    email = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.givenName ?: googleIdTokenCredential.id.substringBefore("@"),
                    givenName = googleIdTokenCredential.givenName ?: "",
                    familyName = googleIdTokenCredential.familyName ?: "",
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: "",
                    idToken = googleIdTokenCredential.idToken
                )
                preferences.saveUserProfile(user)
                return Result.success(user)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.failure(Exception("Unrecognized credential format."))
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {}
        preferences.clearUserProfile()
        Result.success(Unit)
    }

    fun getCurrentUser(): AuraUser {
        return preferences.getUserProfile()
    }

    fun switchOrSetAccount(name: String, email: String, photoUrl: String = ""): AuraUser {
        val cleanEmail = email.trim()
        val cleanName = if (name.isNotBlank()) name.trim() else cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val user = AuraUser(
            isSignedIn = cleanEmail.isNotBlank(),
            id = cleanEmail,
            email = cleanEmail,
            displayName = cleanName,
            photoUrl = photoUrl
        )
        preferences.saveUserProfile(user)
        return user
    }
}
