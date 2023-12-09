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
import org.golfcoder.tokenizer.Tokenizer
import org.golfcoder.utils.relativeToNow
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

object LeaderboardDayView {
    suspend fun getHtml(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()
        val user = session?.let {
            mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal session.userId)
        }
        val year = 2000 + (call.parameters["year"]?.toIntOrNull() ?: throw NotFoundException("Invalid year"))
        val day = call.parameters["day"]?.toIntOrNull() ?: throw NotFoundException("Invalid day")
        val parts = 2
        val highlightedSolution: Solution? = call.parameters["solution"]?.let { solutionId ->
            mainDatabase.getSuspendingCollection<Solution>().findOne(Solution::_id equal solutionId)
                ?.takeIf { it.codePubliclyVisible || it.userId == user?._id }
                ?: throw NotFoundException("Invalid solution parameter")
        }
        val highlightedSolutionUser: User? = highlightedSolution?.let {
            mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal it.userId)!!
        }
        val highlightedSolutionTokenized: List<Tokenizer.Token>? = highlightedSolution?.let { solution ->
            solution.language.tokenizer.tokenize(solution.code)
        }

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
                    val defaultLanguage = user?.defaultLanguage ?: Solution.Language.PYTHON
                    Solution.Language.entries.forEach { language ->
                        option {
                            value = language.name
                            selected = language == defaultLanguage
                            if (language.tokenizer is NotYetAvailableTokenizer) {
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
                    name = "submitButton"
                    onClick = "submitForm(event)"
                    value = "Calculate tokens"
                }
            }
        }

        call.respondHtmlView("Advent of Code Leaderboard $year day $day") {
            h1 { +"Advent of Code Leaderboard $year day $day" }

            div("left-right-layout") {
                div("left") {
                    renderUpload()

                    h2 { +"Leaderboard" }
                    renderLeaderboardTable(solutions, userIdsToUsers, user)
                }

                div("right") {
                    if (highlightedSolution != null) {
                        h2 {
                            +"${highlightedSolution.tokenCount} tokens in ${highlightedSolution.language.displayName} "
                            +"for part ${highlightedSolution.part} by "
                            when {
                                highlightedSolutionUser!!._id == user?._id -> +"you"
                                highlightedSolutionUser.nameIsPublic -> +highlightedSolutionUser.name
                                else -> +"anonymous"
                            }
                        }
                        renderSolution(highlightedSolution, highlightedSolutionTokenized!!)
                    }
                }
            }
        }
    }

    private fun HtmlBlockTag.renderLeaderboardTable(
        solutions: List<List<Solution>>,
        userIdsToUsers: Map<String, User>,
        user: User?,
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
                                } else if (solution.codePubliclyVisible) {
                                    a(href = "?solution=${solution._id}") {
                                        +"${solution.tokenCount}"
                                    }
                                } else if (solution.userId == user?._id) {
                                    a(href = "?solution=${solution._id}") {
                                        +"${solution.tokenCount} (only accessible by you)"
                                    }
                                } else {
                                    +"${solution.tokenCount}"
                                }
                            }
                        }
                        td("right-align") { +solutions.maxOf { it.uploadDate }.relativeToNow }
                    }
                }
            }
        }
    }

    private fun HtmlBlockTag.renderSolution(solution: Solution, tokens: List<Tokenizer.Token>) {
        p {
            a(href = "/solution/${solution._id}.${solution.language.fileEnding}") {
                +"Download solution"
            }
        }
        var lastLineNumber = 1
        // TODO also do whitespacees from solution.code
        div("solution") {
            tokens.forEach { token ->
                // This is not a perfect whitespace layout, but good enough for now
                (lastLineNumber..<token.sourcePosition.start.lineNumber).forEach {
                    br()

                    // Add indent
                    if (token.type != Tokenizer.Token.Type.WHITESPACE && token.sourcePosition.start.columnNumber > 1) {
                        code("token-whitespace") { +" ".repeat(token.sourcePosition.start.columnNumber) }
                    }
                }
                lastLineNumber = token.sourcePosition.start.lineNumber

                when (token.type) {
                    Tokenizer.Token.Type.CODE_TOKEN -> code { +token.source }
                    Tokenizer.Token.Type.STRING -> {
                        token.source.forEach { char ->
                            code("token-string") {
                                +char.toString()
                            }
                        }
                    }

                    Tokenizer.Token.Type.WHITESPACE -> code("token-whitespace") { +token.source }
                    Tokenizer.Token.Type.COMMENT -> code("token-comment") { +token.source }
                }
            }
        }
    }
}
