package io.pcast.controller.sync

import kotlinx.serialization.Serializable

@Serializable
data class ValidateSyncPhraseResponse(
    val isValid: Boolean
)
