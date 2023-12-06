package golf.adventofcode.endpoints.web

import com.moshbit.katerbase.equal
import com.moshbit.katerbase.inArray
import golf.adventofcode.database.Solution
import golf.adventofcode.database.User
import golf.adventofcode.endpoints.api.UploadSolutionApi
import golf.adventofcode.endpoints.web.TemplateView.template
import golf.adventofcode.mainDatabase
import golf.adventofcode.tokenizer.NotYetAvailableTokenizer
import golf.adventofcode.utils.relativeToNow
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import kotlinx.coroutines.flow.toList
import kotlinx.html.*

object LeaderboardDayView {
    suspend fun getHtml(call: ApplicationCall) {
        val year = 2000 + (call.parameters["year"]?.toIntOrNull() ?: throw NotFoundException("Invalid year"))
        val day = call.parameters["day"]?.toIntOrNull() ?: throw NotFoundException("Invalid day")
        val parts = 2
        val solutions = (1..parts).map { part ->
            mainDatabase.getSuspendingCollection<Solution>()
                .find(Solution::year equal year, Solution::day equal day, Solution::part equal part)
                .sortBy(Solution::tokenCount)
                // Load a little bit more than we display, so we show only 1 score per user.
                // This could be done also with an aggregate query?
                // But then we could also combine the 2 queries (1 per part) into 1 query?
                .limit(200)
                .toList()
        }
        val userIds = solutions.flatten().map { it.userId }.distinct()
        val userIdsToUsers = mainDatabase.getSuspendingCollection<User>()
            .find(User::_id inArray userIds)
            .selectedFields(User::_id, User::name, User::nameIsPublic, User::publicProfilePictureUrl)
            .toList()
            .associateBy { it._id }

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
                    (1..parts).forEach { part ->
                        option {
                            value = part.toString()
                            +"Part $part"
                        }
                    }
                }
                br()
                select { // TODO design: either prettify select, or use radio (similar style as checkbox)
                    name = "language"
                    Solution.Language.entries.forEach { language ->
                        option {
                            value = language.name
                            if (language.tokenizerClass == NotYetAvailableTokenizer::class) {
                                disabled = true
                                +"${language.displayName} (not yet available) "
                            } else {
                                +language.displayName
                            }
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
                p {
                    b { +"Rules" }
                    ul {
                        li {
                            +"You're welcome to participate alone or in a team."
                        }
                        li {
                            +"You may submit multiple solutions and explore different programming languages.."
                        }
                        li {
                            +"Stick to the standard library of your language, no further dependencies/libraries."
                        }
                        li {
                            +"Ensure your code aligns to the template ("
                            Solution.Language.entries
                                .filter { it.template != null }
                                .mapIndexed { index, it ->
                                    if (index != 0) {
                                        +", "
                                    }
                                    a(href = "/template/advent-of-code-golf-${it.name.lowercase()}-template.${it.fileEnding}") {
                                        +it.displayName
                                    }
                                }
                            +"), reading the puzzle input from \"input.txt\", and printing the solution to stdout."
                        }
                        li {
                            +"Please refrain from making network requests, reading data from files other than \"input.txt\", or storing data in variable/function/class names for reflection."
                        }
                    }
                }
                input(type = InputType.submit) {
                    onClick = "submitForm(event)"
                    value = "Submit"
                }
            }
        }

        call.respondHtmlView("Advent of Code Golf Leaderboard $year/day/$day") {
            h1 { +"Advent of Code Golf Leaderboard $year/day/$day" }

            renderUpload()

            h2 { +"Leaderboard" }
            renderLeaderboardTable(solutions, userIdsToUsers)
        }
    }

    private fun HtmlBlockTag.renderLeaderboardTable(
        solutions: List<List<Solution>>,
        userIdsToUsers: Map<String, User>,
    ) {
        val parts = solutions.count()

        if (solutions.isEmpty()) {
            p { +"No solutions submitted yet. Submit now your own solution." }
            return
        }

        table("leaderboard") {
            thead {
                tr {
                    th {}
                    th(classes = "left-align") { +"Name" }
                    th(classes = "left-align") { +"Language" }
                    th(classes = "right-align") { +"Tokens Sum" }
                    (1..parts).forEach { part ->
                        th(classes = "right-align") { +"Tokens Part $part" }
                    }
                    // TODO add download link to source (if public), and add external links.
                    th(classes = "right-align") { +"Last change" }
                }
            }
            tbody {
                // We need to do some transformations here, so we get per user the best solution per part (also per language).
                // And to calculate the total score (for all parts) (per user+language).

                val scoresByUserId = mutableMapOf<String, List<Solution>>()
                solutions.forEach { solutionsPerPart ->
                    solutionsPerPart
                        .groupBy { it.userId + it.language }
                        .forEach { (userId, solutions) ->
                            scoresByUserId[userId] =
                                (scoresByUserId[userId] ?: emptyList()) + solutions.minBy { it.tokenCount ?: 0 }
                        }
                }

                val sortedScores: List<Pair<List<Solution>, Int>> = scoresByUserId.values.associateWith { solutions ->
                    (1..parts).sumOf { part ->
                        val solution = solutions.find { it.part == part }
                        when {
                            solution == null -> 10_000// No solution for part yet submitted
                            solution.tokenCount == null -> 0 // Score not calculated yet: show "Calculating..." on top for a short time
                            else -> solution.tokenCount!!
                        }
                    }
                }.toList().sortedBy { it.second }

                // Render leaderboard
                sortedScores.forEachIndexed { index, (solutions, totalScore) ->
                    tr {
                        td("rank") { +"${index + 1}" }
                        td {
                            // All solutions in sortedScores have per entry the same user
                            val user = userIdsToUsers[solutions.first().userId]
                            renderUserProfileImage(user, big = false)
                            +(user?.name?.takeIf { user.nameIsPublic } ?: "anonymous")
                        }
                        // All solutions in sortedScores have per entry the same language
                        td { +solutions.first().language.displayName }

                        td("right-align") {
                            +"$totalScore"
                        }

                        (1..parts).forEach { part ->
                            val solution = solutions.find { it.part == part }
                            td("right-align") {
                                if (solution == null) {
                                    +"-"
                                } else {
                                    if (solution.tokenCount == null) {
                                        a(href = "") { // Just reload site, calculation should have finished by then
                                            +"Calculating..."
                                        }
                                    } else {
                                        +"${solution.tokenCount}"
                                    }
                                }
                            }
                        }
                        td("right-align") { +solutions.maxOf { it.uploadDate }.relativeToNow }
                    }
                }
            }
        }
    }
}
