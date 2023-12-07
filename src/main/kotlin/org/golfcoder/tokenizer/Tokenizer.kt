package org.golfcoder.tokenizer

interface Tokenizer {
    data class Token(val sourcePosition: ClosedRange<Position>, val source: String, val type: Type) {
        enum class Type {
            CODE_TOKEN, // Name, operation, etc.
            STRING, // String start/end indicators (e.g. '"') must be either included listed as CODE_TOKEN or be included in STRING
            WHITESPACE, // Space, tab, newline, BOM, etc.
            COMMENT
        }

        data class Position(val lineNumber: Int, val columnNumber: Int) : Comparable<Position> {
            override fun compareTo(other: Position): Int {
                return this.lineNumber * 100_000 + this.columnNumber - other.lineNumber * 100_000 - other.columnNumber
            }
        }
    }

    suspend fun tokenize(input: String): List<Token>

    suspend fun getTokenCount(tokens: List<Token>): Int {
        return tokens.sumOf { token ->
            when (token.type) {
                Token.Type.CODE_TOKEN -> 1
                Token.Type.STRING -> token.source.length
                Token.Type.WHITESPACE -> 0
                Token.Type.COMMENT -> 0
            }
        }
    }
}