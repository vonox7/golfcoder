package org.golfcoder.endpoints.web

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import org.golfcoder.Sysinfo
import org.golfcoder.database.pgpayloads.Solution
import org.golfcoder.database.pgpayloads.UserTable
import org.golfcoder.database.pgpayloads.UserTable.OAuthDetails
import org.golfcoder.plugins.UserSession
import org.golfcoder.utils.randomId
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object DebugView {
    // Helpful for debugging locally, without having any secrets from an oauth provider
    suspend fun createRandomUser(call: ApplicationCall) = suspendTransaction {
        require(Sysinfo.isLocal)

        call.sessions.clear<UserSession>()

        // Create new random user
        val userId = randomId()
        val userName =
            listOf("John", "Jane", "Max", "Anna", "Peter", "Maria", "Paul", "Sarah", "Tim", "Lisa").random() +
                    " " +
                    listOf("Doe", "Smith", "Taylor", "Jackson", "White", "Harris", "Martin", "Black").random()

        UserTable.insert {
            it[id] = userId
            it[oauthDetails] = emptyArray<OAuthDetails>()
            it[name] = userName
            it[publicProfilePictureUrl] = null
            it[admin] = true
            it[defaultLanguage] = Solution.Language.KOTLIN
        }

        call.sessions.set(UserSession(userId, userName))
        call.respondRedirect("/")
    }
}