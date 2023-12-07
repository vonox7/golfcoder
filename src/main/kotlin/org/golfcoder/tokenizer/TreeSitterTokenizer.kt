package org.golfcoder.tokenizer

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.golfcoder.httpClient

abstract class TreeSitterTokenizer(private val language: String) : Tokenizer {

    @Serializable
    private class TreeSitterRequest(val language: String, val code: String)

    @Serializable
    private class TreeSitterResponse(val tokens: List<Token>) {
        @Serializable
        class Token(
            val startRow: Int,
            val startColumn: Int,
            val endRow: Int,
            val endColumn: Int,
            val type: String,
            val text: String,
        )
    }

    override suspend fun tokenize(input: String): List<Tokenizer.Token> {
        val response = httpClient.post("http://localhost:8031/tokenize") {
            contentType(ContentType.Application.Json)
            setBody(TreeSitterRequest(language, input))
        }.body<TreeSitterResponse>()

        return response.tokens.map { treeSitterToken ->
            Tokenizer.Token(
                Tokenizer.Token.Position(
                    treeSitterToken.startRow,
                    treeSitterToken.startColumn
                )..Tokenizer.Token.Position(treeSitterToken.endRow, treeSitterToken.endColumn),
                treeSitterToken.text, // Some whitespaces (and comments?) might be missing. We could combine them using start/end position in case we add a visual feedback for the user.
                when (treeSitterToken.type) {
                    "ERROR" -> throw Exception("Syntax error on line ${treeSitterToken.startRow}:${treeSitterToken.startColumn}:\n${treeSitterToken.text}")
                    "comment" -> Tokenizer.Token.Type.COMMENT
                    "string_content" -> Tokenizer.Token.Type.STRING
                    else -> Tokenizer.Token.Type.CODE_TOKEN
                }
            )
        }
    }
}

class JavascriptTokenizer : TreeSitterTokenizer("javascript")
class PythonTokenizer : TreeSitterTokenizer("python")