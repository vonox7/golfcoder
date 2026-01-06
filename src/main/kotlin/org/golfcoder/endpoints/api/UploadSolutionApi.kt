package org.golfcoder.endpoints.api

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.sentry.Sentry
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlinx.serialization.Serializable
import org.golfcoder.Sysinfo
import org.golfcoder.coderunner.Coderunner
import org.golfcoder.database.Solution
import org.golfcoder.database.pgpayloads.*
import org.golfcoder.endpoints.web.LeaderboardDayView
import org.golfcoder.plugins.UserSession
import org.golfcoder.tokenizer.Tokenizer
import org.golfcoder.utils.randomId
import org.golfcoder.utils.toJavaDate
import org.golfcoder.utils.toKotlinLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder.DESC
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.time.LocalDate
import java.time.Month
import java.util.*

object UploadSolutionApi {

    const val MAX_CODE_LENGTH = 100_000
    val YEARS_RANGE: IntRange
        get() = 2015..LocalDate.now().let { if (it.month == Month.DECEMBER) it.year else it.year - 1 }
    val PART_RANGE = 1..2
    private const val MIN_CODE_LENGTH = 10

    fun getDaysRange(year: Int): IntRange {
        return if (year <= 2024) {
            1..25
        } else {
            1..12
        }
    }

    enum class SubmitState {
        ONLY_TOKENIZE,
        SUBMIT_NOW,
        LOGIN_TO_SUBMIT,
    }

    @Serializable
    private class UploadSolutionRequest(
        val year: String,
        val day: String,
        val part: String,
        val code: String,
        val language: Solution.Language,
        val codeIsPublic: String = "off",
        val submitState: SubmitState,
    )

    private class ExpectedOutputRow(val input: String, val output: String)

    // Returns e.g. "1*****7890" for "1234567890" to prevent cheating but still provide a good UX
    private fun maskOutput(output: String): String {
        if (output.length < 5) {
            return "*".repeat(output.length)
        }
        return output
            .mapIndexed { index, char -> if (index == 0 || index > output.length / 2) char else '*' }
            .joinToString("")
    }

    suspend fun post(call: ApplicationCall) = suspendTransaction {
        val request = call.receive<UploadSolutionRequest>()
        val userSession = call.sessions.get<UserSession>()
        val currentUser = userSession?.getUser()

        // Redirect to login screen if "login to submit" was pressed
        if (userSession == null && request.submitState == SubmitState.LOGIN_TO_SUBMIT) {
            call.respond(ApiCallResult(redirect = "/login"))
            return@suspendTransaction
        }

        // Validate user input
        require(request.code.length <= MAX_CODE_LENGTH)
        if (request.code.length <= MIN_CODE_LENGTH) {
            call.respond(ApiCallResult(buttonText = "Forgot to paste your code?"))
            return@suspendTransaction
        }

        // Tokenize
        val tokenizer = request.language.tokenizer
        val tokens: List<Tokenizer.Token>
        val tokenCount: Int
        println("Tokenize for user ${userSession?.userId}: ${request.year}/${request.day}/${request.part} ${request.language}:\n${request.code}")
        try {
            tokens = tokenizer.tokenize(request.code)
            if (Sysinfo.isLocal) {
                println(tokens.joinToString("\n"))
            }
            tokenCount = tokenizer.getTokenCount(tokens)
        } catch (e: Exception) {
            Sentry.captureException(e)
            // Could either be a syntax error or a tokenizer error (like network issue, bug in our code...)
            call.respond(ApiCallResult(buttonText = "Tokenizer failed", alertHtml = createHTML().div {
                +"Tokenizer failed:"
                br()
                code { +"${e.message}" }
            }))
            return@suspendTransaction
        }

        // Log to user object
        if (userSession != null) {
            UserTable.update({ UserTable.id.eq(userSession.userId) }) {
                it[defaultLanguage] = request.language
                it.update(tokenizedCodeCount, tokenizedCodeCount + 1)
                if (request.submitState == SubmitState.SUBMIT_NOW) {
                    it.update(codeRunCount, codeRunCount + 1)
                }
            }
        }

        if (request.submitState == SubmitState.ONLY_TOKENIZE) {
            val solutionsHtml = with(LeaderboardDayView) {
                createHTML().div {
                    h2 { +"Preview: $tokenCount tokens" }
                    p { +"Click the submit button again to add this solution to the leaderboard." }
                    renderSolution(tokens)
                }
            }
            call.respond(
                if (call.sessions.get<UserSession>() == null) {
                    ApiCallResult(
                        buttonText = "$tokenCount tokens. Login to submit to the leaderboard.",
                        resetButtonTextSeconds = null,
                        changeInput = mapOf("submitState" to SubmitState.LOGIN_TO_SUBMIT.name),
                        setInnerHtml = mapOf("solution" to solutionsHtml),
                    )
                } else {
                    ApiCallResult(
                        buttonText = "$tokenCount tokens. Click here to submit to the leaderboard.",
                        resetButtonTextSeconds = null,
                        changeInput = mapOf("submitState" to SubmitState.SUBMIT_NOW.name),
                        setInnerHtml = mapOf("solution" to solutionsHtml),
                    )
                }
            )
            return@suspendTransaction
        } else {
            requireNotNull(userSession)
        }
        val expectedOutputs = ExpectedOutputTable
            .select(ExpectedOutputTable.input, ExpectedOutputTable.output)
            .where(
                (ExpectedOutputTable.year eq request.year.toInt()) and
                        (ExpectedOutputTable.day eq request.day.toInt()) and
                        (ExpectedOutputTable.part eq request.part.toInt())
            )
            .map { ExpectedOutputRow(it[ExpectedOutputTable.input], it[ExpectedOutputTable.output]) }
            .toList()
            .sortedByDescending { it.output.length }

        if (expectedOutputs.isEmpty()) {
            call.respond(
                ApiCallResult(
                    buttonText = "Please wait",
                    resetButtonTextSeconds = null,
                    alertHtml = createHTML().div {
                        +"Please wait a few hours until submitting your solution for this day."
                        br()
                        +"We need to solve it first ;)."
                    },
                    changeInput = mapOf("submitState" to SubmitState.ONLY_TOKENIZE.name),
                )
            )
        } else {
            val expectedOutput = expectedOutputs
                .firstOrNull { it.input.length <= request.language.coderunner.stdinCharLimit }

            if (expectedOutput == null) {
                call.respond(
                    ApiCallResult(
                        buttonText = "Language for this puzzle not yet supported.",
                        resetButtonTextSeconds = null,
                        alertHtml = createHTML().div {
                            +"Due to OneCompiler limitations submitting your solution for this day is not yet supported."
                            br()
                            +"Please wait or choose Kotlin to submit your solution for this day."
                        },
                        changeInput = mapOf("submitState" to SubmitState.ONLY_TOKENIZE.name),
                    )
                )
                throw Exception("Stdin too long, think about OneCompiler alternatives for ${request.language}")
            } else {
                // Run code
                println("Run code for user ${userSession.userId}: ${request.year}/${request.day}/${request.part} ${request.language}")
                runCode(call, request, expectedOutput, userSession, currentUser, tokenCount, tokenizer)
            }
        }
    }

