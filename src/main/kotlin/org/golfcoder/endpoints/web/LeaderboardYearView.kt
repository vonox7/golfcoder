package org.golfcoder.endpoints.web

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.p

object LeaderboardYearView {
    suspend fun getHtml(call: ApplicationCall) {
        val year = 2000 + (call.parameters["year"]?.toIntOrNull() ?: throw NotFoundException("Invalid year"))

        call.respondHtmlView("Advent of Code Leaderboard $year") {
            h1 { +"Advent of Code Leaderboard $year" }

            p {
                +"A code golf community leaderboard for "
                a("https://adventofcode.com", "_blank") { +"adventofcode.com" }
                +", with a focus on code size. "
                +"Every name, including variables and functions, is considered as a single token, irrespective of its length. "
                a("/about") { +"More about Golfcoder" }
                +"."
            }

            (1..24).forEach { day ->
                p {
                    a("/$year/day/$day") { +"Day $day" }
                    // TODO: Show best solutions for each day next to day link
                }
            }
        }
    }
}
