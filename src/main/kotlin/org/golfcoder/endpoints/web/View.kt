package org.golfcoder.endpoints.web

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import kotlinx.html.*
import org.golfcoder.Sysinfo
import org.golfcoder.database.User
import org.golfcoder.plugins.UserSession

suspend fun ApplicationCall.respondHtmlView(
    pageTitle: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    render: HtmlBlockTag.() -> Unit,
) {
    this.respondHtml(status) {
        head(pageTitle = pageTitle)
        body {
            header(this@respondHtmlView)
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

private fun HtmlBlockTag.header(call: ApplicationCall) {
    header {
        a(href = "/", classes = "header-text") {
            img(src = "/static/images/favicon.ico", alt = "Logo", classes = "header-logo") {
                width = "20"
                height = "20"
            }
            +"AdventOfCode.golf"
        }
        +" "
        a(href = "/about") {
            +"About & FAQ".replace(" ", "\u00A0") // Non-breaking space
        }
        +" "
        val userSession = call.principal<UserSession>()
        if (userSession == null) {
            a(href = "/login") {
                +"Login"
            }
            +" "
            if (Sysinfo.isLocal) {
                a(href = "/create-random-user") {
                    +"Create random user (local only)".replace(" ", "\u00A0") // Non-breaking space
                }
            }
        } else {
            +" "
            a(href = "/logout") {
                +"Logout"
            }
            +" "
            a(href = "/user/edit") {
                +userSession.displayName.replace(" ", "\u00A0") // Non-breaking space
            }
        }
    }
}

// Render either url or initials (fallback to XX initials)
fun HtmlBlockTag.renderUserProfileImage(user: User?, big: Boolean = false) {
    val publicProfilePictureUrl = user?.publicProfilePictureUrl?.takeIf { it.startsWith("https://") }
    val classes = "profile-image${if (big) " big" else ""}"
    if (publicProfilePictureUrl != null) {
        img(classes = classes) {
            src = publicProfilePictureUrl
            attributes["referrerpolicy"] = "no-referrer"
            attributes["loading"] = "lazy"
            alt = "Profile image"
        }
    } else {
        span(classes = classes) {
            val initials = user?.name?.split(" ")?.joinToString("") { it.first().uppercase() }?.take(2) ?: ""
            +(initials + "XX").take(2)
        }
    }
}