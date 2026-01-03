package org.golfcoder.endpoints.api

import com.moshbit.katerbase.equal
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.golfcoder.database.User
import org.golfcoder.database.pgpayloads.UserTable
import org.golfcoder.httpClient
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.golfcoder.utils.bodyOrPrintException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.time.Instant

object EditUserApi {
    const val MAX_USER_NAME_LENGTH = 50

    @Serializable
    private class EditUserRequest(
        val name: String = "",
        val nameIsPublic: String = "off",
        val profilePictureIsPublic: String = "off",
        val showAdventOfCodeRepositoryLink: String = "off",
    )

    @Serializable
    private class GithubReposResponse(
        val name: String, // e.g. "advent-of-code-2023"
        @SerialName("html_url") val htmlUrl: String, // e.g. "https://github.com/user/advent-of-code-2023"
        @SerialName("pushed_at") val pushedAtString: String, // e.g. "2023-12-05T10:52:25Z"
    ) {
        val pushedAt: Instant get() = Instant.parse(pushedAtString)
    }

    suspend fun post(call: ApplicationCall) = suspendTransaction {
        val request = call.receive<EditUserRequest>()
        val session = call.sessions.get<UserSession>()!!
        val currentUserAdventOfCodeRepositoryInfo =
            UserTable.select(UserTable.adventOfCodeRepositoryInfo)
                .where(UserTable.id eq session.userId)
                .map { it[UserTable.adventOfCodeRepositoryInfo] }
                .firstOrNull()
                ?: mainDatabase.getSuspendingCollection<User>()
                    .findOne(User::_id equal session.userId)?.adventOfCodeRepositoryInfo

        val newName = request.name.take(MAX_USER_NAME_LENGTH).trim().takeIf { it.isNotEmpty() } ?: "XXX"

        mainDatabase.getSuspendingCollection<User>()
            .updateOne(User::_id equal session.userId) {
                User::name setTo newName
                User::nameIsPublic setTo (request.nameIsPublic == "on")
                User::profilePictureIsPublic setTo (request.profilePictureIsPublic == "on")
                if (currentUserAdventOfCodeRepositoryInfo != null) {
                    User::adventOfCodeRepositoryInfo.child(User.AdventOfCodeRepositoryInfo::publiclyVisible) setTo (request.showAdventOfCodeRepositoryLink == "on")
                }
            }

        call.sessions.set(UserSession(session.userId, newName))

        call.respond(ApiCallResult(buttonText = "Saved", reloadSite = true))
    }

    // Find advent-of-code github repository/repositories: Auto detect aoc-repo-urls (either 1 or 1 per year)
    suspend fun getAdventOfCodeRepositoryInfo(
        profileName: String,
        principal: OAuthAccessTokenResponse.OAuth2,
    ): User.AdventOfCodeRepositoryInfo {
        val repos = httpClient
            .get("https://api.github.com/users/${profileName}/repos") {
                header(HttpHeaders.Authorization, "Bearer ${principal.accessToken}")
            }
            .bodyOrPrintException<List<GithubReposResponse>>()

        // Find all repos with "advent-of-code", but if none are found then find repos with "aoc"
        val adventOfCodeRepos = repos
            .filter { repo -> "adventofcode" in repo.name.lowercase().filter { it.isLetter() } }
            .takeIf { it.isNotEmpty() }
            ?: repos
                .filter { repo -> "aoc" in repo.name.lowercase().filter { it.isLetter() } }
                .takeIf { it.isNotEmpty() }

        // Users might have either 1 advent-of-code repo, or 1 per year
        val yearRegex = Regex("20[0-9]{1,2}")
        return when {
            adventOfCodeRepos.isNullOrEmpty() -> {
                User.AdventOfCodeRepositoryInfo(githubProfileName = profileName)
            }

            adventOfCodeRepos.count() == 1 || adventOfCodeRepos.none { it.name.contains(yearRegex) } -> {
                User.AdventOfCodeRepositoryInfo(
                    githubProfileName = profileName,
                    singleAocRepositoryUrl = adventOfCodeRepos.maxBy { it.pushedAt }.htmlUrl
                )
            }

            else -> {
                val yearAocRepositoryUrl = mutableMapOf<String, String>()
                (2015..2099).forEach { year ->
                    val yearRepo = adventOfCodeRepos.filter { year.toString() in it.name }.maxByOrNull { it.pushedAt }
                    if (yearRepo != null) {
                        yearAocRepositoryUrl[year.toString()] = yearRepo.htmlUrl
                    }
                }
                User.AdventOfCodeRepositoryInfo(
                    githubProfileName = profileName,
                    yearAocRepositoryUrl = yearAocRepositoryUrl
                )
            }
        }
    }
}