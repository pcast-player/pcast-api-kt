package io.pcast.module.sync.api

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.extensions.userId
import io.pcast.module.sync.SyncService
import io.pcast.module.sync.request.ValidateSyncPhraseRequest
import io.pcast.module.sync.response.CreateSyncCodeResponse
import io.pcast.module.sync.response.FriendlyIdResponse
import io.pcast.module.sync.response.ValidateSyncPhraseResponse
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("SyncRouter")

/**
 * TODO: /sync/phrase endpoints are temporary test scaffolding and will be removed.
 *       Key generation must move to the client. The server role is only to
 *       validate a client-supplied phrase and derive (then discard) its seed.
 */
fun Route.registerSyncRoutes() {
    val service by inject<SyncService>()

    get("/sync/phrase") {
        val userId = call.userId()
        // Do not log the phrase; record only the user performing the action.
        log.info("sync phrase requested by user={}", userId)

        val syncPhrase = service.createSyncPhrase()

        call.response.header(HttpHeaders.CacheControl, "no-store")
        call.response.header("Pragma", "no-cache")
        call.respond(HttpStatusCode.Created, CreateSyncCodeResponse(syncPhrase))
    }

    post("/sync/phrase") {
        val userId = call.userId()
        val request = call.receive<ValidateSyncPhraseRequest>()
        val phrase = request.syncPhrase.toCharArray()

        log.info("sync phrase validation requested by user={}", userId)

        val valid = service.validateSyncPhrase(phrase)
        phrase.fill('\u0000') // zero out the char array after use

        if (!valid) throw AbortError(HttpError.BadRequest, "Invalid sync phrase")

        call.response.header(HttpHeaders.CacheControl, "no-store")
        call.response.header("Pragma", "no-cache")
        call.respond(ValidateSyncPhraseResponse(true))
    }

    get("/sync/friendly-id") {
        val friendlyId = service.createFriendlyId()

        call.respond(HttpStatusCode.Created, FriendlyIdResponse(friendlyId))
    }
}
