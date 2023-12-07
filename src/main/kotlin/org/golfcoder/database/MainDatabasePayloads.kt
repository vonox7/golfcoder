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
    var tokenCount: Int? = null
    var tokenCountAnalyzeDate: Date? = null

    // Most used languages on GitHub for advent-of-code: https://github.com/search?q=advent-of-code+2023&type=repositories

    enum class Language(
        val displayName: String,
        val fileEnding: String,
        val onecompilerLanguageId: String, // Find out by checking the URL after selecting a language at the dropdown at https://onecompiler.com/
        val tokenizerClass: KClass<out Tokenizer>,
    ) {
        PYTHON("Python", "py", "python", PythonTokenizer::class),
        RUST("Rust", "rs", "rust", NotYetAvailableTokenizer::class),
        GO("Go", "go", "go", NotYetAvailableTokenizer::class),
        KOTLIN("Kotlin", "kt", "kotlin", KotlinTokenizer::class),
        JAVASCRIPT("JavaScript", "js", "javascript", JavascriptTokenizer::class),
        CSHARP("C#", "cs", "csharp", NotYetAvailableTokenizer::class),
        TYPESCRIPT("TypeScript", "ts", "typescript", NotYetAvailableTokenizer::class),
        CPLUSPLUS("C++", "cpp", "cpp", NotYetAvailableTokenizer::class),
        JAVA("Java", "java", "java", NotYetAvailableTokenizer::class),
        C("C", "c", "c", NotYetAvailableTokenizer::class),
    }
}