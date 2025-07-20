package io.pcast.module.sync.request

import kotlinx.serialization.Serializable

@Serializable
data class ValidateSyncPhraseRequest(
    val syncPhrase: String,
)
