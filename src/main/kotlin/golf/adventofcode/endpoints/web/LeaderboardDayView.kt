package golf.adventofcode.endpoints.web

import golf.adventofcode.database.Solution
import golf.adventofcode.endpoints.api.UploadSolutionApi
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import kotlinx.html.*

object LeaderboardDayView {
    suspend fun getHtml(call: ApplicationCall) {
        val year = 2000 + (call.parameters["year"]?.toIntOrNull() ?: throw NotFoundException("Invalid year"))
        val day = call.parameters["day"]?.toIntOrNull() ?: throw NotFoundException("Invalid day")

        fun HtmlBlockTag.renderUpload() {
            h2 { +"Submit solution" }
            // TODO: Fix design. Maybe use dialog?
            form(action = "/api/solution/upload") {
                input(type = InputType.hidden) {
                    name = "year"
                    value = year.toString()
                }
                input(type = InputType.hidden) {
                    name = "day"
                    value = day.toString()
                }
                select {
                    name = "part"
                    option {
                        value = "1"
                        +"Part 1"
                    }
                    option {
                        value = "2"
                        +"Part 2"
                    }
                }
                br()
                select { // TODO design: either prettify select, or use radio (similar style as checkbox)
                    name = "language"
                    Solution.Language.entries.forEach {
                        option {
                            value = it.name
                            +it.name
                        }
                    }
                }
                label("checkbox-container") {
                    +"Code publicly visible"
                    input(type = InputType.checkBox) {
                        name = "codeIsPublic"
                        checked = true
                        span("checkbox")
                    }
                }
                textArea {
                    name = "code"
                    rows = "10"
                    cols = "80"
                    placeholder = "Paste your code here"
                    maxLength = UploadSolutionApi.MAX_CODE_LENGTH.toString()
                }
                br()
                input(type = InputType.submit) {
                    onClick = "submitForm(event)"
                    value = "Submit"
                }
            }
        }

        call.respondHtmlView("Advent of Code Golf Leaderboard $year/day/$day") {
            h1 { +"Advent of Code Golf Leaderboard $year/day/$day" }

            renderUpload()

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
