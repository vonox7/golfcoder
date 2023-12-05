package golf.adventofcode.tokenizer

class NotYetAvailableTokenizer : Tokenizer {
    override fun tokenize(input: String): List<Tokenizer.Token> {
        throw Exception("Tokenizer not yet available")
    }
}