    private suspend fun runCode(
        call: ApplicationCall,
        request: UploadSolutionRequest,
        expectedOutput: ExpectedOutputRow,
        userSession: UserSession,
        currentUser: User?,
        tokenCount: Int,
        tokenizer: Tokenizer,
    ) {
        val coderunnerResult = try {
            request.language.coderunner.run(request.code, request.language, expectedOutput.input)
        } catch (e: Exception) {
            Sentry.captureException(Exception("runCode failed for ${request.language}", e))
            Coderunner.RunResult(
                stdout = "",
                error = "Code compilation or execution failed:\n\n${e.message}",
            )
        }

        val codeRunnerStdout = coderunnerResult.stdout.trim().takeIf { it.isNotEmpty() }

        call.respond(
            when {
                codeRunnerStdout == expectedOutput.output -> {
                    // Correct solution. Save solution to database
                    val solutionId = randomId()
                    SolutionTable.insert {
                        it[id] = solutionId
                        it[userId] = userSession.userId
                        it[year] = request.year.toInt()
                        it[day] = request.day.toInt()
                        it[part] = request.part.toInt()
                        it[code] = request.code
                        it[language] = request.language
                        it[codePublicVisible] = request.codeIsPublic == "on"
                        it[this.tokenCount] = tokenCount
                        it[tokenizerVersion] = tokenizer.tokenizerVersion
                    }

                    recalculateScore(year = request.year.toInt(), day = request.day.toInt())

                    ApiCallResult(
                        buttonText = "Submitted",
                        redirect = "/${request.year}/day/${request.day}?solution=${solutionId}"
                    )
                }

                codeRunnerStdout != null -> {
                    // Incorrect solution, but code execution worked
                    ApiCallResult(
                        buttonText = "Calculate tokens",
                        resetButtonTextSeconds = null,
                        changeInput = mapOf("submitState" to SubmitState.ONLY_TOKENIZE.name),
                        alertHtml = createHTML().div {
                            +"Wrong stdout. Expected "
                            code {
                                if (currentUser?.admin == true) +expectedOutput.output else +maskOutput(
                                    expectedOutput.output
                                )
                            }
                            +", but got "
                            code { +codeRunnerStdout }
                            +"."
                            br()
                            br()
                            +"Some characters are masked with *. Please run & validate your code first locally."
                            br()
                            br()
                            +"If you think this is a bug, please report it to Golfcoder on GitHub (see FAQ)."
                        }
                    )
                }

                coderunnerResult.error.isNullOrEmpty() -> {
                    // Code got executed, but no stdout and no error (e.g. missing print statement)
                    ApiCallResult(
                        buttonText = "Calculate tokens",
                        resetButtonTextSeconds = null,
                        changeInput = mapOf("submitState" to SubmitState.ONLY_TOKENIZE.name),
                        alertHtml = createHTML().div {
                            +"Missing stdout. Expected a number. Ensure that your code prints the solution to stdout."
                        }
                    )
                }

                else -> {
                    // Code execution failed
                    ApiCallResult(
                        buttonText = "Calculate tokens",
                        resetButtonTextSeconds = null,
                        changeInput = mapOf("submitState" to SubmitState.ONLY_TOKENIZE.name),
                        alertHtml = createHTML().div {
                            +"Code compilation or execution failed:"
                            br()
                            code { +coderunnerResult.error }
                        }
                    )
                }
            })
    }

