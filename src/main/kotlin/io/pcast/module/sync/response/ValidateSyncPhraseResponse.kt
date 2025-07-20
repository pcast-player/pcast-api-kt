package io.pcast.module.sync.response

import kotlinx.serialization.Serializable

@Serializable
data class ValidateSyncPhraseResponse(
    val isValid: Boolean,
)
