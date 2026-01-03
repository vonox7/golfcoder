package org.golfcoder.endpoints.web

import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.html.*
import org.golfcoder.database.LeaderboardPosition
import org.golfcoder.database.Solution
import org.golfcoder.database.User
import org.golfcoder.database.pgpayloads.SolutionTable
import org.golfcoder.database.pgpayloads.toSolution
import org.golfcoder.endpoints.api.EditUserApi
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.golfcoder.utils.relativeToNow
import org.jetbrains.exposed.v1.core.SortOrder.DESC
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction


object EditUserView {
    suspend fun getHtml(call: ApplicationCall) = suspendTransaction {
        val session = call.sessions.get<UserSession>()!!
        val currentUser = mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal session.userId)!!

        val mySolutions = mainDatabase.getSuspendingCollection<Solution>()
            .find(Solution::userId equal session.userId)
            .sortByDescending(Solution::uploadDate)
            .toList() +
                SolutionTable
                    .selectAll()
                    .where(SolutionTable.userId eq session.userId)
                    .orderBy(SolutionTable.id, DESC)
                    .map { it.toSolution() }
                    .toList()

        val myLeaderboardPositions = mainDatabase.getSuspendingCollection<LeaderboardPosition>()
            .find(LeaderboardPosition::userId equal session.userId)
            .toList()
            .sortedBy { it.year * 10000 + it.day }
            .groupBy { it.year * 10000 + it.day }

        call.respondHtmlView("Golfcoder ${session.displayName}") {
            h1 {
                renderUserProfileImage(currentUser, big = true)
                +session.displayName
            }
            p {
                +"You joined Golfcoder ${currentUser.createdOn.relativeToNow()}. "
            }

            val currentLoginProviders = currentUser.oAuthDetails.map { it.provider }
            p {
                +"Logged in via ${currentLoginProviders.joinToString { LoginView.oauth2Providers[it]!! }}. "
                a(href = "/login") { +"Add/remove OAuth2 providers." }
            }

            form(action = "/api/user/edit") {
                label {
                    attributes["for"] = "name"
                    +"Name: "
                    input(type = InputType.text) {
                        name = "name"
                        value = session.displayName
                        maxLength = EditUserApi.MAX_USER_NAME_LENGTH.toString()
                    }
                }

                label("checkbox-container") {
                    +"Show my name on the leaderboard (${session.displayName} vs \"anonymous\")"
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

                val singleAocRepositoryUrl = currentUser.adventOfCodeRepositoryInfo?.singleAocRepositoryUrl
                val yearAocRepositoryUrl = currentUser.adventOfCodeRepositoryInfo?.yearAocRepositoryUrl

                label("checkbox-container") {
                    val disabled = singleAocRepositoryUrl == null && yearAocRepositoryUrl.isNullOrEmpty()
                    +"Link my name in the leaderboard with my linked advent-of-code GitHub repository"
                    input(type = InputType.checkBox) {
                        name = "showAdventOfCodeRepositoryLink"
                        checked = currentUser.adventOfCodeRepositoryInfo?.publiclyVisible == true && !disabled
                        this.disabled = disabled
                        span("checkbox")
                    }
                }

                p("text-secondary-info") {
                    +"If your GitHub account is linked, your name in the leaderboard will get automatically linked with your advent-of-code repository. "
                    +"You might name your repository e.g. advent-of-code, my-aoc-solutions, AdventOfCodeInPython or AoC-XXX."
                    br()
                    +"If you add years (e.g. 2023) to your repo names, the corresponding repository will be linked to your leaderboard. "
                    +"So you might name your repositories also advent-of-code-2023, my-aoc2023-solutions, AdventOfCode_2023 or XXX-2023-AoC."
                }
                p {
                    when {
                        singleAocRepositoryUrl == null && yearAocRepositoryUrl.isNullOrEmpty() -> {
                            +"No advent-of-code GitHub repository linked."
                        }

                        singleAocRepositoryUrl != null -> {
                            +"Linked advent-of-code GitHub repository (1 for all years): "
                            a(href = singleAocRepositoryUrl) { +singleAocRepositoryUrl }
                        }

                        else -> {
                            +"Linked advent-of-code GitHub repositories per year:"
                            ul {
                                yearAocRepositoryUrl?.forEach { (year, repositoryUrl) ->
                                    li {
                                        +"$year: "
                                        a(href = repositoryUrl, target = "_blank") { +repositoryUrl }
                                    }
                                }
                            }
                        }
                    }
                }
                p {
                    a(href = "/login/github") {
                        if (currentUser.oAuthDetails.any { it.provider == "github" }) {
                            +"Refresh ${currentUser.adventOfCodeRepositoryInfo?.githubProfileName ?: ""} GitHub repositories"
                        } else {
                            +"Link GitHub account"
                        }
                    }
                }

                input(type = InputType.submit) {
                    onClick = "submitForm(event)"
                    value = "Save"
                }
            }

            h2 { +"My leaderboard positions" }
            if (mySolutions.isEmpty()) {
                p { +"No solutions uploaded yet." }
            } else {
                myLeaderboardPositions.forEach { (_, leaderboardPositionPerDay) ->
                    h3 { +"${leaderboardPositionPerDay.first().year} day ${leaderboardPositionPerDay.first().day}" }
                    with(LeaderboardDayView) {
                        renderLeaderboard(
                            leaderboardPositionPerDay.sortedBy { it.position },
                            userIdsToUsers = mapOf(session.userId to currentUser),
                            currentUser = currentUser
                        )
                    }
                }
            }

            h2 { +"My solutions" }
            if (mySolutions.isEmpty()) {
                p { +"No solutions uploaded yet." }
            } else {
                renderMySolutions(mySolutions)
            }

            h2 { +"User actions" }
            p {
                a(href = "/logout") {
                    +"Logout"
                }
            }
            p {
                a(href = "/user/delete") {
                    +"Delete Golfcoder account"
                }
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
                    th(classes = "right-align") { }
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
                        td("right-align") { +solution.uploadDate.relativeToNow() }
                        td("right-align") {
                            if (solution.markedAsCheated) {
                                +"Flagged as cheated solution."
                                br()
                                +"Please follow the Golfcoder rules "
                                br()
                                +"for a fair competition."
                            }
                        }
                    }
                }
            }
        }
    }

}