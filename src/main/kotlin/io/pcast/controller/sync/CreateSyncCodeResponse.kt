package io.pcast.controller.sync

import kotlinx.serialization.Serializable

@Serializable
data class CreateSyncCodeResponse(
    val syncPhrase: String,
)
