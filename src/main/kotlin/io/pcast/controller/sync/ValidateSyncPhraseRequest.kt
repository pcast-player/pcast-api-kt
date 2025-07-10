package io.pcast.controller.sync

import kotlinx.serialization.Serializable

@Serializable
data class ValidateSyncPhraseRequest(
    val syncPhrase: String,
)
