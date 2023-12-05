package golf.adventofcode.tokenizer

interface Tokenizer {
    data class Token(val sourcePosition: ClosedRange<Position>, val source: String, val type: Type) {
        enum class Type {
            CODE_TOKEN, // Name, operation, etc.
            STRING,
            WHITESPACE, // Space, tab, newline, BOM, etc.
            COMMENT
        }

        data class Position(val lineNumber: Int, val columnNumber: Int) : Comparable<Position> {
            override fun compareTo(other: Position): Int {
                return this.lineNumber * 100_000 + this.columnNumber - other.lineNumber * 100_000 - other.columnNumber
            }
        }
    }

    fun tokenize(input: String): List<Token>

    fun getTokenCount(input: String): Int {
        return tokenize(input).sumOf { token ->
            when (token.type) {
                Token.Type.CODE_TOKEN -> 1
                Token.Type.STRING -> token.source.length
                Token.Type.WHITESPACE -> 0
                Token.Type.COMMENT -> 0
            }
        }
    }
}