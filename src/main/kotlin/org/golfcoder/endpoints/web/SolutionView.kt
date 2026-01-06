package org.golfcoder.endpoints.web

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.flow.firstOrNull
import org.golfcoder.database.pgpayloads.SolutionTable
import org.golfcoder.database.pgpayloads.getUser
import org.golfcoder.database.pgpayloads.toSolution
import org.golfcoder.plugins.UserSession
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object SolutionView {
    suspend fun download(call: ApplicationCall) = suspendTransaction {
        val solutionFileName = call.parameters["solutionFileName"] ?: throw NotFoundException("No solution specified")
        val (solutionId, fileEnding) = Regex("([a-f0-9]+).([a-z]+)")
            .matchEntire(solutionFileName)
            ?.destructured
            ?: throw NotFoundException("Invalid solution file name")

        val session = call.sessions.get<UserSession>()
        val currentUser = session?.getUser()

        val solution = SolutionTable.selectAll().where(SolutionTable.id eq solutionId).firstOrNull()?.toSolution()
            ?.takeIf { it.codePubliclyVisible || currentUser?.admin == true }
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
