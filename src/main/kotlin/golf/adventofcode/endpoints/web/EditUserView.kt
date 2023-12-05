package golf.adventofcode.endpoints.web

import com.moshbit.katerbase.equal
import golf.adventofcode.database.User
import golf.adventofcode.mainDatabase
import golf.adventofcode.plugins.UserSession
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.html.*

object EditUserView {
    suspend fun getHtml(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()!!
        val userProfile = mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal session.userId)!!

        call.respondHtmlView("Advent of Code ${session.displayName}") {
            h1 { +session.displayName }
            form(action = "/api/user/edit") {
                label("checkbox-container") {
                    +"Show my name on the leaderboard (${session.displayName} vs \"anonymous\")"
                    input(type = InputType.checkBox) {
                        name = "nameIsPublic"
                        checked = userProfile.nameIsPublic
                        span("checkbox")
                    }
                }

                input(type = InputType.submit) {
                    onClick = "submitForm(event)"
                    value = "Save"
                }
            }
        }
    }
}