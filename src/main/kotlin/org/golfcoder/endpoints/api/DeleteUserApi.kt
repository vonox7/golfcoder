package org.golfcoder.endpoints.api

import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.golfcoder.database.Solution
import org.golfcoder.database.User
import org.golfcoder.database.pgpayloads.SolutionTable
import org.golfcoder.database.pgpayloads.UserTable
import org.golfcoder.mainDatabase
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
            val deleteSubmissionsDeleteCount = mainDatabase.getSuspendingCollection<Solution>()
                .deleteMany(Solution::userId equal session.userId).deletedCount +
                    SolutionTable.deleteWhere { SolutionTable.userId eq session.userId }
            if (deleteSubmissionsDeleteCount > 0L) {
                UploadSolutionApi.recalculateAllScores()
            }
        }

        mainDatabase.getSuspendingCollection<User>().deleteOne(User::_id equal session.userId)
        UserTable.deleteWhere { UserTable.id eq session.userId }

        call.sessions.clear<UserSession>()

        call.respond(ApiCallResult(buttonText = "Deleted user", redirect = "/"))
    }
}