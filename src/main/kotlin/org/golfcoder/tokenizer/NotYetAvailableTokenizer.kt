package org.golfcoder.tokenizer

class NotYetAvailableTokenizer : Tokenizer {
    override suspend fun tokenize(input: String): List<Tokenizer.Token> {
        throw Exception("Tokenizer not yet available")
    }
}