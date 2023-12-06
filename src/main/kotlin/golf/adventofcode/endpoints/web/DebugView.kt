package golf.adventofcode.endpoints.web

import golf.adventofcode.Sysinfo
import golf.adventofcode.database.User
import golf.adventofcode.mainDatabase
import golf.adventofcode.plugins.UserSession
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.util.*

object DebugView {
    // Helpful for debugging locally, without having any secrets from an oauth provider
    suspend fun createRandomUser(call: ApplicationCall) {
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

        call.sessions.set(UserSession(newUser._id, newUser.name))
        call.respondRedirect("/")
    }
}