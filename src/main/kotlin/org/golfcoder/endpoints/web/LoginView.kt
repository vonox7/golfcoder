package org.golfcoder.endpoints.web

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.golfcoder.database.pgpayloads.getUser
import org.golfcoder.plugins.UserSession
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object LoginView {
    // providerName to providerDisplayName
    val oauth2Providers =
        mapOf("github" to "GitHub", "google" to "Google", "reddit" to "Reddit", "twitter" to "Twitter")

    suspend fun getHtml(call: ApplicationCall) = suspendTransaction {
        val session = call.sessions.get<UserSession>()
        val currentUser = session?.getUser()

        call.respondHtmlView(if (currentUser == null) "Login" else "Link Account") {
            if (currentUser == null) {
                h1 {
                    +"Login or create new account"
                }
                p {
                    +"Please login via OAuth2 with one of theses providers:"
                }
                ul {
                    for ((providerName, providerDisplayName) in oauth2Providers) {
                        li {
                            a(href = "/login/$providerName") { +providerDisplayName }
                            if (providerName == "github") {
                                +" (recommended to link your leaderboard solutions to your public advent-of-code repository)"
                            }
                        }
                    }
                }
            } else {
                val currentLoginProviders = currentUser.oAuthDetails.map { it.provider }
                val newLoginProviders = oauth2Providers.filter { it.key !in currentLoginProviders }
                h1 {
                    +"Link Account"
                }
                p {
                    +"You are already logged in via ${currentLoginProviders.joinToString { oauth2Providers[it]!! }}."
                }
                if (newLoginProviders.isNotEmpty()) {
                    p {
                        +"If you want to link another account, select one of these providers."
                    }
                    ul {
                        for ((providerName, providerDisplayName) in newLoginProviders) {
                            li {
                                a(href = "/login/$providerName") { +providerDisplayName }
                            }
                        }
                    }
                }
                h2 {
                    +"Unlink Account"
                }
                p {
                    if (currentLoginProviders.count() > 1) {
                        +"You can unlink all but your last provider. Click on the provider to unlink it."
                    } else {
                        +"You can't unlink your last provider. Login first via another provider to be able to unlink this one."
                    }
                }
                ul {
                    currentLoginProviders.forEach { providerName ->
                        li {
                            a(href = "/user/unlink/$providerName") { +oauth2Providers[providerName]!! }
                        }
                    }
                }
            }

            p {
                +"You can link multiple providers to the same Golfcoder user. "
                +"This allows you to login with any of these providers. "
                +"You can also unlink providers again or delete your Golfcoder account at any time."
            }
        }
    }
}