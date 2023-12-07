package org.golfcoder.endpoints.web

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import org.golfcoder.database.Solution
import org.intellij.lang.annotations.Language

object TemplateView {
    suspend fun download(call: ApplicationCall) {
        val templateFileName = call.parameters["templateFileName"] ?: throw NotFoundException("No language specified")
        val (languageName, fileEnding) = Regex("golfcoder-([a-z]+)-template.([a-z]+)")
            .matchEntire(templateFileName)
            ?.destructured
            ?: throw NotFoundException("Invalid template file name")
        val language = Solution.Language.entries.find { it.name.lowercase() == languageName }
            ?: throw NotFoundException("Invalid language")
        if (fileEnding != language.fileEnding) throw NotFoundException("Invalid file ending")
        val template = language.template ?: throw NotFoundException("No template available")

        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, templateFileName)
                .toString()
        )
        call.respondText(contentType = ContentType.Text.Any, text = template)
    }

    val Solution.Language.template: String?
        get() = when (this) {
            Solution.Language.PYTHON -> {
                @Language("Python")
                val code = """
                |lines = []
                |while True:
                |    try:
                |        lines.append(input())
                |    except EOFError:
                |        break
                |print(len(lines))""".trimMargin()
                code
            }

            Solution.Language.JAVASCRIPT -> {
                @Language("JavaScript")
                val code = """let lines = [];
                |
                |require('readline')
                |    .createInterface({input: process.stdin})
                |    .on('line', (line) => {
                |        lines.push(line);
                |    })
                |    .on('close', () => {
                |        console.log(lines.length);
                |    });""".trimMargin()
                code
            }

            else -> null
        }
}
