package io.pcast.error

import io.ktor.http.HttpStatusCode

sealed class HttpError(
    val statusCode: HttpStatusCode,
) {
    data object BadRequest : HttpError(statusCode = HttpStatusCode.BadRequest)

    data object NotFound : HttpError(statusCode = HttpStatusCode.NotFound)

    data object NoContent : HttpError(statusCode = HttpStatusCode.NoContent)

    data object InternalError : HttpError(statusCode = HttpStatusCode.InternalServerError)

    data object Unauthorized : HttpError(statusCode = HttpStatusCode.Unauthorized)

    data object Conflict : HttpError(statusCode = HttpStatusCode.Conflict)

    data object NotImplemented : HttpError(statusCode = HttpStatusCode.NotImplemented)
}

class AbortError(
    val code: HttpError,
    val details: String,
    cause: Throwable? = null,
) : Throwable(details, cause)
