package io.pcast.service.sync

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.pcast.result.attempt
import io.pcast.result.or
import io.pcast.result.unwrap
import org.koin.ktor.ext.inject

fun Route.registerSyncRoutes() {
    val service by inject<SyncService>()

    get("/sync/phrase") {
        attempt {
            val syncCode = service.createSyncPhrase().unwrap()
            val response = CreateSyncCodeViewModel(syncCode)

            call.respond(HttpStatusCode.Created, response)
        } or {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/sync/phrase") {
        attempt {
            val request = call.receive<ValidateSyncPhraseRequest>()
            val phrase = request.syncPhrase.toCharArray()

            service.getSeedFromSyncPhrase(phrase).unwrap()

            call.respond(ValidateSyncPhraseViewModel(true))
        } or {
            call.respond(HttpStatusCode.InternalServerError, ValidateSyncPhraseViewModel(false))
        }
    }
}
