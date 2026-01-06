package org.golfcoder.endpoints.web

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.golfcoder.database.pgpayloads.getUser
import org.golfcoder.plugins.UserSession
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object UnlinkLoginProviderView {
    suspend fun getHtml(call: ApplicationCall) = suspendTransaction {
        val session = call.sessions.get<UserSession>()!!
        val currentUser = session.getUser()

        val providerName = call.parameters["provider"]!!
        val providerDisplayName = LoginView.oauth2Providers[providerName]!!

        val currentLoginProviders = currentUser.oAuthDetails.map { it.provider }
        val remainingProviders =
            LoginView.oauth2Providers.filter { it.key in currentLoginProviders && it.key != providerName }

        call.respondHtmlView("Unlink $providerDisplayName") {
            h1 {
                +"Unlink $providerDisplayName"
            }
            if (remainingProviders.isEmpty()) {
                p {
                    +"You are not logged in via any other provider. Login first via another provider to be able to unlink this one."
                }
            } else {
                form(action = "/api/user/unlink/$providerName") {
                    p {
                        +"Are you sure you want to unlink your $providerDisplayName account?"
                    }
                    p {
                        +"You will still be able to login via ${remainingProviders.values.joinToString()}."
                    }
                    input(type = InputType.submit) {
                        name = "submitButton"
                        onClick = "submitForm(event)"
                        value = "Unlink $providerDisplayName"
                    }
                }
            }
        }
    }
}