package golf.adventofcode

import golf.adventofcode.endpoints.web.AboutView
import golf.adventofcode.endpoints.web.LeaderboardDayView
import golf.adventofcode.endpoints.web.LeaderboardYearView
import golf.adventofcode.plugins.configureHTTP
import golf.adventofcode.plugins.configureSecurity
import golf.adventofcode.plugins.configureSerialization
import golf.adventofcode.plugins.configureTemplating
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val container = System.getenv("CONTAINER") ?: "local"

fun main() {
    println("Starting ktor...")
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 8030,
        host = System.getenv("HOST") ?: "0.0.0.0",
        module = Application::ktorServerModule
    ).start(wait = true)
}

private fun Application.ktorServerModule() {
    configureSecurity()
    configureHTTP()
    configureSerialization()
    configureTemplating()

    routing {
        get("/") { call.respondRedirect("/2023") }
        get("/favicon.ico") { call.respondRedirect("/static/images/favicon.ico", permanent = true) }

        get(Regex("about")) { AboutView.getHtml(call) }
        get(Regex("20(?<year>[0-9]{2})")) { LeaderboardYearView.getHtml(call) }
        get(Regex("20(?<year>[0-9]{2})/day/(?<day>[0-9]{1,2})")) { LeaderboardDayView.getHtml(call) }

        // TODO robots.txt, etc.

        staticResources("/static/css-${Sysinfo.release}", "static/css")
        staticResources("/static/js-${Sysinfo.release}", "static/js")
        staticResources("/static/images", "static/images")
    }
}
