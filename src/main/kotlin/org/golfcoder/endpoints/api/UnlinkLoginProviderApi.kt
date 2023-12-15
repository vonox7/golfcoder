package org.golfcoder.endpoints.api

import com.moshbit.katerbase.any
import com.moshbit.katerbase.equal
import com.moshbit.katerbase.notEqual
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import org.golfcoder.database.User
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession

object UnlinkLoginProviderApi {

    suspend fun post(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()!!
        val provider = call.parameters["provider"]!!

        mainDatabase.getSuspendingCollection<User>()
            .updateOne(
                User::_id equal session.userId,
                User::oAuthDetails.any(User.OAuthDetails::provider notEqual provider) // Don't unlink the last provider
            ) {
                User::oAuthDetails.pullWhere(User.OAuthDetails::provider equal provider)
            }

        call.respond(ApiCallResult(buttonText = "Updated", redirect = "/user/edit"))
    }
}