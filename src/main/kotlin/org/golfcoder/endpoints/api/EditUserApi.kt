package org.golfcoder.endpoints.api

import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.golfcoder.database.User
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession

object EditUserApi {

    @Serializable
    private class EditUserRequest(val nameIsPublic: String = "off")

    suspend fun post(call: ApplicationCall) {
        val request = call.receive<EditUserRequest>()

        mainDatabase.getSuspendingCollection<User>()
            .updateOne(User::_id equal call.sessions.get<UserSession>()!!.userId) {
                User::nameIsPublic setTo (request.nameIsPublic == "on")
            }

        call.respond(ApiCallResult(buttonText = "Saved"))
    }
}