    private class SolutionPart(
        val id: String, val userId: String, val language: Solution.Language, val tokenCount: Int,
        val part: Int, val codePubliclyVisible: Boolean = false, val uploadDate: Date
    )

    suspend fun recalculateScore(year: Int, day: Int) = suspendTransaction {
        // The whole recalculation could be done also with 1 single aggregate query?
        val solutions: List<List<SolutionPart>> = PART_RANGE.map { part ->
            SolutionTable.select(
                SolutionTable.id, SolutionTable.userId, SolutionTable.language,
                SolutionTable.tokenCount, SolutionTable.codePublicVisible, SolutionTable.uploadDate
            ).where {
                (SolutionTable.year eq year) and
                        (SolutionTable.day eq day) and
                        (SolutionTable.part eq part) and
                        (SolutionTable.markedAsCheated eq false)
            }
                .orderBy(SolutionTable.tokenCount, DESC)
                .limit(200)
                .map {
                    SolutionPart(
                        id = it[SolutionTable.id],
                        userId = it[SolutionTable.userId],
                        language = it[SolutionTable.language],
                        tokenCount = it[SolutionTable.tokenCount],
                        part = part,
                        codePubliclyVisible = it[SolutionTable.codePublicVisible],
                        uploadDate = it[SolutionTable.uploadDate].toJavaDate()
                    )
                }
                .toList()
        }

        // We need to do some transformations here, so we get per user the best solution per part (also per language).
        // And to calculate the total score (for all parts) (per user+language).
        val scoresByUserId = mutableMapOf<String, List<SolutionPart>>()
        solutions.forEach { solutionsPerPart ->
            solutionsPerPart
                .groupBy { it.userId + it.language }
                .forEach { (userId, solutions) ->
                    scoresByUserId[userId] =
                        (scoresByUserId[userId] ?: emptyList()) + solutions.minBy { it.tokenCount }
                }
        }

        val sortedScores: List<Pair<List<SolutionPart>, Int>> = scoresByUserId.values.associateWith { userSolutions ->
            PART_RANGE.sumOf { part ->
                userSolutions.find { it.part == part }?.tokenCount
                    ?: 10_000 // No solution for part yet submitted
            }
        }.toList().sortedBy { it.second }

        LeaderboardPositionTable.deleteWhere { (LeaderboardPositionTable.year eq year) and (LeaderboardPositionTable.day eq day) }
        var position = 1
        LeaderboardPositionTable.batchInsert(sortedScores) { (solutions, score) ->
            this[LeaderboardPositionTable.year] = year
            this[LeaderboardPositionTable.day] = day
            this[LeaderboardPositionTable.position] = position++
            this[LeaderboardPositionTable.userId] = solutions.first().userId
            this[LeaderboardPositionTable.language] = solutions.first().language
            this[LeaderboardPositionTable.tokenSum] = score
            this[LeaderboardPositionTable.partInfos] = solutions.associate { solution ->
                solution.part to LeaderboardPositionTable.PartInfo(
                    tokens = solution.tokenCount,
                    solutionId = solution.id,
                    codePubliclyVisible = solution.codePubliclyVisible,
                    uploadDate = solution.uploadDate.toKotlinLocalDateTime(),
                )
            }
        }
    }

    suspend fun recalculateAllScores() {
        var leaderboards = 0

        YEARS_RANGE.forEach { year ->
            getDaysRange(year).forEach { day ->
                leaderboards++
                recalculateScore(year, day)
            }
        }

        println("Recalculated all scores for $leaderboards leaderboards.")
    }
}