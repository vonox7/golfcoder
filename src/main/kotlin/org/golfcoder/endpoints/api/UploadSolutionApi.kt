package org.golfcoder.endpoints.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.golfcoder.database.Solution
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.golfcoder.tokenizer.analyzerThread
import java.util.*

object UploadSolutionApi {

    const val MAX_CODE_LENGTH = 100_000
    private const val MIN_CODE_LENGTH = 10

    private val onecompilerHttpClient = HttpClient(CIO) {
        engine {
            maxConnectionsCount = 5 // Make sure we don't overload the onecompiler API
        }
        install(ContentNegotiation) {
            json()
        }
    }

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
        val exception: String?,
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
        val input: String, // TODO remove and load from database
        val language: Solution.Language,
        val codeIsPublic: String = "off",
        val externalLinks: List<String> = emptyList(), // TODO UI needed (with explanation)
    )

    suspend fun post(call: ApplicationCall) {
        val request = call.receive<UploadSolutionRequest>()
        val userSession = call.sessions.get<UserSession>()!!

        // Validate user input
        require(request.code.length <= MAX_CODE_LENGTH)
        if (request.code.length <= MIN_CODE_LENGTH) {
            call.respond(ApiCallResult(buttonText = "Forgot to paste your code?"))
            return
        }
        require(request.externalLinks.count() <= 3)
        require(request.externalLinks.all { it.length < 300 })

        // Execute code via Onecompiler
        val onecompilerResponse = onecompilerHttpClient.post("https://onecompiler-apis.p.rapidapi.com/api/v1/run") {
            contentType(ContentType.Application.Json)
            header("X-RapidAPI-Key", System.getenv("RAPIDAPI_KEY"))
            header("X-RapidAPI-Host", "onecompiler-apis.p.rapidapi.com")
            setBody(
                OnecompilerRequest(
                    language = request.language.onecompilerLanguageId,
                    stdin = request.input,
                    files = listOf(
                        OnecompilerRequest.File(
                            name = "index.${request.language.fileEnding}",
                            content = request.code
                        )
                    )
                )
            )
        }.body<OnecompilerResponse>()

        println(
            "Limit remaining: ${onecompilerResponse.limitRemaining} (execution time: ${onecompilerResponse.executionTime}ms for " +
                    "${request.code.length} characters in ${request.language} from ${userSession.userId})" +
                    if (onecompilerResponse.exception == null) " (execution failed)" else ""
        )

        if (onecompilerResponse.exception != null) {
            call.respond(
                ApiCallResult(
                    buttonText = "Code execution failed",
                    alertText = "Code execution failed:\n\n${onecompilerResponse.stderr ?: onecompilerResponse.exception}"
                )
            )
            return
        }

        val outputNumber = onecompilerResponse.stdout?.trim()?.toLongOrNull()
        if (outputNumber == null) {
            call.respond(
                ApiCallResult(
                    buttonText = "Wrong stdout",
                    alertText = "Wrong stdout. Expected a number, but got \"${onecompilerResponse.stdout?.trim()}\"."
                )
            )
            return
        }
        // TODO validate for correct outputNumber

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
            uploadDate = Date()
        }, upsert = false)

        // Trigger instantly analysis
        analyzerThread.interrupt()

        call.respond(ApiCallResult(buttonText = "Submitted", reloadSite = true))
    }
}