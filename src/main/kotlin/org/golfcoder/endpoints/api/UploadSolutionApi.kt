package org.golfcoder.endpoints.api

import com.moshbit.katerbase.equal
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.golfcoder.Sysinfo
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.database.Solution
import org.golfcoder.httpClient
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import java.util.*
import kotlin.reflect.full.primaryConstructor

object UploadSolutionApi {

    const val MAX_CODE_LENGTH = 100_000
    private const val MIN_CODE_LENGTH = 10

    @Serializable
    private class OnecompilerRequest(
        val language: String,
        val stdin: String,
        val files: List<File>,
    ) {
        @Serializable
        class File(
            val name: String,
            val content: String,
        )
    }

    @Serializable
    private class OnecompilerResponse(
        val status: String, // Is even then "success" when there is an exception
        val exception: String? = null, // Populated by Python compiler, but not by Kotlin compiler
        val stdout: String?,
        val stderr: String?,
        val executionTime: Int,
        val limitRemaining: Int,
        val stdin: String,
    )

    @Serializable
    private class UploadSolutionRequest(
        val year: String,
        val day: String,
        val part: String,
        val code: String,
        val language: Solution.Language,
        val codeIsPublic: String = "off",
        val externalLinks: List<String> = emptyList(), // TODO UI needed (with explanation)
        val onlyTokenize: String,
    )

    // TODO restructure into multiple functions/classes (especially "executeCode()")
    suspend fun post(call: ApplicationCall) {
        val request = call.receive<UploadSolutionRequest>()
        val userSession = call.sessions.get<UserSession>()!!
        val now = Date()

        // Validate user input
        require(request.code.length <= MAX_CODE_LENGTH)
        if (request.code.length <= MIN_CODE_LENGTH) {
            call.respond(ApiCallResult(buttonText = "Forgot to paste your code?"))
            return
        }
        require(request.externalLinks.count() <= 3)
        require(request.externalLinks.all { it.length < 300 })

        // Tokenize
        val tokenizer = request.language.tokenizerClass.primaryConstructor!!.call()
        val tokenCount = try {
            val tokens = tokenizer.tokenize(request.code)
            if (Sysinfo.isLocal) {
                println(tokens.joinToString("\n"))
            }
            tokenizer.getTokenCount(tokens)
        } catch (e: Exception) {
            // Could either be a syntax error or a tokenizer error (like network issue, bug in our code...)
            call.respond(ApiCallResult(buttonText = "Tokenizer failed", alertText = "Tokenizer failed:\n${e.message}"))
            return
        }

        if (request.onlyTokenize == "on") {
            call.respond(
                ApiCallResult(
                    buttonText = "$tokenCount tokens. Click here to submit to the leaderboard.",
                    resetButtonTextSeconds = null,
                    changeInput = mapOf("onlyTokenize" to "off"),
                )
            )
            return
        }

        val expectedOutput = mainDatabase.getSuspendingCollection<ExpectedOutput>()
            .findOne(
                ExpectedOutput::year equal request.year.toInt(),
                ExpectedOutput::day equal request.day.toInt(),
                ExpectedOutput::part equal request.part.toInt(),
            ) ?: run {
            call.respond(
                ApiCallResult(
                    buttonText = "Please wait",
                    resetButtonTextSeconds = null,
                    alertText = "Please wait a few hours until submitting your solution for this day.\n" +
                            "We need to solve it first ;).",
                    changeInput = mapOf("onlyTokenize" to "on"),
                )
            )
            return
        }

        val onecompilerResponse = httpClient.post("https://onecompiler-apis.p.rapidapi.com/api/v1/run") {
            contentType(ContentType.Application.Json)
            header("X-RapidAPI-Key", System.getenv("RAPIDAPI_KEY"))
            header("X-RapidAPI-Host", "onecompiler-apis.p.rapidapi.com")
            setBody(
                OnecompilerRequest(
                    language = request.language.onecompilerLanguageId,
                    stdin = expectedOutput.input,
                    files = listOf(
                        OnecompilerRequest.File(
                            name = "index.${request.language.fileEnding}",
                            content = request.code
                        )
                    )
                )
            )
        }.body<OnecompilerResponse>()

        val onecompilerException = (onecompilerResponse.stderr?.takeIf { it.isNotEmpty() }
            ?: onecompilerResponse.exception?.takeIf { it.isNotEmpty() })
            // Sanitize output from OneCompiler (Kotlin)
            ?.removePrefix("OpenJDK 64-Bit Server VM warning: Options -Xverify:none and -noverify were deprecated in JDK 13 and will likely be removed in a future release.\n")

        println(
            "Onecompiler limit remaining: ${onecompilerResponse.limitRemaining}. " +
                    "Execution time: ${onecompilerResponse.executionTime}ms for " +
                    "${request.code.length} characters in ${request.language} from ${userSession.userId})" +
                    if (onecompilerException == null) "" else " (execution failed)"
        )

        if (onecompilerException != null) {
            call.respond(
                ApiCallResult(
                    buttonText = "Calculate tokens",
                    resetButtonTextSeconds = null,
                    changeInput = mapOf("onlyTokenize" to "on"),
                    alertText = "Code execution failed:\n\n${onecompilerException}"
                )
            )
            return
        }

        val outputNumber = onecompilerResponse.stdout?.trim()?.toLongOrNull()
        if (outputNumber == null) {
            call.respond(
                ApiCallResult(
                    buttonText = "Calculate tokens",
                    resetButtonTextSeconds = null,
                    changeInput = mapOf("onlyTokenize" to "on"),
                    alertText = "Wrong stdout. Expected a number, but got \"${onecompilerResponse.stdout?.trim()}\"."
                )
            )
            return
        }
        // Validate for correct outputNumber
        if (outputNumber != expectedOutput.output) {
            call.respond(
                ApiCallResult(
                    buttonText = "Calculate tokens",
                    resetButtonTextSeconds = null,
                    changeInput = mapOf("onlyTokenize" to "on"),
                    alertText = "Wrong stdout. The number does not match the expected output.\n" +
                            "If you think this is a bug, please report it to Golfcoder (see About & FAQ)."
                )
            )
            return
        }

        // Save solution to database
        mainDatabase.getSuspendingCollection<Solution>().insertOne(Solution().apply {
            _id = randomId()
            userId = userSession.userId
            year = request.year.toInt()
            day = request.day.toInt()
            part = request.part.toInt()
            code = request.code
            language = request.language
            codePubliclyVisible = request.codeIsPublic == "on"
            externalLinks = request.externalLinks
            uploadDate = now
            this.tokenCount = tokenCount
            tokenizerVersion = language.tokenizerVersion
        }, upsert = false)

        call.respond(ApiCallResult(buttonText = "Submitted", reloadSite = true))
    }
}