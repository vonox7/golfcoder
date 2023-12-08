package org.golfcoder.database

import com.moshbit.katerbase.MongoMainEntry
import com.moshbit.katerbase.MongoSubEntry
import org.golfcoder.tokenizer.*
import java.util.*
import kotlin.reflect.KClass

class User : MongoMainEntry() {
    // _id is the userId
    var oAuthDetails: List<OAuthDetails> = emptyList()
    var createdOn: Date = Date()
    var name: String = ""
    var publicProfilePictureUrl: String? = null
    var nameIsPublic: Boolean = true
    var profilePictureIsPublic: Boolean = true

    class OAuthDetails(
        val provider: String,
        val providerUserId: String,
        val createdOn: Date,
    ) : MongoSubEntry()
}

class Solution : MongoMainEntry() {
    lateinit var userId: String
    lateinit var uploadDate: Date
    lateinit var code: String
    lateinit var language: Language
    var year: Int = 0
    var day: Int = 0
    var part: Int = 0
    var codePubliclyVisible: Boolean = false
    var externalLinks: List<String> = emptyList() // e.g. Github, Blog explaining stuff...
    var tokenCount: Int = 0
    var tokenizerVersion: Int = 0

    // Most used languages on GitHub for advent-of-code: https://github.com/search?q=advent-of-code+2023&type=repositories

    enum class Language(
        val displayName: String,
        val fileEnding: String,
        val onecompilerLanguageId: String, // Find out by checking the URL after selecting a language at the dropdown at https://onecompiler.com/
        val tokenizerClass: KClass<out Tokenizer>,
        val tokenizerVersion: Int, // Increment this when the tokenizer changes to force re-analysis of all solutions
    ) {
        PYTHON("Python", "py", "python", PythonTokenizer::class, 1),
        RUST("Rust", "rs", "rust", NotYetAvailableTokenizer::class, 1),
        GO("Go", "go", "go", NotYetAvailableTokenizer::class, 1),
        KOTLIN("Kotlin", "kt", "kotlin", KotlinTokenizer::class, 1),
        JAVASCRIPT("JavaScript", "js", "javascript", JavascriptTokenizer::class, 1),
        CSHARP("C#", "cs", "csharp", NotYetAvailableTokenizer::class, 1),
        TYPESCRIPT("TypeScript", "ts", "typescript", NotYetAvailableTokenizer::class, 1),
        CPLUSPLUS("C++", "cpp", "cpp", NotYetAvailableTokenizer::class, 1),
        JAVA("Java", "java", "java", NotYetAvailableTokenizer::class, 1),
        C("C", "c", "c", NotYetAvailableTokenizer::class, 1),
    }
}

class ExpectedOutput : MongoMainEntry() {
    var year: Int = 0
    var day: Int = 0
    var part: Int = 0
    var input: String = ""
    var output: Long = 0L
    lateinit var source: Source

    enum class Source {
        FORNWALL // Fornwall seems to be quite fast to solve problems, and has a way to get input+output. Maybe add more sources later to be faster.
    }
}