package golf.adventofcode.endpoints.web

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import kotlinx.html.h1
import kotlinx.html.p

object LeaderboardDayView {
    suspend fun getHtml(call: ApplicationCall) {
        val year = 2000 + (call.parameters["year"]?.toIntOrNull() ?: throw NotFoundException("Invalid year"))
        val day = call.parameters["day"]?.toIntOrNull() ?: throw NotFoundException("Invalid day")

        call.respondHtmlView("Advent of Code $year/day/$day") {
            h1 { +"Advent of Code $year/day/$day" }
            p { +"Hello, world!" }
        }
    }
}
