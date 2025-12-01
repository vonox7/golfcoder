package org.golfcoder.coderunner

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.golfcoder.database.Solution
import org.golfcoder.httpClient
import org.golfcoder.utils.bodyOrPrintException

// Find out onecompilerLanguageId by checking the URL after selecting a language at the dropdown at https://onecompiler.com/
// Char limit see https://onecompiler.com/apis/code-execution
class OnecompilerCoderunner(private val onecompilerLanguageId: String) : Coderunner(stdinCharLimit = 10_000) {

    @Serializable
    private class OnecompilerRequest(
        val language: String,
        val stdin: String,
        val files: List<File>,
    ) {
        @Serializable
        class File(
            val name: String,
            val content: String,
        )
    }

    @Serializable
    private class OnecompilerResponse(
        val status: String, // Is even then "success" when there is an exception
        val exception: String? = null, // Populated by Python compiler, but not by Kotlin compiler
        val stdout: String?,
        val stderr: String?,
        val executionTime: Int,
        val limitPerMonthRemaining: Int? = null,
        val stdin: String,
    )

    override suspend fun run(code: String, language: Solution.Language, stdin: String): Coderunner.RunResult {
        val onecompilerResponse = httpClient.post("https://onecompiler-apis.p.rapidapi.com/api/v1/run") {
            contentType(ContentType.Application.Json)
            header("X-RapidAPI-Key", System.getenv("RAPIDAPI_KEY"))
            header("X-RapidAPI-Host", "onecompiler-apis.p.rapidapi.com")
            setBody(
                OnecompilerRequest(
                    language = onecompilerLanguageId,
                    stdin = stdin,
                    files = listOf(
                        OnecompilerRequest.File(
                            name = "index.${language.fileEnding}",
                            content = code
                        )
                    )
                )
            )
        }.bodyOrPrintException<OnecompilerResponse>()

        var onecompilerException = (onecompilerResponse.stderr?.takeIf { it.isNotEmpty() }
            ?: onecompilerResponse.exception?.takeIf { it.isNotEmpty() })
            // Sanitize output from OneCompiler (Kotlin)
            ?.removePrefix("OpenJDK 64-Bit Server VM warning: Options -Xverify:none and -noverify were deprecated in JDK 13 and will likely be removed in a future release.\n")

        var stdout = onecompilerResponse.stdout
        if (onecompilerException == null && language == Solution.Language.BASH && onecompilerResponse.stdout?.trim() == "0") {
            onecompilerException = "Wrong stdout: 0\n\n" +
                "Note that you can only use bash commands, but no unix utilities / BusyBox.\n" +
                "So no awk, grep, sed, cat or similar.\n" +
                "Call `man bash` for further help."
            stdout = null
        }

        println(
            "Onecompiler limit remaining: ${onecompilerResponse.limitPerMonthRemaining}. " +
                    "Execution time: ${onecompilerResponse.executionTime}ms for " +
                    "${code.length} characters in $language" +
                    if (onecompilerException == null) "" else " (execution failed)"
        )

        return Coderunner.RunResult(stdout = stdout ?: "", error = onecompilerException)
    }
}