package com.example.aura.data

data class AuraUser(
    val isSignedIn: Boolean = false,
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val givenName: String = "",
    val familyName: String = "",
    val photoUrl: String = "",
    val idToken: String = ""
)
