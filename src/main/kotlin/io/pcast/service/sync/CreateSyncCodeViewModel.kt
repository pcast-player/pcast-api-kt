package io.pcast.service.sync

import kotlinx.serialization.Serializable

@Serializable
data class CreateSyncCodeViewModel(
    val syncPhrase: String,
)
