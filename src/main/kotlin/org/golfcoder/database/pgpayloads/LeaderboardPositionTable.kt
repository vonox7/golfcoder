package org.golfcoder.database.pgpayloads

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.golfcoder.database.Solution
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

fun ResultRow.toLeaderboardPosition(): LeaderboardPosition {
    return LeaderboardPosition().apply {
        year = this@toLeaderboardPosition[LeaderboardPositionTable.year]
        day = this@toLeaderboardPosition[LeaderboardPositionTable.day]
        position = this@toLeaderboardPosition[LeaderboardPositionTable.position]
        userId = this@toLeaderboardPosition[LeaderboardPositionTable.userId]
        language = this@toLeaderboardPosition[LeaderboardPositionTable.language]
        tokenSum = this@toLeaderboardPosition[LeaderboardPositionTable.tokenSum]
        partInfos = this@toLeaderboardPosition[LeaderboardPositionTable.partInfos]
    }
}


class LeaderboardPosition {
    var year: Int = 0
    var day: Int = 0
    var position: Int = 0
    lateinit var userId: String
    lateinit var language: Solution.Language
    var tokenSum: Int = 0
    var partInfos: Map<Int, LeaderboardPositionTable.PartInfo> = emptyMap() // part to PartInfo
}