package golf.adventofcode.endpoints.web

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import kotlinx.html.*

object LeaderboardDayView {
    suspend fun getHtml(call: ApplicationCall) {
        val year = 2000 + (call.parameters["year"]?.toIntOrNull() ?: throw NotFoundException("Invalid year"))
        val day = call.parameters["day"]?.toIntOrNull() ?: throw NotFoundException("Invalid day")

        call.respondHtmlView("Advent of Code Golf Leaderboard $year/day/$day") {
            h1 { +"Advent of Code Golf Leaderboard $year/day/$day" }

            h2 { +"Part 1" }
            renderLeaderboardTable()

            h2 { +"Part 2" }
            renderLeaderboardTable()
        }
    }

    private fun HtmlBlockTag.renderLeaderboardTable() {
        table("leaderboard") {
            thead {
                tr {
                    th {}
                    th(classes = "left-align") { +"Username" }
                    th(classes = "left-align") { +"Language" }
                    th(classes = "right-align") { +"Tokens" }
                    th(classes = "right-align") { +"Date" }
                }
            }
            tbody {
                (1..10).forEach { rank ->
                    tr {
                        td("rank") { +"$rank" }
                        td { +"My_username" }
                        td { +"Python" }
                        td("right-align") { +"1234" }
                        td("right-align") { +"Two days ago" }
                    }
                }
            }
        }
    }
}
