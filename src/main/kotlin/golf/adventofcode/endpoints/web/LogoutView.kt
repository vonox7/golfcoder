package golf.adventofcode.endpoints.web

import golf.adventofcode.plugins.UserSession
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*

object LogoutView {
    suspend fun doLogout(call: ApplicationCall) {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/")
    }
}