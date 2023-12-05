package golf.adventofcode.database

import com.moshbit.katerbase.MongoMainEntry
import golf.adventofcode.tokenizer.NotYetAvailableTokenizer
import golf.adventofcode.tokenizer.PythonTokenizer
import golf.adventofcode.tokenizer.Tokenizer
import java.util.*
import kotlin.reflect.KClass

class User(
    // _id is the userId
    // TODO oauthStuff
    val createdOn: Date,
    val publicName: String
) : MongoMainEntry()

class Solution(
    val userId: String,
    val uploadDate: Date,
    val code: String,
    val language: Language,
    val year: Int,
    val day: Int,
    val part: Int,
    val codePubliclyVisible: Boolean,
    val externalLinks: List<String>, // e.g. Github, Blog explaining stuff...
    val tokenCount: Int? = null,
    val tokenCountAnalyzeDate: Date? = null
) : MongoMainEntry() {
    // Most used languages on GitHub for advent-of-code: https://github.com/search?q=advent-of-code+2023&type=repositories
    enum class Language(val fileEnding: String, val tokenizerClass: KClass<out Tokenizer>) {
        PYTHON("py", PythonTokenizer::class),
        RUST("rs", NotYetAvailableTokenizer::class),
        GO("go", NotYetAvailableTokenizer::class),
        KOTLIN("kt", NotYetAvailableTokenizer::class),
        JAVASCRIPT("js", NotYetAvailableTokenizer::class),
        CSHARP("cs", NotYetAvailableTokenizer::class),
        TYPESCRIPT("ts", NotYetAvailableTokenizer::class),
        CPLUSPLUS("cpp", NotYetAvailableTokenizer::class),
        JAVA("java", NotYetAvailableTokenizer::class),
        C("c", NotYetAvailableTokenizer::class),
    }
}