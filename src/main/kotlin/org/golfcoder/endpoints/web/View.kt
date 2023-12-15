package org.golfcoder.endpoints.web

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.sessions.*
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
            errorDialog()
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
            content = "#0c171f"
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
            rel = "stylesheet"
            type = "text/css"
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
        iframe(classes = "github-star-button") {
            src = "https://ghbtns.com/github-btn.html?user=vonox7&repo=golfcoder&type=star&count=true&size=large"
            attributes["frameborder"] = "0"
            attributes["scrolling"] = "0"
            width = "140"
            height = "30"
            title = "GitHub"
        }
        a(href = "/", classes = "header-text") {
            img(src = "/static/images/favicon.ico", alt = "Logo", classes = "header-logo") {
                width = "20"
                height = "20"
            }
            +"Golfcoder"
        }
        +" "
        a(href = "/about") {
            +"About & FAQ".replace(" ", "\u00A0") // Non-breaking space
        }
        +" "
        val userSession = call.sessions.get<UserSession>()
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
            a(href = "/user/edit") {
                +userSession.displayName.replace(" ", "\u00A0") // Non-breaking space
            }
        }
    }
}

private fun HtmlBlockTag.errorDialog() {
    dialog {
        id = "error-dialog"
        div {
            div("header") {
                +"Error"
            }
            div {
                id = "error-dialog-body"
            }
            div("footer") {
                button {
                    onClick = """document.getElementById("error-dialog").close();"""
                    +"OK"
                }
            }
        }
    }
}


// Render either url or initials (fallback to XX initials)
fun HtmlBlockTag.renderUserProfileImage(user: User?, big: Boolean = false) {
    val publicProfilePictureUrl = user?.publicProfilePictureUrl?.takeIf { it.startsWith("https://") }
    val classes = "profile-image${if (big) " big" else ""}"
    if (publicProfilePictureUrl != null && user.profilePictureIsPublic) {
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