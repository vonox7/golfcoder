package org.golfcoder.coderunner

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.golfcoder.database.Solution
import org.golfcoder.httpClient

class KotlinPlaygroundCoderunner : Coderunner {

    @Serializable
    private class KotlinPlaygroundRunRequest(
        val args: String,
        val files: List<File>,
        val confType: String,
    ) {
        @Serializable
        class File(
            val name: String,
            val text: String,
            val publicId: String,
        )
    }

    @Serializable
    private class KotlinPlaygroundRunResponse(
        val exception: String?,
        val text: String,
        val errors: Map<String, List<Error>>, // Filename to list of errors
    ) {
        @Serializable
        class Error(
            val interval: Interval,
            val message: String,
            val severity: String,
            val className: String,
        ) {
            @Serializable
            class Interval(
                val start: Position,
                val end: Position,
            ) {
                @Serializable
                class Position(
                    val line: Int,
                    val ch: Int,
                )
            }
        }
    }


    override suspend fun run(code: String, language: Solution.Language, stdin: String): Coderunner.RunResult {
        require(language == Solution.Language.KOTLIN)
        val stdInAsListString = stdin.split("\n").joinToString(",") { "\"\"\"$it\"\"\"" }

        val response = httpClient.post("https://api.kotlinlang.org//api/1.9.20/compiler/run") {
            contentType(ContentType.Application.Json)
            setBody(
                KotlinPlaygroundRunRequest(
                    args = "",
                    files = listOf(
                        KotlinPlaygroundRunRequest.File(
                            name = "File.kt",
                            // Inject stdin into readLine() function, as Kotlin Playground doesn't have a stdin.
                            // See https://youtrack.jetbrains.com/issue/KT-46705/Kotlin-Playground-Add-support-for-program-input
                            text = "$code\n\n" +
                                    "val golfcoderStdInValueThatYouMightNotAccessDirectlyButJustUseReadLine = mutableListOf($stdInAsListString); " +
                                    "fun readLine(): String? = golfcoderStdInValueThatYouMightNotAccessDirectlyButJustUseReadLine.removeFirstOrNull();",
                            publicId = "",
                        )
                    ),
                    confType = "java",
                )
            )
        }.body<KotlinPlaygroundRunResponse>()

        mutableListOf("").removeAt(0)

        val playgroundErrorString = response.exception?.takeIf { it.isNotEmpty() }
            ?: response.errors.values.flatten().takeIf { it.isNotEmpty() }?.joinToString("\n") { error ->
                "${error.message} at line ${error.interval.start.line + 1}:${error.interval.start.ch + 1}"
            }

        val resultString = response.text
            .substringAfter("<outStream>", missingDelimiterValue = "")
            .substringBeforeLast("</outStream>", missingDelimiterValue = "")

        return Coderunner.RunResult(
            stdout = resultString,
            error = playgroundErrorString,
        )
    }
}