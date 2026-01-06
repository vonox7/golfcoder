package org.golfcoder.coderunner

import org.golfcoder.database.pgpayloads.Solution


abstract class Coderunner(val stdinCharLimit: Int) {
    // Returns stdout, or an exception with a user-friendly message
    abstract suspend fun run(code: String, language: Solution.Language, stdin: String): RunResult

    class RunResult(
        val stdout: String,
        val error: String?, // Contains compiler warnings and/or errors
    )
}