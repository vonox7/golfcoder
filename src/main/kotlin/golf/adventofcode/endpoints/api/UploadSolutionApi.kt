package golf.adventofcode.endpoints.api

import golf.adventofcode.database.Solution
import golf.adventofcode.mainDatabase
import golf.adventofcode.plugins.UserSession
import golf.adventofcode.tokenizer.analyzerThread
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import java.util.*

object UploadSolutionApi {

    const val MAX_CODE_LENGTH = 10_000

    @Serializable
    private class UploadSolutionRequest(
        val year: String,
        val day: String,
        val part: String,
        val code: String,
        val language: Solution.Language,
        val codeIsPublic: String = "off",
        val externalLinks: List<String> = emptyList(), // TODO UI needed (with explanation)
    )

    suspend fun post(call: ApplicationCall) {
        val request = call.receive<UploadSolutionRequest>()
        require(request.code.length <= MAX_CODE_LENGTH)
        require(request.externalLinks.count() <= 3)
        require(request.externalLinks.all { it.length < 300 })

        mainDatabase.getSuspendingCollection<Solution>().insertOne(Solution().apply {
            _id = randomId()
            userId = call.sessions.get<UserSession>()!!.userId
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