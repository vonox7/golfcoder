package org.golfcoder.database

import com.moshbit.katerbase.MongoMainEntry
import com.moshbit.katerbase.MongoSubEntry
import org.golfcoder.coderunner.Coderunner
import org.golfcoder.coderunner.KotlinPlaygroundCoderunner
import org.golfcoder.coderunner.OnecompilerCoderunner
import org.golfcoder.tokenizer.NotYetAvailableTokenizer
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
                RUST -> NotYetAvailableTokenizer()
                GO -> NotYetAvailableTokenizer()
                KOTLIN -> TreeSitterTokenizer("kotlin", tokenizerVersion = 1)
                JAVASCRIPT -> TreeSitterTokenizer("javascript", tokenizerVersion = 1)
                CSHARP -> NotYetAvailableTokenizer()
                TYPESCRIPT -> NotYetAvailableTokenizer()
                CPLUSPLUS -> NotYetAvailableTokenizer()
                JAVA -> NotYetAvailableTokenizer()
                C -> NotYetAvailableTokenizer()
            }
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