package org.golfcoder.database

import com.moshbit.katerbase.MongoMainEntry
import com.moshbit.katerbase.MongoSubEntry
import org.golfcoder.coderunner.Coderunner
import org.golfcoder.coderunner.KotlinPlaygroundCoderunner
import org.golfcoder.coderunner.OnecompilerCoderunner
import org.golfcoder.expectedoutputaggregator.*
import org.golfcoder.tokenizer.Tokenizer
import org.golfcoder.tokenizer.TreeSitterTokenizer
import java.util.*

class User : MongoMainEntry() {
    // _id is the userId
    var oAuthDetails: List<OAuthDetails> = emptyList()
    var createdOn: Date = Date()
    var name: String = ""
    var publicProfilePictureUrl: String? = null
    var nameIsPublic: Boolean = true
    var profilePictureIsPublic: Boolean = true
    var defaultLanguage: Solution.Language? = null
    var tokenizedCodeCount: Int = 0 // Needed to show "highscores" in the future?
    var codeRunCount: Int = 0 // Needed to show "highscores" in the future?
    var adventOfCodeRepositoryInfo: AdventOfCodeRepositoryInfo? = null

    // Admins can see all solutions to detect cheaters and can delete solutions. Must be set manually in the database.
    var admin: Boolean = false

    class OAuthDetails(
        val provider: String,
        val providerUserId: String,
        val createdOn: Date,
    ) : MongoSubEntry()

    // Either singleAocRepositoryUrl, yearAocRepositoryUrl or none will be set
    class AdventOfCodeRepositoryInfo(
        val githubProfileName: String,
        var singleAocRepositoryUrl: String? = null,
        var yearAocRepositoryUrl: Map<String, String> = emptyMap(), // year-string to repository-Url
        var publiclyVisible: Boolean = true,
    ) : MongoSubEntry()

    fun getAdventOfCodeRepositoryUrl(year: Int): String? {
        return (adventOfCodeRepositoryInfo?.singleAocRepositoryUrl
            ?: adventOfCodeRepositoryInfo?.yearAocRepositoryUrl?.get(year.toString()))
            ?.takeIf { adventOfCodeRepositoryInfo?.publiclyVisible == true }
    }
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
    var tokenCount: Int = 0
    var tokenizerVersion: Int = 0
    var markedAsCheated: Boolean = false

    // Most used languages on GitHub for advent-of-code: https://github.com/search?q=advent-of-code+2023&type=repositories

    enum class Language(
        val displayName: String,
        val fileEnding: String,
    ) {
        PYTHON("Python", "py"),
        RUST("Rust", "rs"),
        GO("Go", "go"),
        KOTLIN("Kotlin", "kt"),
        JAVASCRIPT("JavaScript", "js"),
        CSHARP("C#", "cs"),
        TYPESCRIPT("TypeScript", "ts"),
        CPLUSPLUS("C++", "cpp"),
        JAVA("Java", "java"),
        C("C", "c"),
        SWIFT("Swift", "swift"),
        SCALA("Scala", "scala"),
        RUBY("Ruby", "rb"),
        BASH("Bash", "sh"),
        ;

        val coderunner: Coderunner
            get() = when (this) {
                PYTHON -> OnecompilerCoderunner("python")
                RUST -> OnecompilerCoderunner("rust")
                GO -> OnecompilerCoderunner("go")
                KOTLIN -> KotlinPlaygroundCoderunner() // Onecompiler supports as of now only Kotlin version 1.3
                JAVASCRIPT -> OnecompilerCoderunner("javascript")
                CSHARP -> OnecompilerCoderunner("csharp")
                TYPESCRIPT -> OnecompilerCoderunner("typescript")
                CPLUSPLUS -> OnecompilerCoderunner("cpp")
                JAVA -> OnecompilerCoderunner("java")
                C -> OnecompilerCoderunner("c")
                SWIFT -> OnecompilerCoderunner("swift")
                SCALA -> OnecompilerCoderunner("scala")
                RUBY -> OnecompilerCoderunner("ruby")
                BASH -> OnecompilerCoderunner("bash")
            }


        val tokenizer: Tokenizer
            get() = when (this) {
                PYTHON -> TreeSitterTokenizer("python", tokenizerVersion = 2)
                RUST -> TreeSitterTokenizer("rust", tokenizerVersion = 2)
                GO -> TreeSitterTokenizer("go", tokenizerVersion = 2)
                KOTLIN -> TreeSitterTokenizer("kotlin", tokenizerVersion = 4)
                JAVASCRIPT -> TreeSitterTokenizer("javascript", tokenizerVersion = 2)
                CSHARP -> TreeSitterTokenizer("csharp", tokenizerVersion = 2)
                TYPESCRIPT -> TreeSitterTokenizer("typescript", tokenizerVersion = 2)
                CPLUSPLUS -> TreeSitterTokenizer("cpp", tokenizerVersion = 4)
                JAVA -> TreeSitterTokenizer("java", tokenizerVersion = 2)
                C -> TreeSitterTokenizer("c", tokenizerVersion = 3)
                SWIFT -> TreeSitterTokenizer("swift", tokenizerVersion = 2)
                SCALA -> TreeSitterTokenizer("scala", tokenizerVersion = 2)
                RUBY -> TreeSitterTokenizer("ruby", tokenizerVersion = 2)
                BASH -> TreeSitterTokenizer("bash", tokenizerVersion = 2)
            }
    }
}

class LeaderboardPosition : MongoMainEntry() {
    var year: Int = 0
    var day: Int = 0
    var position: Int = 0
    lateinit var userId: String
    lateinit var language: Solution.Language
    var tokenSum: Int = 0
    var partInfos: Map<Int, PartInfo> = emptyMap() // part to PartInfo

    class PartInfo(
        val tokens: Int,
        val solutionId: String,
        val codePubliclyVisible: Boolean,
        val uploadDate: Date,
    ) : MongoSubEntry()
}

class ExpectedOutput : MongoMainEntry() {
    var year: Int = 0
    var day: Int = 0
    var part: Int = 0
    var input: String = ""
    var output: String = ""
    lateinit var source: Source

    enum class Source {
        FORNWALL,
        FORNWALL_RUST,
        GEREONS,
        SEVEN_REBUX,
        KATE,
        SHAHATA,
        ;

        val aggregator: ExpectedOutputAggregator
            get() = when (this) {
                FORNWALL -> FornwallAggregator()
                FORNWALL_RUST -> FornwallRustAggregator()
                GEREONS -> GereonsAggregator()
                SEVEN_REBUX -> SevenRebuxAggregator()
                KATE -> KateAggregator()
                SHAHATA -> ShahataAggregator()
            }
    }
}