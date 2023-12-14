package org.golfcoder.database

import com.moshbit.katerbase.MongoMainEntry
import com.moshbit.katerbase.MongoSubEntry
import org.golfcoder.coderunner.Coderunner
import org.golfcoder.coderunner.KotlinPlaygroundCoderunner
import org.golfcoder.coderunner.OnecompilerCoderunner
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator
import org.golfcoder.expectedoutputaggregator.FornwallAggregator
import org.golfcoder.expectedoutputaggregator.GereonsAggregator
import org.golfcoder.expectedoutputaggregator.SevenRebuxAggregator
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

    class OAuthDetails(
        val provider: String,
        val providerUserId: String,
        val createdOn: Date,
    ) : MongoSubEntry()

    // Either singleAocRepositoryUrl, yearAocRepositoryUrl or none will be set
    class AdventOfCodeRepositoryInfo(
        var singleAocRepositoryUrl: String? = null,
        var yearAocRepositoryUrl: Map<String, String> = emptyMap(), // year-string to repository-Url
    ) : MongoSubEntry()

    fun getAdventOfCodeRepositoryUrl(year: Int): String? {
        return adventOfCodeRepositoryInfo?.singleAocRepositoryUrl
            ?: adventOfCodeRepositoryInfo?.yearAocRepositoryUrl?.get(year.toString())
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
    var externalLinks: List<String> = emptyList() // e.g. Github, Blog explaining stuff...
    var tokenCount: Int = 0
    var tokenizerVersion: Int = 0

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
        C("C", "c");

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
            }


        val tokenizer: Tokenizer
            get() = when (this) {
                PYTHON -> TreeSitterTokenizer("python", tokenizerVersion = 1)
                RUST -> TreeSitterTokenizer("rust", tokenizerVersion = 1)
                GO -> TreeSitterTokenizer("go", tokenizerVersion = 1)
                KOTLIN -> TreeSitterTokenizer("kotlin", tokenizerVersion = 3)
                JAVASCRIPT -> TreeSitterTokenizer("javascript", tokenizerVersion = 1)
                CSHARP -> TreeSitterTokenizer("csharp", tokenizerVersion = 1)
                TYPESCRIPT -> TreeSitterTokenizer("typescript", tokenizerVersion = 1)
                CPLUSPLUS -> TreeSitterTokenizer("cpp", tokenizerVersion = 1)
                JAVA -> TreeSitterTokenizer("java", tokenizerVersion = 1)
                C -> TreeSitterTokenizer("c", tokenizerVersion = 1)
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
    var output: Long = 0L
    lateinit var source: Source

    enum class Source {
        FORNWALL,
        GEREONS,
        SEVEN_REBUX
        ;

        val aggregator: ExpectedOutputAggregator
            get() = when (this) {
                FORNWALL -> FornwallAggregator()
                GEREONS -> GereonsAggregator()
                SEVEN_REBUX -> SevenRebuxAggregator()
            }
    }
}