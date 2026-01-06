package org.golfcoder.database

import kotlinx.datetime.LocalDateTime
import org.golfcoder.coderunner.Coderunner
import org.golfcoder.coderunner.KotlinPlaygroundCoderunner
import org.golfcoder.coderunner.OnecompilerCoderunner
import org.golfcoder.tokenizer.Tokenizer
import org.golfcoder.tokenizer.TreeSitterTokenizer
import java.util.*

class User {
    lateinit var id: String
    var oAuthDetails: List<OAuthDetails> = emptyList()
    lateinit var createdOn: LocalDateTime
    var name: String = ""
    var publicProfilePictureUrl: String? = null
    var nameIsPublic: Boolean = true
    var profilePictureIsPublic: Boolean = true
    var defaultLanguage: Solution.Language? = null
    var adventOfCodeRepositoryInfo: AdventOfCodeRepositoryInfo? = null

    // Admins can see all solutions to detect cheaters and can delete solutions. Must be set manually in the database.
    var admin: Boolean = false

    class OAuthDetails(
        val provider: String,
        val providerUserId: String,
        val createdOn: Date,
    )

    // Either singleAocRepositoryUrl, yearAocRepositoryUrl or none will be set
    class AdventOfCodeRepositoryInfo(
        val githubProfileName: String,
        var singleAocRepositoryUrl: String? = null,
        var yearAocRepositoryUrl: Map<String, String> = emptyMap(), // year-string to repository-Url
        var publiclyVisible: Boolean = true,
    )

    fun getAdventOfCodeRepositoryUrl(year: Int): String? {
        return (adventOfCodeRepositoryInfo?.singleAocRepositoryUrl
            ?: adventOfCodeRepositoryInfo?.yearAocRepositoryUrl?.get(year.toString()))
            ?.takeIf { adventOfCodeRepositoryInfo?.publiclyVisible == true }
    }
}

class Solution {
    lateinit var id: String
    lateinit var userId: String
    lateinit var uploadDate: LocalDateTime
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
                PYTHON -> TreeSitterTokenizer("python", tokenizerVersion = 6)
                RUST -> TreeSitterTokenizer("rust", tokenizerVersion = 6)
                GO -> TreeSitterTokenizer("go", tokenizerVersion = 6)
                KOTLIN -> TreeSitterTokenizer("kotlin", tokenizerVersion = 6)
                JAVASCRIPT -> TreeSitterTokenizer("javascript", tokenizerVersion = 6)
                CSHARP -> TreeSitterTokenizer("csharp", tokenizerVersion = 6)
                TYPESCRIPT -> TreeSitterTokenizer("typescript", tokenizerVersion = 6)
                CPLUSPLUS -> TreeSitterTokenizer("cpp", tokenizerVersion = 6)
                JAVA -> TreeSitterTokenizer("java", tokenizerVersion = 6)
                C -> TreeSitterTokenizer("c", tokenizerVersion = 6)
                SWIFT -> TreeSitterTokenizer("swift", tokenizerVersion = 6)
                SCALA -> TreeSitterTokenizer("scala", tokenizerVersion = 6)
                RUBY -> TreeSitterTokenizer("ruby", tokenizerVersion = 6)
                BASH -> TreeSitterTokenizer("bash", tokenizerVersion = 6)
            }
    }
}
