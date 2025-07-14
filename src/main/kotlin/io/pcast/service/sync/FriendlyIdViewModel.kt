package io.pcast.service.sync

import kotlinx.serialization.Serializable

@Serializable
data class FriendlyIdViewModel(
    val friendlyId: String,
)
