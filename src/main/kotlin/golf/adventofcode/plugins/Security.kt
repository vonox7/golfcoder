package golf.adventofcode.plugins

import com.moshbit.katerbase.child
import com.moshbit.katerbase.equal
import golf.adventofcode.Sysinfo
import golf.adventofcode.database.User
import golf.adventofcode.mainDatabase
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import kotlinx.coroutines.flow.count
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

private val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

const val sessionAuthenticationName = "session-aocg"
private const val oauthGoogleAuthenticationName = "oauth-google"

fun Application.configureSecurity() {
    install(Sessions) {
        val secretEncryptKey = System.getenv("SESSION_ENCRYPT_KEY") ?: run {
            if (Sysinfo.isLocal) {
                // We need for localhost development also a key, which should be persistent. But this key is not really a secret.
                "04d26957f9d853a229831119d4556be5"
            } else {
                throw Exception("No SESSION_KEY env-var set")
            }
        }

        val secretSignKey = System.getenv("SESSION_SIGN_KEY") ?: run {
            if (Sysinfo.isLocal) {
                // We need for localhost development also a key, which should be persistent. But this key is not really a secret.
                "4c374529af89134c7299eb993605"
            } else {
                throw Exception("No SESSION_KEY env-var set")
            }
        }

        cookie<UserSession>(sessionAuthenticationName) {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 30 * 24 * 60 * 60
            cookie.httpOnly = true
            cookie.secure = !Sysinfo.isLocal
            transform(SessionTransportTransformerEncrypt(hex(secretEncryptKey), hex(secretSignKey)))
        }
    }

    install(Authentication) {
        session<UserSession>(sessionAuthenticationName) {
            validate { session: UserSession ->
                // Check if user exists in db. Otherwise, we might have crashed during the oauth handshake
                session.takeIf {
                    mainDatabase.getSuspendingCollection<User>()
                        .find(User::_id equal session.userId)
                        .selectedFields(User::_id)
                        .count() == 1
                }
            }
            challenge("/login") // Redirect to /login if validate failed
        }
    }

    authentication {
        oauth(oauthGoogleAuthenticationName) {
            urlProvider = {
                if (Sysinfo.isLocal) "http://localhost:8030/oauth-callback" else "https://adventofcodegolf.com/oauth-callback"
            }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = System.getenv("OAUTH_GOOGLE_CLIENT_ID")
                        ?: error("No OAUTH_GOOGLE_CLIENT_ID env-var set"),
                    clientSecret = System.getenv("OAUTH_GOOGLE_CLIENT_SECRET")
                        ?: error("No OAUTH_GOOGLE_CLIENT_SECRET env-var set"),
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile")
                )
            }
            client = httpClient
        }
    }
    routing {
        authenticate(oauthGoogleAuthenticationName) {
            get("login") { // TODO rename later to /login-with-google when we have multiple oauth providers
                // Ktor automatically redirects to authorizeUrl, this lambda gets never called
            }

            get("/oauth-callback") {
                val principal: OAuthAccessTokenResponse.OAuth2 =
                    call.authentication.principal() ?: error("No principal")

                val userInfo: GoogleUserInfo = httpClient.get("https://www.googleapis.com/oauth2/v2/userinfo") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${principal.accessToken}")
                    }
                }.body()

                // Create user in db (if not exists), otherwise just update lastUsedOn
                val date = Date()
                val userInDatabase = mainDatabase.getSuspendingCollection<User>().findOneOrInsert(
                    User::oAuthDetails.child(User.OAuthDetails::provider) equal "google",
                    User::oAuthDetails.child(User.OAuthDetails::providerUserId) equal userInfo.id
                ) {
                    User().apply {
                        _id = randomId()
                        oAuthDetails = listOf(
                            User.OAuthDetails(
                                provider = "google",
                                providerUserId = userInfo.id,
                                createdOn = date,
                            )
                        )
                        createdOn = date
                        name = userInfo.name
                        publicProfilePictureUrl = userInfo.picture
                    }
                }

                call.sessions.set(UserSession(userInDatabase._id, userInDatabase.name))
                call.respondRedirect("/")
            }
        }
    }
}

class UserSession(val userId: String, val displayName: String) : Principal

@Serializable
private data class GoogleUserInfo(
    val id: String,
    val name: String,
    @SerialName("given_name") val givenName: String,
    @SerialName("family_name") val familyName: String,
    val picture: String,
    val locale: String
)