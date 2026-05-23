package io.pcast.module.auth.passkey.request

import kotlinx.serialization.Serializable

@Serializable
data class PasskeyAuthenticationOptionsRequest(
    val email: String? = null,
)
