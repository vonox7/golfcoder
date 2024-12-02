package org.golfcoder.tokenizer

import com.moshbit.katerbase.equal
import com.moshbit.katerbase.lower
import io.sentry.Sentry
import org.golfcoder.database.Solution
import org.golfcoder.endpoints.api.UploadSolutionApi
import org.golfcoder.mainDatabase

object TokenRecalculator {
    suspend fun recalculateSolutions() {
        println("Recalculate solutions...")
        var recalculationCount = 0

        Solution.Language.entries.forEach { language ->
            val tokenizer = language.tokenizer
            if (tokenizer is NotYetAvailableTokenizer) {
                return@forEach // Ignore languages that are not yet supported
            }

            mainDatabase.getSuspendingCollection<Solution>()
                .find(
                    Solution::language equal language.name,
                    Solution::tokenizerVersion lower tokenizer.tokenizerVersion
                )
                .collect { solution ->
                    try {
                        val tokenCount = tokenizer.getTokenCount(tokenizer.tokenize(solution.code))
                        mainDatabase.getCollection<Solution>().updateOne(Solution::_id equal solution._id) {
                            Solution::tokenCount setTo tokenCount
                            Solution::tokenizerVersion setTo tokenizer.tokenizerVersion
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
