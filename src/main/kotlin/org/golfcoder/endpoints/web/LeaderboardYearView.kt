package org.golfcoder.endpoints.web

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.p

object LeaderboardYearView {
    suspend fun getHtml(call: ApplicationCall) {
        val year = 2000 + (call.parameters["year"]?.toIntOrNull() ?: throw NotFoundException("Invalid year"))

        call.respondHtmlView("Golfcoder Advent of Code Leaderboard $year") {
            h1 { +"Golfcoder Advent of Code Leaderboard $year" }
            (1..24).forEach { day ->
                p {
                    a("/$year/day/$day") { +"Day $day" }
                }
            }
        }
    }
}
