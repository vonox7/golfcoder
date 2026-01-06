package org.golfcoder.database.pgpayloads

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.golfcoder.database.Solution
import org.golfcoder.endpoints.api.EditUserApi.MAX_USER_NAME_LENGTH
import org.golfcoder.plugins.UserSession
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.json.jsonb
import org.jetbrains.exposed.v1.r2dbc.selectAll

object UserTable : Table("user") {
    val id = varchar("id", 32)
    val oauthDetails = jsonb<Array<OAuthDetails>>("oauthDetails", Json)
    val createdOn = datetime("createdOn").defaultExpression(CurrentDateTime)
    val name = varchar("name", MAX_USER_NAME_LENGTH)
    val publicProfilePictureUrl = varchar("publicProfilePictureUrl", 2048).nullable()
    val nameIsPublic = bool("nameIsPublic").default(true)
    val profilePictureIsPublic = bool("profilePictureIsPublic").default(true)
    val defaultLanguage = enumerationByName<Solution.Language>("defaultLanguage", length = 20).nullable().default(null)
    val tokenizedCodeCount = integer("tokenizedCodeCount").default(0) // Needed to show "highscores" in the future?
    val codeRunCount = integer("codeRunCount").default(0) // Needed to show "highscores" in the future?
    val adventOfCodeRepositoryInfo = jsonb<AdventOfCodeRepositoryInfo>("adventOfCodeRepositoryInfo", Json).nullable()

    // Admins can see all solutions to detect cheaters and can delete solutions. Must be set manually in the database.
    val admin = bool("admin").default(false)

    override val primaryKey = PrimaryKey(id)

    @Serializable
    data class OAuthDetails(
        val provider: String,
        val providerUserId: String,
        val createdOn: LocalDateTime,
    )

    @Serializable
    data class AdventOfCodeRepositoryInfo(
        val githubProfileName: String,
        val singleAocRepositoryUrl: String? = null,
        val yearAocRepositoryUrl: Map<String, String> = emptyMap(), // year-string to repository-Url
        val publiclyVisible: Boolean = true,
    ) {
        fun getUrl(year: Int): String? {
            return (singleAocRepositoryUrl ?: yearAocRepositoryUrl[year.toString()])?.takeIf { publiclyVisible }
        }
    }
}

class User(
    val id: String,
    val oAuthDetails: List<UserTable.OAuthDetails>,
    val createdOn: LocalDateTime,
    val name: String,
    val publicProfilePictureUrl: String?,
    val nameIsPublic: Boolean,
    val profilePictureIsPublic: Boolean,
    val defaultLanguage: Solution.Language?,
    val adventOfCodeRepositoryInfo: UserTable.AdventOfCodeRepositoryInfo?,
    var admin: Boolean,
)

fun ResultRow.toUser() = User(
    id = this[UserTable.id],
    oAuthDetails = this[UserTable.oauthDetails].toList(),
    createdOn = this[UserTable.createdOn],
    name = this[UserTable.name],
    publicProfilePictureUrl = this[UserTable.publicProfilePictureUrl],
    nameIsPublic = this[UserTable.nameIsPublic],
    profilePictureIsPublic = this[UserTable.profilePictureIsPublic],
    defaultLanguage = this[UserTable.defaultLanguage],
    adventOfCodeRepositoryInfo = this[UserTable.adventOfCodeRepositoryInfo],
    admin = this[UserTable.admin],
)

suspend fun getUser(userId: String): User? {
    return UserTable.selectAll().where(UserTable.id eq userId).map { it.toUser() }.firstOrNull()
}

suspend fun UserSession.getUser(): User {
    return getUser(this.userId)!!
}