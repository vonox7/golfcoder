package org.golfcoder.database.pgpayloads

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.json.jsonb


object LeaderboardPositionTable : Table("leaderboard_position") {
    val year = integer("year")
    val day = integer("day")
    val position = integer("position")
    val userId = varchar("userId", length = 32)
    val language = enumerationByName("language", length = 20, Solution.Language::class)
    val tokenSum = integer("tokenSum")
    val partInfos = jsonb<Map<Int, PartInfo>>("partInfos", Json)

    override val primaryKey = PrimaryKey(year, day, userId, language)

    init {
        index(isUnique = false, userId)
        index(isUnique = false, year, day, tokenSum)
        index(isUnique = false, columns = arrayOf(year, position), filterCondition = { day eq 1 })
    }

    @Serializable
    class PartInfo(
        val tokens: Int,
        val solutionId: String,
        val codePubliclyVisible: Boolean,
        val uploadDate: LocalDateTime,
    )
}

class LeaderboardPosition(
    val year: Int,
    val day: Int,
    val position: Int,
    val userId: String,
    val language: Solution.Language,
    val tokenSum: Int,
    val partInfos: Map<Int, LeaderboardPositionTable.PartInfo>, // part to PartInfo
)

fun ResultRow.toLeaderboardPosition(): LeaderboardPosition {
    return LeaderboardPosition(
        year = this[LeaderboardPositionTable.year],
        day = this[LeaderboardPositionTable.day],
        position = this[LeaderboardPositionTable.position],
        userId = this[LeaderboardPositionTable.userId],
        language = this[LeaderboardPositionTable.language],
        tokenSum = this[LeaderboardPositionTable.tokenSum],
        partInfos = this[LeaderboardPositionTable.partInfos],
    )
}
