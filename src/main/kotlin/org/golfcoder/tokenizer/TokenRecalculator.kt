package org.golfcoder.tokenizer

import com.moshbit.katerbase.equal
import com.moshbit.katerbase.lower
import org.golfcoder.database.Solution
import org.golfcoder.mainDatabase
import kotlin.reflect.full.primaryConstructor

object TokenRecalculator {
    suspend fun recalculateSolutions() {
        println("Recalculate solutions...")
        var recalculationCount = 0

        Solution.Language.entries.forEach { language ->
            if (language.tokenizerClass == NotYetAvailableTokenizer::class) {
                return@forEach // Ignore languages that are not yet supported
            }

            mainDatabase.getSuspendingCollection<Solution>()
                .find(
                    Solution::language equal language.name,
                    Solution::tokenizerVersion lower language.tokenizerVersion
                )
                .collect { solution ->
                    val tokenizer = solution.language.tokenizerClass.primaryConstructor!!.call()
                    try {
                        val tokenCount = tokenizer.getTokenCount(tokenizer.tokenize(solution.code))
                        mainDatabase.getCollection<Solution>().updateOne(Solution::_id equal solution._id) {
                            Solution::tokenCount setTo tokenCount
                            Solution::tokenizerVersion setTo language.tokenizerVersion
                        }
                        recalculationCount++
                    } catch (e: Exception) {
                        e.printStackTrace() // Tokenizer might fail, but continue with next solution
                    }
                }
        }
        println("All necessary $recalculationCount solutions recalculated")
    }
}
