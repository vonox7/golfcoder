package org.golfcoder.endpoints.web

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import org.golfcoder.Sysinfo
import org.golfcoder.database.User
import org.golfcoder.database.pgpayloads.UserTable
import org.golfcoder.database.pgpayloads.UserTable.OAuthDetails
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.util.*

object DebugView {
    // Helpful for debugging locally, without having any secrets from an oauth provider
    suspend fun createRandomUser(call: ApplicationCall) = suspendTransaction {
        require(Sysinfo.isLocal)

        call.sessions.clear<UserSession>()

        // Create new random user
        val date = Date()
        val newUser = User().apply {
            _id = randomId()
            oAuthDetails = emptyList()
            createdOn = date
            name = listOf("John", "Jane", "Max", "Anna", "Peter", "Maria", "Paul", "Sarah", "Tim", "Lisa").random() +
                    " " +
                    listOf("Doe", "Smith", "Taylor", "Jackson", "White", "Harris", "Martin", "Black").random()
            publicProfilePictureUrl = null
        }
        mainDatabase.getSuspendingCollection<User>().insertOne(newUser, upsert = false)

        UserTable.insert {
            it[id] = newUser._id // TODO randomId()
            it[oauthDetails] = emptyArray<OAuthDetails>()
            it[name] = newUser.name // TODO inline
            it[publicProfilePictureUrl] = null
        }

        call.sessions.set(UserSession(newUser._id, newUser.name))
        call.respondRedirect("/")
    }
}