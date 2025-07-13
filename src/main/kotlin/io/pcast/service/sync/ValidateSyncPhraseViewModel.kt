package io.pcast.service.sync

import kotlinx.serialization.Serializable

@Serializable
data class ValidateSyncPhraseViewModel(
    val isValid: Boolean,
)
