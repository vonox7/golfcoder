package golf.adventofcode.endpoints.web

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import kotlinx.html.h1
import kotlinx.html.p

object LeaderboardYearView {
    suspend fun getHtml(call: ApplicationCall) {
        val year = 2000 + (call.parameters["year"]?.toIntOrNull() ?: throw NotFoundException("Invalid year"))

        call.respondHtmlView("Advent of Code $year") {
            h1 { +"Advent of Code $year" }
            p { +"Hello, world!" }
        }
    }
}
