package io.pcast.module.auth.passkey.request

import kotlinx.serialization.Serializable

@Serializable
data class PasskeySignupOptionsRequest(
    val email: String,
) {
    fun normalizedEmail(): String = email.trim().lowercase()
}
