package org.golfcoder.database.pgpayloads

import kotlinx.datetime.LocalDateTime
import org.golfcoder.coderunner.Coderunner
import org.golfcoder.coderunner.KotlinPlaygroundCoderunner
import org.golfcoder.coderunner.OnecompilerCoderunner
import org.golfcoder.tokenizer.Tokenizer
import org.golfcoder.tokenizer.TreeSitterTokenizer
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object SolutionTable : Table("solution") {
    val id = varchar("id", length = 32)
    val userId = varchar("userId", length = 32) // TODO change to ref
    val uploadDate = datetime("uploadDate").defaultExpression(CurrentDateTime)
    val code = text("code")
    val language = enumerationByName("language", length = 20, Solution.Language::class)
    val year = integer("year")
    val day = integer("day")
    val part = integer("part")
    val codePublicVisible = bool("codePublicVisible")
    val tokenCount = integer("tokenCount")
    val tokenizerVersion = integer("tokenizerVersion")
    val markedAsCheated = bool("markedAsCheated").default(false)

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, year, day, part, markedAsCheated, tokenCount)
        index(isUnique = false, language, tokenizerVersion)
        index(isUnique = false, userId, uploadDate)
    }
}

class Solution(
    val id: String,
    val userId: String,
    val uploadDate: LocalDateTime,
    val code: String,
    val language: Language,
    val year: Int,
    val day: Int,
    val part: Int,
    val codePubliclyVisible: Boolean,
    val tokenCount: Int,
    val tokenizerVersion: Int,
    val markedAsCheated: Boolean
) {
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

fun ResultRow.toSolution(): Solution {
    return Solution(
        id = this[SolutionTable.id],
        userId = this[SolutionTable.userId],
        uploadDate = this[SolutionTable.uploadDate],
        code = this[SolutionTable.code],
        language = this[SolutionTable.language],
        year = this[SolutionTable.year],
        day = this[SolutionTable.day],
        part = this[SolutionTable.part],
        codePubliclyVisible = this[SolutionTable.codePublicVisible],
        tokenCount = this[SolutionTable.tokenCount],
        tokenizerVersion = this[SolutionTable.tokenizerVersion],
        markedAsCheated = this[SolutionTable.markedAsCheated],
    )
}