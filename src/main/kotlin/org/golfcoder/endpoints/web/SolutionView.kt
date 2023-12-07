package org.golfcoder.endpoints.web

import com.moshbit.katerbase.equal
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import org.golfcoder.database.Solution
import org.golfcoder.mainDatabase

object SolutionView {
    suspend fun download(call: ApplicationCall) {
        val solutionFileName = call.parameters["solutionFileName"] ?: throw NotFoundException("No solution specified")
        val (solutionId, fileEnding) = Regex("([a-f0-9]+).([a-z]+)")
            .matchEntire(solutionFileName)
            ?.destructured
            ?: throw NotFoundException("Invalid solution file name")

        val solution = mainDatabase.getSuspendingCollection<Solution>()
            .findOne(Solution::_id equal solutionId, Solution::codePubliclyVisible equal true)
            ?: throw NotFoundException("Solution not found or not publicly visible")

        if (fileEnding != solution.language.fileEnding) throw NotFoundException("Invalid file ending")

        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, solutionFileName)
                .toString()
        )
        call.respondText(contentType = ContentType.Text.Any, text = solution.code)
    }
}
