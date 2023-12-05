package golf.adventofcode.tokens

import io.ktor.util.*
import java.io.File
import kotlin.random.Random

class PythonTokenizer : Tokenizer {
    // Creates a file with the given input,
    // runs the python tokenizer iva `python -m tokenize randomFilename.py`,
    // and then deletes the temporary file.
    // Not the most elegant solution, but it works for now.
    override fun tokenize(input: String): List<Tokenizer.Token> {
        val filename = "tokenizer-tmp/python/" +
                Random.Default.nextBytes(32)
                    .encodeBase64()
                    .replace("/", "_") // Make sure we don't end up with bad filenames
                    .replace("+", "-")
                    .replace("=", "") +
                ".py"
        val file = File(filename)

        try {
            file.parentFile.mkdirs()
            file.writeText(input)

            val tokenString = arrayOf("python3", "-m", "tokenize", filename)
                .runCommand(waitMilliSeconds = 1000, printOutput = true)

            val lineRegex = Regex("""^([0-9]+),([0-9]+)-([0-9]+),([0-9]+):\s*([A-Z]+)\s*(.+?)\s*$""")
            var endMarkerFound = false

            val tokens = tokenString
                .split("\n")
                .filter { it.isNotEmpty() }
                .map { line ->
                    val match = lineRegex.matchEntire(line) ?: throw Exception("Could not match lineRegex: $line")
                    val (startLine, startColumn, endLine, endColumn, type, value) = match.destructured
                    Tokenizer.Token(
                        sourcePosition = Tokenizer.Token.Position(startLine.toInt(), startColumn.toInt())..
                                Tokenizer.Token.Position(endLine.toInt(), endColumn.toInt()),
                        source = value.removeSurrounding("'").removeSurrounding("\""),
                        type = when (type) {
                            "NAME" -> Tokenizer.Token.Type.CODE_TOKEN
                            "OP" -> Tokenizer.Token.Type.CODE_TOKEN
                            "STRING" -> Tokenizer.Token.Type.STRING
                            "ENCODING" -> Tokenizer.Token.Type.WHITESPACE
                            "NEWLINE" -> Tokenizer.Token.Type.WHITESPACE
                            "INDENT" -> Tokenizer.Token.Type.WHITESPACE
                            "DEDENT" -> Tokenizer.Token.Type.WHITESPACE
                            "ENDMARKER" -> {
                                endMarkerFound = true
                                Tokenizer.Token.Type.WHITESPACE
                            }

                            "NL" -> Tokenizer.Token.Type.WHITESPACE
                            "COMMENT" -> Tokenizer.Token.Type.COMMENT
                            else -> throw Exception("Unknown type $type in line: $line")
                        },
                    )
                }

            // Ensure that we didn't kill the python process before it finished parsing, and we had no parsing exceptions
            require(endMarkerFound)

            return tokens
        } finally {
            file.delete()
        }
    }

}