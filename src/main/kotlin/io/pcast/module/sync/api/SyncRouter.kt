package io.pcast.module.sync.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.pcast.module.sync.SyncService
import io.pcast.module.sync.request.ValidateSyncPhraseRequest
import io.pcast.module.sync.response.CreateSyncCodeResponse
import io.pcast.module.sync.response.FriendlyIdResponse
import io.pcast.module.sync.response.ValidateSyncPhraseResponse
import org.koin.ktor.ext.inject

fun Route.registerSyncRoutes() {
    val service by inject<SyncService>()

    get("/sync/phrase") {
        try {
            val syncCode = service.createSyncPhrase()

            call.respond(HttpStatusCode.Created, CreateSyncCodeResponse(syncCode))
        } catch (_: Throwable) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/sync/phrase") {
        try {
            val request = call.receive<ValidateSyncPhraseRequest>()
            val phrase = request.syncPhrase.toCharArray()

            service.getSeedFromSyncPhrase(phrase)

            call.respond(ValidateSyncPhraseResponse(true))
        } catch (_: Throwable) {
            call.respond(HttpStatusCode.InternalServerError, ValidateSyncPhraseResponse(false))
        }
    }

    get("/sync/friendly-id") {
        try {
            val friendlyId = service.createFriendlyId()

            call.respond(HttpStatusCode.Created, FriendlyIdResponse(friendlyId))
        } catch (_: Throwable) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
