package org.golfcoder.database.pgpayloads

import com.moshbit.katerbase.equal
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.golfcoder.database.Solution
import org.golfcoder.database.User
import org.golfcoder.endpoints.api.EditUserApi.MAX_USER_NAME_LENGTH
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.golfcoder.utils.toJavaDate
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
        fun getAdventOfCodeRepositoryUrl(year: Int): String? {
            return (singleAocRepositoryUrl ?: yearAocRepositoryUrl[year.toString()])?.takeIf { publiclyVisible }
        }
    }
}

fun ResultRow.toUser() = User().apply {
    _id = this@toUser[UserTable.id]
    oAuthDetails = this@toUser[UserTable.oauthDetails].map {
        User.OAuthDetails(
            provider = it.provider,
            providerUserId = it.providerUserId,
            createdOn = java.util.Date(
                it.createdOn.toJavaLocalDateTime().toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
            )
        )
    }
    createdOn = this@toUser[UserTable.createdOn].toJavaDate()
    name = this@toUser[UserTable.name]
    publicProfilePictureUrl = this@toUser[UserTable.publicProfilePictureUrl]
    nameIsPublic = this@toUser[UserTable.nameIsPublic]
    profilePictureIsPublic = this@toUser[UserTable.profilePictureIsPublic]
    defaultLanguage = this@toUser[UserTable.defaultLanguage]
    tokenizedCodeCount = this@toUser[UserTable.tokenizedCodeCount]
    codeRunCount = this@toUser[UserTable.codeRunCount]
    adventOfCodeRepositoryInfo = this@toUser[UserTable.adventOfCodeRepositoryInfo]?.let {
        User.AdventOfCodeRepositoryInfo(
            githubProfileName = it.githubProfileName,
            singleAocRepositoryUrl = it.singleAocRepositoryUrl,
            yearAocRepositoryUrl = it.yearAocRepositoryUrl,
            publiclyVisible = it.publiclyVisible
        )
    }
    admin = this@toUser[UserTable.admin]

}

suspend fun getUser(userId: String): User? {
    return UserTable.selectAll().where(UserTable.id eq userId).map { it.toUser() }.firstOrNull()
        ?: mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal userId)
}

suspend fun UserSession.getUser(): User {
    return getUser(this.userId)!!
}