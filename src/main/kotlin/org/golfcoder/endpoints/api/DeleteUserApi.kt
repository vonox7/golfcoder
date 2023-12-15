package org.golfcoder.endpoints.api

import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.golfcoder.database.Solution
import org.golfcoder.database.User
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession

object DeleteUserApi {

    @Serializable
    private class DeleteUserRequest(
        val keepSubmissions: String = "off",
        val name: String,
    )

    suspend fun post(call: ApplicationCall) {
        val request = call.receive<DeleteUserRequest>()
        val session = call.sessions.get<UserSession>()!!
        val currentUser = mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal session.userId)!!

        if (request.name != currentUser.name) {
            call.respond(ApiCallResult(buttonText = "Wrong user name"))
            return
        }

        if (request.keepSubmissions != "on") {
            val deleteSubmissionsResult = mainDatabase.getSuspendingCollection<Solution>()
                .deleteMany(Solution::userId equal session.userId)
            if (deleteSubmissionsResult.deletedCount > 0L) {
                UploadSolutionApi.recalculateAllScores()
            }
        }

        mainDatabase.getSuspendingCollection<User>().deleteOne(User::_id equal session.userId)

        call.sessions.clear<UserSession>()

        call.respond(ApiCallResult(buttonText = "Deleted user", redirect = "/"))
    }
}