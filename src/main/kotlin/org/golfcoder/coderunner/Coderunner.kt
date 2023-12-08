package org.golfcoder.coderunner

import org.golfcoder.database.Solution

interface Coderunner {
    // Returns stdout, or an exception with a user-friendly message
    suspend fun run(code: String, language: Solution.Language, stdin: String): Result<String>
}