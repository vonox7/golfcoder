package org.golfcoder.endpoints.api

import com.moshbit.katerbase.MongoMainEntry.Companion.generateId
import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.sentry.Sentry
import kotlinx.coroutines.flow.toList
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlinx.serialization.Serializable
import org.golfcoder.Sysinfo
import org.golfcoder.coderunner.Coderunner
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.database.LeaderboardPosition
import org.golfcoder.database.Solution
import org.golfcoder.database.User
import org.golfcoder.endpoints.web.LeaderboardDayView
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.golfcoder.tokenizer.Tokenizer
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

    // Returns e.g. "1*****7890" for "1234567890" to prevent cheating but still provide a good UX
    private fun maskOutput(output: String): String {
        if (output.length < 5) {
            return "*".repeat(output.length)
        }
        return output
            .mapIndexed { index, char -> if (index == 0 || index > output.length / 2) char else '*' }
            .joinToString("")
    }

    suspend fun post(call: ApplicationCall) {
        val request = call.receive<UploadSolutionRequest>()
        val userSession = call.sessions.get<UserSession>()
        val currentUser =
            userSession?.let { mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal it.userId) }

        // Redirect to login screen if "login to submit" was pressed
        if (userSession == null && request.submitState == SubmitState.LOGIN_TO_SUBMIT) {
            call.respond(ApiCallResult(redirect = "/login"))
            return
        }

        // Validate user input
        require(request.code.length <= MAX_CODE_LENGTH)
        if (request.code.length <= MIN_CODE_LENGTH) {
            call.respond(ApiCallResult(buttonText = "Forgot to paste your code?"))
            return
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
            return
        }

        // Log to user object
        if (userSession != null)
            mainDatabase.getSuspendingCollection<User>().updateOne(User::_id equal userSession.userId) {
                User::defaultLanguage setTo request.language
                User::tokenizedCodeCount incrementBy 1
                if (request.submitState == SubmitState.SUBMIT_NOW) {
                    User::codeRunCount incrementBy 1
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
            return
        } else {
            requireNotNull(userSession)
        }

        val expectedOutputs = mainDatabase.getSuspendingCollection<ExpectedOutput>()
            .find(
                ExpectedOutput::year equal request.year.toInt(),
                ExpectedOutput::day equal request.day.toInt(),
                ExpectedOutput::part equal request.part.toInt(),
            )
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
        expectedOutput: ExpectedOutput,
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

        call.respond(when {
            codeRunnerStdout == expectedOutput.output -> {
                // Correct solution. Save solution to database
                val solution = Solution().apply {
                    _id = randomId()
                    userId = userSession.userId
                    year = request.year.toInt()
                    day = request.day.toInt()
                    part = request.part.toInt()
                    code = request.code
                    language = request.language
                    codePubliclyVisible = request.codeIsPublic == "on"
                    uploadDate = Date()
                    this.tokenCount = tokenCount
                    tokenizerVersion = tokenizer.tokenizerVersion
                }
                mainDatabase.getSuspendingCollection<Solution>().insertOne(solution, upsert = false)

                recalculateScore(year = request.year.toInt(), day = request.day.toInt())

                ApiCallResult(
                    buttonText = "Submitted",
                    redirect = "/${request.year}/day/${request.day}?solution=${solution._id}"
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
                        code { if (currentUser?.admin == true) +expectedOutput.output else +maskOutput(expectedOutput.output) }
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

    suspend fun recalculateScore(year: Int, day: Int) {
        // The whole recalculation could be done also with 1 single aggregate query?
        val solutions = PART_RANGE.map { part ->
            @Suppress("DEPRECATION") // use excludeFields - only `code` is excluded since it is too big
            mainDatabase.getSuspendingCollection<Solution>()
                .find(
                    Solution::year equal year,
                    Solution::day equal day,
                    Solution::part equal part,
                    Solution::markedAsCheated equal false
                )
                .sortBy(Solution::tokenCount)
                .excludeFields(Solution::code)
                .limit(200)
                .toList()
        }

        // We need to do some transformations here, so we get per user the best solution per part (also per language).
        // And to calculate the total score (for all parts) (per user+language).
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
            PART_RANGE.sumOf { part ->
                userSolutions.find { it.part == part }?.tokenCount
                    ?: 10_000 // No solution for part yet submitted
            }
        }.toList().sortedBy { it.second }

        mainDatabase.getSuspendingCollection<LeaderboardPosition>().bulkWrite {
            deleteMany(LeaderboardPosition::year equal year, LeaderboardPosition::day equal day)
            sortedScores.forEachIndexed { scoreIndex, (solutions, score) ->
                insertOne(LeaderboardPosition().also { leaderboardPosition ->
                    leaderboardPosition._id = generateId(
                        solutions.first().userId,
                        solutions.first().language.name,
                        year.toString(),
                        day.toString()
                    )
                    leaderboardPosition.year = year
                    leaderboardPosition.day = day
                    leaderboardPosition.position = scoreIndex + 1
                    leaderboardPosition.userId = solutions.first().userId
                    leaderboardPosition.language = solutions.first().language
                    leaderboardPosition.tokenSum = score
                    leaderboardPosition.partInfos = solutions.associate { solution ->
                        solution.part to LeaderboardPosition.PartInfo(
                            tokens = solution.tokenCount,
                            solutionId = solution._id,
                            codePubliclyVisible = solution.codePubliclyVisible,
                            uploadDate = solution.uploadDate,
                        )
                    }
                }, upsert = true)
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