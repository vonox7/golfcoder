package org.golfcoder.endpoints.web

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import org.golfcoder.plugins.UserSession

object LogoutView {
    suspend fun doLogout(call: ApplicationCall) {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/")
    }
}