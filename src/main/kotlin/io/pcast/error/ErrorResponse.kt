package io.pcast.error

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val message: String,
)
