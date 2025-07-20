package io.pcast.service.sync

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.registerSyncRoutes() {
    val service by inject<SyncService>()

    get("/sync/phrase") {
        try {
            val syncCode = service.createSyncPhrase()

            call.respond(HttpStatusCode.Created, CreateSyncCodeViewModel(syncCode))
        } catch (_: Throwable) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/sync/phrase") {
        try {
            val request = call.receive<ValidateSyncPhraseRequest>()
            val phrase = request.syncPhrase.toCharArray()

            service.getSeedFromSyncPhrase(phrase)

            call.respond(ValidateSyncPhraseViewModel(true))
        } catch (_: Throwable) {
            call.respond(HttpStatusCode.InternalServerError, ValidateSyncPhraseViewModel(false))
        }
    }

    get("/sync/friendly-id") {
        try {
            val friendlyId = service.createFriendlyId()

            call.respond(HttpStatusCode.Created, FriendlyIdViewModel(friendlyId))
        } catch (_: Throwable) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
