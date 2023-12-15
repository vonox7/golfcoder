package org.golfcoder.endpoints.web

import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.flow.toList
import kotlinx.html.*
import org.golfcoder.database.LeaderboardPosition
import org.golfcoder.database.getUserProfiles
import org.golfcoder.endpoints.api.UploadSolutionApi
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.golfcoder.utils.relativeToNow

object LeaderboardYearView {
    suspend fun getHtml(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()
        val year = 2000 + (call.parameters["year"]?.toIntOrNull() ?: throw NotFoundException("Invalid year"))
        val positionOneLeaderboardPositions = mainDatabase.getSuspendingCollection<LeaderboardPosition>()
            .find(LeaderboardPosition::year equal year, LeaderboardPosition::position equal 1)
            .toList()
            .associateBy { it.day }
        val positionOneUserIdsToUsers =
            getUserProfiles(positionOneLeaderboardPositions.values.map { it.userId }.toSet())

        call.respondHtmlView("Advent of Code Leaderboard $year") {
            h1 { +"Advent of Code Leaderboard $year" }

            p {
                +"A code golf community leaderboard for "
                a("https://adventofcode.com", "_blank") { +"adventofcode.com" }
                +", with a focus on code size. "
                +"Every name, including variables and functions, is considered as a single token, irrespective of its length. "
                a("/about") { +"More about Golfcoder" }
                +"."
            }

            table("leaderboard") {
                thead {
                    tr {
                        th {}
                        th(classes = "left-align") { +"Highscore" }
                        th(classes = "left-align") { +"Language" }
                        th(classes = "right-align") { +"Tokens Sum" }
                        UploadSolutionApi.PART_RANGE.forEach { part ->
                            th(classes = "right-align") { +"Tokens Part $part" }
                        }
                        th(classes = "right-align mobile-hidden") { +"Last change" }
                    }
                }
                tbody {
                    UploadSolutionApi.DAYS_RANGE.forEach { day ->
                        val positionOne = positionOneLeaderboardPositions[day]

                        tr {
                            td("text-big") {
                                a("/$year/day/$day") { +"Day $day" }
                            }

                            if (positionOne == null) {
                                td("text-secondary-info") {
                                    colSpan = "6"
                                    +" No submissions yet"
                                }
                            } else {
                                td {
                                    // All solutions in sortedScores have per entry the same user
                                    val user = positionOneUserIdsToUsers[positionOne.userId]
                                    renderUserProfileImage(user, big = false)
                                    val userName = (user?.name?.takeIf { user.nameIsPublic } ?: "anonymous")
                                    val adventOfCodeRepositoryUrl =
                                        user?.getAdventOfCodeRepositoryUrl(year)?.takeIf { user.nameIsPublic }
                                    if (adventOfCodeRepositoryUrl == null) {
                                        +userName
                                    } else {
                                        a(href = adventOfCodeRepositoryUrl, target = "_blank") { +userName }
                                    }
                                }
                                td {
                                    +positionOne.language.displayName
                                }
                                td("right-align") {
                                    +positionOne.tokenSum.toString()
                                }

                                UploadSolutionApi.PART_RANGE.forEach { part ->
                                    val partInfo = positionOne.partInfos[part]
                                    td("right-align") {
                                        if (partInfo == null) {
                                            +"-"
                                        } else if (partInfo.codePubliclyVisible) {
                                            a(href = "/$year/day/$day?solution=${partInfo.solutionId}#solution") {
                                                +"${partInfo.tokens}"
                                            }
                                        } else if (positionOne.userId == session?.userId) {
                                            a(href = "/$year/day/$day?solution=${partInfo.solutionId}#solution") {
                                                +"${partInfo.tokens} (only accessible by you)"
                                            }
                                        } else {
                                            +"${partInfo.tokens}"
                                        }
                                    }
                                }

                                td("right-align mobile-hidden") {
                                    +positionOne.partInfos.values.maxOf { it.uploadDate }.relativeToNow
                                }
                            }
                        }
                    }
                }
            }

            p {
                +"Click on a day to submit your solution."
            }
        }
    }
}
