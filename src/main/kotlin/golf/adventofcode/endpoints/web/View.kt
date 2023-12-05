package golf.adventofcode.endpoints.web

import golf.adventofcode.Sysinfo
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import kotlinx.html.*

suspend fun ApplicationCall.respondHtmlView(
    pageTitle: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    render: HtmlBlockTag.() -> Unit,
) {
    this.respondHtml(status) {
        head(pageTitle = pageTitle)
        body {
            header()
            render()
        }
    }
}

private fun HTML.head(pageTitle: String) {
    head {
        meta {
            charset = "UTF-8"
        }
        meta {
            name = "viewport"
            content = "width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=5"
        }
        meta {
            name = "theme-color"
            content = "#fcdd2d"
        }
        title {
            +pageTitle
        }
        link {
            rel = "icon"
            type = "image/x-icon"
            href = "/static/images/favicon.ico"
        }
        link {
            rel = "shortcut icon"
            href = "/static/images/favicon.ico"
        }

        link {
            rel = "preload stylesheet"
            attributes["as"] = "style"
            type = "text/css"
            attributes["crossorigin"] = "anonymous"
            href = "/static/css-${Sysinfo.release}/main.css"
        }
        script {
            src = "/static/js-${Sysinfo.release}/main.js"
            defer = true
        }
    }
}

private fun HtmlBlockTag.header() {
    header {
        a(href = "/", classes = "header-text") {
            img(src = "/static/images/favicon.ico", alt = "Logo", classes = "header-logo") {
                width = "20"
                height = "20"
            }
            +"AdventOfCode.golf"
        }
        a(href = "/about") {
            +"About"
        }
    }
}