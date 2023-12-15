package org.golfcoder.endpoints.web

import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.golfcoder.database.User
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession

object DeleteUserView {
    suspend fun getHtml(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()!!
        val currentUser = mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal session.userId)!!

        call.respondHtmlView("Delete Golfcoder account") {
            h1 {
                +"Delete Golfcoder account ${currentUser.name}"
            }
            form(action = "/api/user/delete") {
                p {
                    +"Are you sure you want to delete your Golfcoder account?"
                }
                p {
                    +"You can also "
                    a(href = "/login") { +"link multiple OAuth2 providers" }
                    +" with your Golfcoder account and "
                    a(href = "/login") { +"unlink them again" }
                    +"."
                }

                label("checkbox-container") {
                    +"Keep my submissions in an anonymized form. They will get associated with \"anonymous\"."
                    input(type = InputType.checkBox) {
                        name = "keepSubmissions"
                        checked = true
                        span("checkbox")
                    }
                }

                label {
                    attributes["for"] = "name"
                    +"Type your user name"
                    b { +" (${currentUser.name})" }
                    +" to confirm account deletion: "
                    input(type = InputType.text) {
                        name = "name"
                    }
                }

                p {
                    +"This action cannot be undone."
                    br()
                    +"You will be logged out after deleting your account."
                    br()
                    +"You can then create a new Golfcoder account with the same name and the same OAuth2 provider."
                }

                input(type = InputType.submit) {
                    name = "submitButton"
                    onClick = "submitForm(event)"
                    value = "Delete Golfcoder account"
                }
            }
        }
    }
}