package org.golfcoder.endpoints.web

import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.golfcoder.database.User
import org.golfcoder.endpoints.api.EditUserApi
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.golfcoder.utils.relativeToNow


object EditUserView {
    suspend fun getHtml(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()!!
        val userProfile = mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal session.userId)!!

        call.respondHtmlView("Golfcoder ${userProfile.name}") {
            h1 {
                renderUserProfileImage(userProfile, big = true)
                +userProfile.name
            }
            p {
                +"User was created ${userProfile.createdOn.relativeToNow}. "
            }
            form(action = "/api/user/edit") {
                label {
                    attributes["for"] = "name"
                    +"Name: "
                    input(type = InputType.text) {
                        name = "name"
                        value = userProfile.name
                        maxLength = EditUserApi.MAX_USER_NAME_LENGTH.toString()
                    }
                }

                label("checkbox-container") {
                    +"Show my name on the leaderboard (${userProfile.name} vs \"anonymous\")"
                    input(type = InputType.checkBox) {
                        name = "nameIsPublic"
                        checked = userProfile.nameIsPublic
                        span("checkbox")
                    }
                }

                label("checkbox-container") {
                    +"Show my profile picture on the leaderboard (or use just my initials)"
                    input(type = InputType.checkBox) {
                        name = "profilePictureIsPublic"
                        checked = userProfile.profilePictureIsPublic
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