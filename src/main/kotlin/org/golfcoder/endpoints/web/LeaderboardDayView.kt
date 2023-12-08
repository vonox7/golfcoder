package org.golfcoder.endpoints.web

import com.moshbit.katerbase.equal
import com.moshbit.katerbase.inArray
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.flow.toList
import kotlinx.html.*
import org.golfcoder.database.Solution
import org.golfcoder.database.User
import org.golfcoder.endpoints.api.UploadSolutionApi
import org.golfcoder.endpoints.web.TemplateView.template
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.golfcoder.tokenizer.NotYetAvailableTokenizer
import org.golfcoder.utils.relativeToNow

object LeaderboardDayView {
    suspend fun getHtml(call: ApplicationCall) {
        val year = 2000 + (call.parameters["year"]?.toIntOrNull() ?: throw NotFoundException("Invalid year"))
        val day = call.parameters["day"]?.toIntOrNull() ?: throw NotFoundException("Invalid day")
        val parts = 2
        val solutions = (1..parts).map { part ->
            @Suppress("DEPRECATION") // use excludeFields - only `code` is excluded since it is too big
            mainDatabase.getSuspendingCollection<Solution>()
                .find(Solution::year equal year, Solution::day equal day, Solution::part equal part)
                .sortBy(Solution::tokenCount)
                .excludeFields(Solution::code)
                // Load a little bit more than we display, so we show only 1 score per user.
                // This could be done also with an aggregate query?
                // But then we could also combine the 2 queries (1 per part) into 1 query?
                .limit(200)
                .toList()
        }
        val userIds = solutions.flatten().map { it.userId }.distinct()
        val userIdsToUsers = mainDatabase.getSuspendingCollection<User>()
            .find(User::_id inArray userIds)
            .selectedFields(
                User::_id,
                User::name,
                User::nameIsPublic,
                User::profilePictureIsPublic,
                User::publicProfilePictureUrl,
            )
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
                br()
                textArea {
                    name = "code"
                    rows = "10"
                    cols = "80"
                    placeholder = "Paste your code here"
                    maxLength = UploadSolutionApi.MAX_CODE_LENGTH.toString()
                }
                br()
                b { +"Rules" }
                ul {
                    li {
                        +"You're welcome to participate alone or in a team."
                    }
                    li {
                        +"You may submit multiple solutions and explore different programming languages."
                    }
                    li {
                        +"Stick to the standard library of your language, no further dependencies/libraries, except the ones which "
                        a(href = "https://onecompiler.com/", target = "_blank") { +"OneCompiler" }
                        +" provides (e.g. NumPy for Python)."
                    }
                    li {
                        +"Ensure your code aligns to the template ("
                        Solution.Language.entries
                            .filter { it.template != null }
                            .mapIndexed { index, it ->
                                if (index != 0) {
                                    +", "
                                }
                                a(href = "/template/golfcoder-${it.name.lowercase()}-template.${it.fileEnding}") {
                                    +it.displayName
                                }
                            }
                        +"), reading the puzzle input from stdin (terminated with end-of-file), and printing the solution to stdout."
                    }
                    li {
                        +"Please refrain from making network requests, reading data from files, or storing data in variable/function/class names for reflection."
                    }
                    li {
                        +"Note that OneCompiler currently supports only Kotlin 1.3, so newer functions like sumOf are not yet supported. I've already contacted OneCompiler to update to a newer Kotlin version."
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
                input(type = InputType.hidden) {
                    name = "onlyTokenize"
                    value = "on"
                }
                input(type = InputType.submit) {
                    onClick = "submitForm(event)"
                    if (call.sessions.get<UserSession>() == null) {
                        disabled = true
                        value = "Please login to submit a solution"
                    } else {
                        value = "Calculate tokens"
                    }
                }
            }
        }

        call.respondHtmlView("Advent of Code Leaderboard $year day $day") {
            h1 { +"Advent of Code Leaderboard $year day $day" }

            renderUpload()

            h2 { +"Leaderboard" }
            renderLeaderboardTable(solutions, userIdsToUsers)
        }
    }

    private fun HtmlBlockTag.renderLeaderboardTable(
        solutions: List<List<Solution>>,
        userIdsToUsers: Map<String, User>,
    ) {
        // We need to do some transformations here, so we get per user the best solution per part (also per language).
        // And to calculate the total score (for all parts) (per user+language).
        val parts = solutions.count()
        val scoresByUserId = mutableMapOf<String, List<Solution>>()
        solutions.forEach { solutionsPerPart ->
            solutionsPerPart
                .groupBy { it.userId + it.language }
                .forEach { (userId, solutions) ->
                    scoresByUserId[userId] =
                        (scoresByUserId[userId] ?: emptyList()) + solutions.minBy { it.tokenCount }
                }
        }

        val sortedScores: List<Pair<List<Solution>, Int>> = scoresByUserId.values.associateWith { userSolutions ->
            (1..parts).sumOf { part ->
                userSolutions.find { it.part == part }?.tokenCount
                    ?: 10_000 // No solution for part yet submitted
            }
        }.toList().sortedBy { it.second }

        if (sortedScores.isEmpty()) {
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
                    th(classes = "right-align") { +"Last change" }
                }
            }
            tbody {
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
                                    if (solution.tokenizerVersion < solution.language.tokenizerVersion) {
                                        +"Recalculating..."
                                    } else {
                                        if (solution.codePubliclyVisible) {
                                            a(href = "/solution/${solution._id}.${solution.language.fileEnding}") {
                                                +"${solution.tokenCount}"
                                            }
                                        } else {
                                            +"${solution.tokenCount}"
                                        }
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
