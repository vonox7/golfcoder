package golf.adventofcode.endpoints.web

import golf.adventofcode.plugins.UserSession
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.html.h1

object EditUserView {
    suspend fun getHtml(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()!!
        call.respondHtmlView("Advent of Code ${session.displayName}") {
            h1 { +session.displayName }
            +"TODO: User management"
        }
    }
}