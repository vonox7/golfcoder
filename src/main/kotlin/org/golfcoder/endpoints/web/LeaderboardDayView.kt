package org.golfcoder.endpoints.web

import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.flow.toList
import kotlinx.html.*
import org.golfcoder.database.LeaderboardPosition
import org.golfcoder.database.Solution
import org.golfcoder.database.User
import org.golfcoder.database.getUserProfiles
import org.golfcoder.endpoints.api.UploadSolutionApi
import org.golfcoder.endpoints.api.UploadSolutionApi.PART_RANGE
import org.golfcoder.endpoints.web.TemplateView.template
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.golfcoder.tokenizer.NotYetAvailableTokenizer
import org.golfcoder.tokenizer.Tokenizer
import org.golfcoder.utils.relativeToNow

object LeaderboardDayView {
    suspend fun getHtml(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()
        val currentUser = session?.let {
            mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal session.userId)
        }
        val year = 2000 + (call.parameters["year"]?.toIntOrNull() ?: throw NotFoundException("Invalid year"))
        val day = call.parameters["day"]?.toIntOrNull() ?: throw NotFoundException("Invalid day")
        val highlightedSolution: Solution? = call.parameters["solution"]?.let { solutionId ->
            mainDatabase.getSuspendingCollection<Solution>().findOne(Solution::_id equal solutionId)
                ?.takeIf { (it.codePubliclyVisible || currentUser?.admin == true) || it.userId == currentUser?._id }
                ?: throw NotFoundException("Invalid solution parameter")
        }

