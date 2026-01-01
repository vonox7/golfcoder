package org.golfcoder.database.pgpayloads

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.golfcoder.database.Solution
import org.golfcoder.endpoints.api.EditUserApi.MAX_USER_NAME_LENGTH
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.json.jsonb

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
        val createdOn: String, // ISO date string
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