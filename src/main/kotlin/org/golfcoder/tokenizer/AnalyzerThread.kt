package org.golfcoder.tokenizer

import com.moshbit.katerbase.equal
import org.golfcoder.database.Solution
import org.golfcoder.mainDatabase
import java.util.*
import kotlin.concurrent.thread
import kotlin.reflect.full.primaryConstructor

val analyzerThread = thread(start = false, isDaemon = true, name = "analyzer-worker") {
    while (true) {
        try {
            mainDatabase.getCollection<Solution>()
                .find(Solution::tokenCountAnalyzeDate equal null)
                .forEach { solution ->
                    if (solution.language.tokenizerClass == NotYetAvailableTokenizer::class) {
                        return@forEach // Ignore languages that are not yet supported
                    }
                    val tokenizer = solution.language.tokenizerClass.primaryConstructor!!.call()
                    val tokenCount = try {
                        tokenizer.getTokenCount(solution.code)
                    } catch (e: Exception) {
                        e.printStackTrace() // Tokenizer might fail
                        null
                    }
                    mainDatabase.getCollection<Solution>().updateOne(Solution::_id equal solution._id) {
                        Solution::tokenCount setTo tokenCount
                        Solution::tokenCountAnalyzeDate setTo Date()
                    }
                }

            // Regularly check for new solutions to analyze.
            // However, we should always get interrupted when there is a new upload (except on race conditions).
            try {
                Thread.sleep(60_000)
            } catch (_: InterruptedException) {
                // We might interrupt the sleep to immediately trigger a new analysis after an upload
            }
        } catch (e: Exception) {
            e.printStackTrace() // Print stack, but continue
        }
    }
}