        val highlightedSolutionUser: User? = highlightedSolution?.let {
            // findOne query will return null when the user was deleted
            mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal it.userId)
        }
        val highlightedSolutionTokenized: List<Tokenizer.Token>? = highlightedSolution?.let { solution ->
            solution.language.tokenizer.tokenize(solution.code)
        }

        val leaderboardPositions = mainDatabase.getSuspendingCollection<LeaderboardPosition>()
            .find(LeaderboardPosition::year equal year, LeaderboardPosition::day equal day)
            .sortBy(LeaderboardPosition::tokenSum)
            .toList()

        val userIdsToUsers = getUserProfiles(leaderboardPositions.map { it.userId }.toSet())

        val defaultLanguage = currentUser?.defaultLanguage ?: Solution.Language.PYTHON

        fun HtmlBlockTag.renderUpload() {
            h2 { +"Submit solution" }
            form(action = "/api/solution/upload") {
                input(type = InputType.hidden) {
                    name = "year"
                    value = year.toString()
                }
                input(type = InputType.hidden) {
                    name = "day"
                    value = day.toString()
                }
                select("right-spacing") {
                    name = "part"
                    PART_RANGE.forEach { part ->
                        option {
                            value = part.toString()
                            +"Part $part"
                        }
                    }
                }
                select {
                    name = "language"
                    onChange = "fillTemplate(event); resetSubmitForm(event)"
                    Solution.Language.entries.forEach { language ->
                        option {
                            value = language.name
                            selected = language == defaultLanguage
                            attributes["data-template"] = language.template ?: ""
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
                    rows = "12"
                    cols = "90"
                    placeholder = "Paste your code here. Download a template for your language below."
                    onChange = "resetSubmitForm(event)"
                    onKeyDown = "resetSubmitForm(event)"
                    maxLength = UploadSolutionApi.MAX_CODE_LENGTH.toString()
                    spellCheck = false
                    +defaultLanguage.template.orEmpty()
                }
                br()
                label("checkbox-container") {
                    +"Code publicly visible"
                    input(type = InputType.checkBox) {
                        name = "codeIsPublic"
                        checked = true
                        span("checkbox")
                    }
                }
                input(type = InputType.hidden) {
                    name = "submitState"
                    value = UploadSolutionApi.SubmitState.ONLY_TOKENIZE.name
                }
                input(type = InputType.submit) {
                    name = "submitButton"
                    onClick = "submitForm(event)"
                    value = "Calculate tokens"
                }
            }
        }

        call.respondHtmlView("Advent of Code Leaderboard $year / Day $day", maxWidth = false) {
            h1 { +"Advent of Code Leaderboard $year / Day $day" }
            div("center subheader") {
                a(href = "https://adventofcode.com/$year/day/$day", target = "_blank") {
                    +"View puzzle on adventofcode.com"
                }
            }

            div("left-right-layout") {
                div("left") {
                    renderUpload()

                    h2 { +"Leaderboard" }
                    if (leaderboardPositions.isEmpty()) {
                        p { +"No solutions submitted yet. Submit now your own solution." }
                    } else {
                        renderLeaderboard(leaderboardPositions, userIdsToUsers, currentUser)
                    }

                    renderRules()
                }

                div("right") {
                    id = "solution" // innerHtml will be set by UploadSolutionApi when submitting code
                    if (highlightedSolution != null) {
                        h2 {
                            +"${highlightedSolution.tokenCount} tokens in ${highlightedSolution.language.displayName} "
                            +"for part ${highlightedSolution.part} by "
                            when {
                                highlightedSolutionUser != null && highlightedSolutionUser._id == currentUser?._id -> +"you"
                                highlightedSolutionUser?.nameIsPublic == true -> +highlightedSolutionUser.name
                                else -> +"anonymous"
                            }
                        }
                        p {
                            a(href = "/solution/${highlightedSolution._id}.${highlightedSolution.language.fileEnding}") {
                                +"Download solution"
                            }
                            if (currentUser?.admin == true) {
                                form(action = "/api/admin/markSolutionAsCheated") {
                                    input(type = InputType.hidden) {
                                        name = "solutionId"
                                        value = highlightedSolution._id
                                    }
                                    input(type = InputType.submit) {
                                        name = "submitButton"
                                        onClick = "submitForm(event)"
                                        value = "Admin: Mark solution as cheated"
                                    }
                                }
                            }
                        }
                        renderSolution(highlightedSolutionTokenized!!)
                    }
                }
            }
        }
    }

    fun HtmlBlockTag.renderLeaderboard(
        leaderboardPositions: List<LeaderboardPosition>,
        userIdsToUsers: Map<String, User>,
        currentUser: User?,
    ) {
        table("leaderboard") {
            thead {
                tr {
                    th {}
                    th(classes = "left-align") { +"Name" }
                    th(classes = "left-align") { +"Language" }
                    th(classes = "right-align") { +"Tokens Sum" }
                    PART_RANGE.forEach { part ->
                        th(classes = "right-align") { +"Tokens Part $part" }
                    }
                    th(classes = "right-align mobile-hidden") { +"Last change" }
                }
            }
            tbody {
                // Render leaderboard
                leaderboardPositions.forEach { leaderboardPosition ->
                    val year = leaderboardPosition.year
                    val day = leaderboardPosition.day
                    tr {
                        td("rank") { +"${leaderboardPosition.position}" }
                        td {
                            // All solutions in sortedScores have per entry the same user
                            val user = userIdsToUsers[leaderboardPosition.userId]
                            renderUserProfileImage(user, big = false)
                            val userName = (user?.name?.takeIf { user.nameIsPublic } ?: "anonymous")
                            val adventOfCodeRepositoryUrl =
                                user?.getAdventOfCodeRepositoryUrl(year)?.takeIf { user.nameIsPublic }
                            if (adventOfCodeRepositoryUrl == null) {
                                +userName
                            } else {
                                a(href = adventOfCodeRepositoryUrl, target = "_blank") { +userName }
                            }
                        }
                        // All solutions in sortedScores have per entry the same language
                        td { +leaderboardPosition.language.displayName }

                        td("right-align") {
                            +"${leaderboardPosition.tokenSum}"
                        }

                        PART_RANGE.forEach { part ->
                            val partInfo = leaderboardPosition.partInfos[part]
                            td("right-align") {
                                if (partInfo == null) {
                                    +"-"
                                } else if (partInfo.codePubliclyVisible) {
                                    a(href = "/$year/day/$day?solution=${partInfo.solutionId}#solution") {
                                        +"${partInfo.tokens}"
                                    }
                                } else if (leaderboardPosition.userId == currentUser?._id || currentUser?.admin == true) {
                                    a(href = "/$year/day/$day?solution=${partInfo.solutionId}#solution") {
                                        +"${partInfo.tokens}"
                                    }
                                    br()
                                    span("text-small") {
                                        +"only accessible by you"
                                    }
                                } else {
                                    +"${partInfo.tokens}"
                                }
                            }
                        }
                        td("right-align mobile-hidden") {
                            +leaderboardPosition.partInfos.values.maxOf { it.uploadDate }.relativeToNow()
                        }
                    }
                }
            }
        }
    }

    private fun HtmlBlockTag.renderRules() {
        br()
        h2 { +"Rules" }
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
                +"Your code must be able to process all valid Advent of Code inputs. Golfcoder might reevaluate correctness of your solution with different inputs after your submission."
            }
        }

    }

    fun HtmlBlockTag.renderSolution(tokens: List<Tokenizer.Token>) {
        val numberRegex = Regex("[0-9.]+[A-Za-z]?") // Matches e.g. 1.3 or 1.3f

        fun String.detectTokenType(): String = when {
            this.all { !it.isLetterOrDigit() } -> "token-symbol" // e.g. brackets, operators...
            this.matches(numberRegex) -> "token-number"
            else -> ""
        }

        var lastLineNumber = 1
        div("solution") {
            tokens.forEach { token ->
                // This is not a perfect whitespace layout, but good enough for now
                repeat((lastLineNumber..<token.sourcePosition.start.lineNumber).count()) {
                    br()

                    // Add indent
                    if (token.sourcePosition.start.columnNumber > 1) {
                        code("token-whitespace") { +" ".repeat(token.sourcePosition.start.columnNumber) }
                    }
                }
                lastLineNumber = token.sourcePosition.start.lineNumber

                when (token.type) {
                    Tokenizer.Token.Type.CODE_TOKEN -> code(token.source.detectTokenType()) { +token.source }
                    Tokenizer.Token.Type.STRING -> {
                        token.source.forEach { char ->
                            code("token-string") {
                                +char.toString()
                            }
                        }
                    }
                    Tokenizer.Token.Type.WHITESPACE -> Unit // Don't render whitespaces, they get added via the above indent calculation
                    Tokenizer.Token.Type.STATEMENT_DELIMITER -> code("token-comment") { +token.source }
                    Tokenizer.Token.Type.COMMENT -> code("token-comment") { +token.source }
                }
            }
        }
    }
}
