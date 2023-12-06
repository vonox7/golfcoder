package golf.adventofcode.endpoints.api

import com.moshbit.katerbase.equal
import golf.adventofcode.database.User
import golf.adventofcode.mainDatabase
import golf.adventofcode.plugins.UserSession
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable

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