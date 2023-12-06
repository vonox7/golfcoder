package golf.adventofcode.database

import com.moshbit.katerbase.MongoMainEntry
import com.moshbit.katerbase.MongoSubEntry
import golf.adventofcode.tokenizer.NotYetAvailableTokenizer
import golf.adventofcode.tokenizer.PythonTokenizer
import golf.adventofcode.tokenizer.Tokenizer
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
    enum class Language(val displayName: String, val fileEnding: String, val tokenizerClass: KClass<out Tokenizer>) {
        PYTHON("Python", "py", PythonTokenizer::class),
        RUST("Rust", "rs", NotYetAvailableTokenizer::class),
        GO("Go", "go", NotYetAvailableTokenizer::class),
        KOTLIN("Kotlin", "kt", NotYetAvailableTokenizer::class),
        JAVASCRIPT("JavaScript", "js", NotYetAvailableTokenizer::class),
        CSHARP("C#", "cs", NotYetAvailableTokenizer::class),
        TYPESCRIPT("TypeScript", "ts", NotYetAvailableTokenizer::class),
        CPLUSPLUS("C++", "cpp", NotYetAvailableTokenizer::class),
        JAVA("Java", "java", NotYetAvailableTokenizer::class),
        C("C", "c", NotYetAvailableTokenizer::class),
    }
}