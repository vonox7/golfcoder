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
    const val MAX_USER_NAME_LENGTH = 50

    @Serializable
    private class EditUserRequest(val name: String = "", val nameIsPublic: String = "off")

    suspend fun post(call: ApplicationCall) {
        val request = call.receive<EditUserRequest>()
        val session = call.sessions.get<UserSession>()!!

        val newName = request.name.take(MAX_USER_NAME_LENGTH).trim().takeIf { it.isNotEmpty() } ?: "XXX"

        mainDatabase.getSuspendingCollection<User>()
            .updateOne(User::_id equal session.userId) {
                User::name setTo newName
                User::nameIsPublic setTo (request.nameIsPublic == "on")
            }

        call.sessions.set(UserSession(session.userId, newName))

        call.respond(ApiCallResult(buttonText = "Saved", reloadSite = true))
    }
}