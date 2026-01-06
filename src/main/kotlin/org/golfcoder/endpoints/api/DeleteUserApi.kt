package org.golfcoder.endpoints.api

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.golfcoder.database.pgpayloads.SolutionTable
import org.golfcoder.database.pgpayloads.UserTable
import org.golfcoder.plugins.UserSession
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object DeleteUserApi {

    @Serializable
    private class DeleteUserRequest(
        val keepSubmissions: String = "off",
        val name: String,
    )

    suspend fun post(call: ApplicationCall) = suspendTransaction {
        val request = call.receive<DeleteUserRequest>()
        val session = call.sessions.get<UserSession>()!!

        if (request.name != session.displayName) {
            call.respond(ApiCallResult(buttonText = "Wrong user name"))
            return@suspendTransaction
        }

        if (request.keepSubmissions != "on") {
            val deleteSubmissionsDeleteCount = SolutionTable.deleteWhere { SolutionTable.userId eq session.userId }
            if (deleteSubmissionsDeleteCount > 0L) {
                UploadSolutionApi.recalculateAllScores()
            }
        }

        UserTable.deleteWhere { UserTable.id eq session.userId }

        call.sessions.clear<UserSession>()

        call.respond(ApiCallResult(buttonText = "Deleted user", redirect = "/"))
    }
}