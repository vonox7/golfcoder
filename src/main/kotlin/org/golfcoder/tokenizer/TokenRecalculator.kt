package org.golfcoder.tokenizer

import io.sentry.Sentry
import org.golfcoder.database.Solution
import org.golfcoder.database.pgpayloads.SolutionTable
import org.golfcoder.endpoints.api.UploadSolutionApi
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update

object TokenRecalculator {
    suspend fun recalculateSolutions() = suspendTransaction {
        println("Recalculate solutions...")
        var recalculationCount = 0

        Solution.Language.entries.forEach { language ->
            val tokenizer = language.tokenizer
            if (tokenizer is NotYetAvailableTokenizer) {
                return@forEach // Ignore languages that are not yet supported
            }

            SolutionTable.selectAll()
                .andWhere { SolutionTable.language eq language }
                .andWhere { SolutionTable.tokenizerVersion less tokenizer.tokenizerVersion }
                .collect { solution ->
                    try {
                        val newTokenCount = tokenizer.getTokenCount(tokenizer.tokenize(solution[SolutionTable.code]))
                        SolutionTable.update({ SolutionTable.id eq solution[SolutionTable.id] }) {
                            it[tokenCount] = newTokenCount
                            it[tokenizerVersion] = tokenizer.tokenizerVersion
                        }
                        recalculationCount++
                    } catch (e: Exception) {
                        Sentry.captureException(e)
                        e.printStackTrace() // Tokenizer might fail, but continue with next solution
                    }
                }
        }
        if (recalculationCount > 0) {
            // Leaderboard might have changed
            UploadSolutionApi.recalculateAllScores()
        }
        println("All necessary $recalculationCount solutions recalculated")
    }
}
