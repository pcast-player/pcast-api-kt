package io.pcast.service.sync

import kotlinx.serialization.Serializable

@Serializable
data class ValidateSyncPhraseRequest(
    val syncPhrase: String,
)
