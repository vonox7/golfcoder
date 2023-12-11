package org.golfcoder.endpoints.web

import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.flow.toList
import kotlinx.html.*
import org.golfcoder.database.Solution
import org.golfcoder.database.User
import org.golfcoder.endpoints.api.EditUserApi
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.golfcoder.utils.relativeToNow


object EditUserView {
    suspend fun getHtml(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()!!
        val currentUser = mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal session.userId)!!

        val mySolutions = mainDatabase.getSuspendingCollection<Solution>()
            .find(Solution::userId equal session.userId)
            .sortByDescending(Solution::uploadDate)
            .toList()

        call.respondHtmlView("Golfcoder ${currentUser.name}") {
            h1 {
                renderUserProfileImage(currentUser, big = true)
                +currentUser.name
            }
            p {
                +"You joined Golfcoder ${currentUser.createdOn.relativeToNow}. "
            }
            form(action = "/api/user/edit") {
                label {
                    attributes["for"] = "name"
                    +"Name: "
                    input(type = InputType.text) {
                        name = "name"
                        value = currentUser.name
                        maxLength = EditUserApi.MAX_USER_NAME_LENGTH.toString()
                    }
                }

                label("checkbox-container") {
                    +"Show my name on the leaderboard (${currentUser.name} vs \"anonymous\")"
                    input(type = InputType.checkBox) {
                        name = "nameIsPublic"
                        checked = currentUser.nameIsPublic
                        span("checkbox")
                    }
                }

                label("checkbox-container") {
                    +"Show my profile picture on the leaderboard (or use just my initials)"
                    input(type = InputType.checkBox) {
                        name = "profilePictureIsPublic"
                        checked = currentUser.profilePictureIsPublic
                        span("checkbox")
                    }
                }

                input(type = InputType.submit) {
                    onClick = "submitForm(event)"
                    value = "Save"
                }
            }

            h2 { +"My solutions" }
            if (mySolutions.isEmpty()) {
                p { +"No solutions uploaded yet." }
            } else {
                renderMySolutions(mySolutions)
            }
        }
    }

    private fun HtmlBlockTag.renderMySolutions(solutions: List<Solution>) {
        table("leaderboard") {
            thead {
                tr {
                    th(classes = "left-align") { +"Language" }
                    th(classes = "right-align") { +"Year" }
                    th(classes = "right-align") { +"Day" }
                    th(classes = "right-align") { +"Part" }
                    th(classes = "right-align") { +"Tokens" }
                    th(classes = "right-align") { +"Date" }
                }
            }
            tbody {
                // Render leaderboard
                solutions.forEach { solution ->
                    tr {
                        td("left-align") { +solution.language.displayName }
                        td("right-align") { +solution.year.toString() }
                        td("right-align") { +solution.day.toString() }
                        td("right-align") { +solution.part.toString() }
                        td("right-align") {
                            a(href = "/${solution.year}/day/${solution.day}?solution=${solution._id}#solution") {
                                +solution.tokenCount.toString()
                            }
                        }
                        td("right-align") { +solution.uploadDate.relativeToNow }
                    }
                }
            }
        }
    }

}