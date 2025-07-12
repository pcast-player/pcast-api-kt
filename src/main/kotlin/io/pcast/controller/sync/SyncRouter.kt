package io.pcast.controller.sync

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
    val handler by inject<SyncHandler>()

    get("/sync/phrase") {
        attempt {
            val syncCode = handler.createSyncPhrase().unwrap()
            val response = CreateSyncCodeResponse(syncCode)

            call.respond(HttpStatusCode.Created, response)
        } or {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/sync/phrase") {
        attempt {
            val request = call.receive<ValidateSyncPhraseRequest>()
            val phrase = request.syncPhrase.toCharArray()

            handler.getSeedFromSyncPhrase(phrase).unwrap()

            call.respond(ValidateSyncPhraseResponse(true))
        } or {
            call.respond(HttpStatusCode.InternalServerError, ValidateSyncPhraseResponse(false))
        }
    }
}
