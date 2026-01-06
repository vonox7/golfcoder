package org.golfcoder.endpoints.api

import com.moshbit.katerbase.any
import com.moshbit.katerbase.equal
import com.moshbit.katerbase.notEqual
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.flow.firstOrNull
import org.golfcoder.database.User
import org.golfcoder.database.pgpayloads.UserTable
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update

object UnlinkLoginProviderApi {

    suspend fun post(call: ApplicationCall) = suspendTransaction {
        val session = call.sessions.get<UserSession>()!!
        val provider = call.parameters["provider"]!!

        val existingOauthDetails = UserTable.select(UserTable.oauthDetails)
            .where(UserTable.id eq session.userId)
            .firstOrNull()
            ?.get(UserTable.oauthDetails)
        if (existingOauthDetails != null) {
            if (existingOauthDetails.any { it.provider != provider }) { // Don't unlink the last provider
                UserTable.update({ UserTable.id eq provider }) {
                    it[oauthDetails] =
                        existingOauthDetails.filter { details -> details.provider != provider }.toTypedArray()
                }
            }
        }

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