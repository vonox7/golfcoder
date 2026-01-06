package org.golfcoder.database.pgpayloads

import org.golfcoder.database.Solution
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object SolutionTable : Table("solution") {
    val id = varchar("id", length = 32)
    val userId = varchar("userId", length = 32) // TODO change to ref
    val uploadDate = datetime("uploadDate").defaultExpression(CurrentDateTime)
    val code = text("code")
    val language = enumerationByName("language", length = 20, Solution.Language::class)
    val year = integer("year")
    val day = integer("day")
    val part = integer("part")
    val codePublicVisible = bool("codePublicVisible")
    val tokenCount = integer("tokenCount")
    val tokenizerVersion = integer("tokenizerVersion")
    val markedAsCheated = bool("markedAsCheated").default(false)

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, year, day, part, markedAsCheated, tokenCount)
        index(isUnique = false, language, tokenizerVersion)
        index(isUnique = false, userId, uploadDate)
    }
}

fun ResultRow.toSolution(): Solution {
    return Solution().apply {
        id = this@toSolution[SolutionTable.id]
        userId = this@toSolution[SolutionTable.userId]
        uploadDate = this@toSolution[SolutionTable.uploadDate]
        code = this@toSolution[SolutionTable.code]
        language = this@toSolution[SolutionTable.language]
        year = this@toSolution[SolutionTable.year]
        day = this@toSolution[SolutionTable.day]
        part = this@toSolution[SolutionTable.part]
        codePubliclyVisible = this@toSolution[SolutionTable.codePublicVisible]
        tokenCount = this@toSolution[SolutionTable.tokenCount]
        tokenizerVersion = this@toSolution[SolutionTable.tokenizerVersion]
        markedAsCheated = this@toSolution[SolutionTable.markedAsCheated]
    }
}