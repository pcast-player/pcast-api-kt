package io.pcast.module.sync.response

import kotlinx.serialization.Serializable

@Serializable
data class FriendlyIdResponse(
    val friendlyId: String,
)
