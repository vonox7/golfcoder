package org.golfcoder.plugins

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import io.ktor.util.reflect.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.golfcoder.Sysinfo
import org.golfcoder.database.User
import org.golfcoder.database.pgpayloads.UserTable
import org.golfcoder.database.pgpayloads.getUser
import org.golfcoder.database.pgpayloads.toUser
import org.golfcoder.endpoints.api.EditUserApi
import org.golfcoder.endpoints.web.LoginView
import org.golfcoder.endpoints.web.respondHtmlView
import org.golfcoder.httpClient
import org.golfcoder.utils.bodyOrPrintException
import org.golfcoder.utils.randomId
import org.golfcoder.utils.toKotlinLocalDateTime
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.util.*

const val sessionAuthenticationName = "session-aocg"

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
                    suspendTransaction {
                        UserTable.select(UserTable.id).where(UserTable.id eq session.userId).count() == 1L
                    }
                }
            }
            challenge {
                // The user has a session cookie but we don't have this user in the db (maybe server restart during login?).
                // Make sure that the user can create a new session
                call.sessions.clear<UserSession>()
                call.respondRedirect("/login")
            }
        }
    }

    fun addOauthProvider(
        providerName: String,
        authorizeUrl: String,
        accessTokenUrl: String,
        accessTokenRequiresBasicAuth: Boolean = false,
        extraAuthParameters: List<Pair<String, String>> = emptyList(),
        extraTokenParameters: List<Pair<String, String>> = emptyList(),
        defaultScopes: List<String>,
        userInfoUrl: String,
        userInfoClassTypeInfo: TypeInfo,
        postprocessLogin: (suspend (User, OauthUserInfoResponse, OAuthAccessTokenResponse.OAuth2) -> Unit)? = null,
    ) {
        authentication {
            oauth(providerName) {
                urlProvider = {
                    if (Sysinfo.isLocal) "http://localhost:8030/login/$providerName" else "https://golfcoder.org/login/$providerName"
                }
                providerLookup = {
                    OAuthServerSettings.OAuth2ServerSettings(
                        name = providerName,
                        authorizeUrl = authorizeUrl,
                        accessTokenUrl = accessTokenUrl,
                        accessTokenRequiresBasicAuth = accessTokenRequiresBasicAuth,
                        requestMethod = HttpMethod.Post,
                        clientId = System.getenv("OAUTH_${providerName.uppercase()}_CLIENT_ID")
                            ?: error("No OAUTH_${providerName.uppercase()}_CLIENT_ID env-var set"),
                        clientSecret = System.getenv("OAUTH_${providerName.uppercase()}_CLIENT_SECRET")
                            ?: error("No OAUTH_${providerName.uppercase()}_CLIENT_SECRET env-var set"),
                        defaultScopes = defaultScopes,
                        extraAuthParameters = extraAuthParameters,
                        extraTokenParameters = extraTokenParameters,
                    )
                }
                client = httpClient
            }
        }

        routing {
            // This function creates new users, logs users in and links oauth2 accounts to existing users
            authenticate(providerName) {
                get("/login/$providerName") {  // Ktor automatically redirects first to authorizeUrl
                    val principal: OAuthAccessTokenResponse.OAuth2 =
                        call.authentication.principal() ?: error("No principal")

                    val userInfo: OauthUserInfoResponse = httpClient.get(userInfoUrl) {
                        header(HttpHeaders.Authorization, "Bearer ${principal.accessToken}")
                    }.bodyOrPrintException(userInfoClassTypeInfo)

                    handleLogin(call, providerName, userInfo, postprocessLogin, principal)
                }
            }
        }
    }

    addOauthProvider(
        providerName = "google",
        authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
        accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
        defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile"),
        userInfoUrl = "https://www.googleapis.com/oauth2/v1/userinfo",
        userInfoClassTypeInfo = typeInfo<GoogleUserInfo>(),
    )

    addOauthProvider(
        providerName = "github",
        authorizeUrl = "https://github.com/login/oauth/authorize",
        accessTokenUrl = "https://github.com/login/oauth/access_token",
        defaultScopes = emptyList(),
        userInfoUrl = "https://api.github.com/user",
        userInfoClassTypeInfo = typeInfo<GithubUserInfo>(),
        postprocessLogin = { user, userInfo, principal ->
            val repoInfo = EditUserApi.getAdventOfCodeRepositoryInfo((userInfo as GithubUserInfo).login, principal)
            UserTable.update({ UserTable.id eq user.id }) {
                it[adventOfCodeRepositoryInfo] = UserTable.AdventOfCodeRepositoryInfo(
                    githubProfileName = userInfo.login,
                    singleAocRepositoryUrl = repoInfo.singleAocRepositoryUrl,
                    yearAocRepositoryUrl = repoInfo.yearAocRepositoryUrl,
                    publiclyVisible = repoInfo.publiclyVisible,
                )
            }
        }
    )

    addOauthProvider(
        providerName = "reddit",
        authorizeUrl = "https://www.reddit.com/api/v1/authorize",
        accessTokenUrl = "https://www.reddit.com/api/v1/access_token",
        accessTokenRequiresBasicAuth = true, // See https://www.reddit.com/r/redditdev/comments/si7cnj/reddit_error_message_unauthorized_error_401/
        defaultScopes = listOf("identity"),
        userInfoUrl = "https://oauth.reddit.com/api/v1/me",
        userInfoClassTypeInfo = typeInfo<RedditUserInfo>(),
    )

    addOauthProvider(
        providerName = "twitter",
        authorizeUrl = "https://twitter.com/i/oauth2/authorize",
        accessTokenUrl = "https://api.twitter.com/2/oauth2/token",
        accessTokenRequiresBasicAuth = true,
        // See https://developer.twitter.com/en/docs/authentication/oauth-2-0/authorization-code
        extraAuthParameters = listOf(
            "code_challenge" to "challenge",
            "code_challenge_method" to "plain",
        ),
        // See https://developer.twitter.com/en/docs/authentication/oauth-2-0/user-access-token
        extraTokenParameters = listOf(
            "code_verifier" to "challenge",
        ),
        // Scopes needed for https://developer.twitter.com/en/docs/twitter-api/users/lookup/api-reference/get-users-me
        defaultScopes = listOf("users.read", "tweet.read"),
        userInfoUrl = "https://api.twitter.com/2/users/me",
        userInfoClassTypeInfo = typeInfo<TwitterUserInfo>(),
    )
}

private suspend fun findUserByOAuthProviderId(
    providerName: String,
    providerUserId: String
): User? {
    // TODO Do that in the query
    return UserTable.selectAll().map { it.toUser() }.firstOrNull { user ->
        user.oAuthDetails.any { it.provider == providerName && it.providerUserId == providerUserId }
    }
}

private suspend fun handleLogin(
    call: ApplicationCall,
    providerName: String,
    userInfo: OauthUserInfoResponse,
    postprocessLogin: (suspend (User, OauthUserInfoResponse, OAuthAccessTokenResponse.OAuth2) -> Unit)?,
    principal: OAuthAccessTokenResponse.OAuth2
) = suspendTransaction {
    val currentSession = call.sessions.get<UserSession>()
    val currentUser = currentSession?.getUser()

    val authenticatedUser: User // User which is now logged in, newly created, or linked to existing user

    if (currentUser == null) {
        // Login or register user
        authenticatedUser = findUserByOAuthProviderId(providerName, userInfo.id)
            ?: UserTable.insertReturning {
                it[id] = randomId()
                it[oauthDetails] = arrayOf(
                    UserTable.OAuthDetails(
                        provider = providerName,
                        providerUserId = userInfo.id,
                        createdOn = Date().toKotlinLocalDateTime()
                    )
                )
                it[name] = userInfo.name.take(EditUserApi.MAX_USER_NAME_LENGTH)
                it[publicProfilePictureUrl] = userInfo.pictureUrl
            }.single().toUser()
    } else {
        // Link oauth2 account to existing user
        val existingUserWithThisProviderId = findUserByOAuthProviderId(providerName, userInfo.id)
        if (existingUserWithThisProviderId == null) {
            // Link oauth2 account to existing user
            authenticatedUser = UserTable.updateReturning(where = { UserTable.id eq currentUser.id }) {
                it[oauthDetails] = currentUser.oAuthDetails.map {
                    UserTable.OAuthDetails(
                        provider = it.provider,
                        providerUserId = it.providerUserId,
                        createdOn = it.createdOn.toKotlinLocalDateTime()
                    )
                }.plus(
                    UserTable.OAuthDetails(
                        provider = providerName,
                        providerUserId = userInfo.id,
                        createdOn = Date().toKotlinLocalDateTime()
                    )
                ).toTypedArray()
                if (currentUser.publicProfilePictureUrl.isNullOrEmpty()) {
                    it[publicProfilePictureUrl] = userInfo.pictureUrl
                }
            }.single().toUser()
        } else if (existingUserWithThisProviderId.id == currentUser.id) {
            // User already exists and is already linked to this oauth2 account - nothing to do
            authenticatedUser = existingUserWithThisProviderId
        } else {
            // User already exists and is linked to another oauth2 account - error
            call.respondHtmlView("Account Link Error") {
                h1 { +"Account Link Error" }
                p { +"This ${LoginView.oauth2Providers[providerName]} account is already linked to another Golfcoder account." }
            }
            return@suspendTransaction
        }
    }

    postprocessLogin?.invoke(authenticatedUser, userInfo, principal)

    call.sessions.set(UserSession(authenticatedUser.id, authenticatedUser.name))
    call.respondRedirect("/")
}

@Serializable
class UserSession(val userId: String, val displayName: String)

@Serializable
private data class GoogleUserInfo(
    override val id: String,
    override val name: String,
    @SerialName("picture") override val pictureUrl: String,
) : OauthUserInfoResponse

@Serializable
private data class GithubUserInfo(
    @SerialName("id") val idLong: Long,
    val login: String, // username, e.g. vonox7, is changeable
    @SerialName("name") val givenName: String? = null, // e.g. "John Doe", or null if only username(="login") is set
    @SerialName("avatar_url") override val pictureUrl: String,
    // A bunch of more fields which we don't need
) : OauthUserInfoResponse {
    override val id: String
        get() = idLong.toString()

    override val name: String
        get() = givenName ?: login
}

@Serializable
private data class RedditUserInfo(
    override val id: String,
    override val name: String,
    @SerialName("icon_img") val fullPictureUrl: String,
    // A bunch of more fields which we don't need
) : OauthUserInfoResponse {
    override val pictureUrl: String
        get() = fullPictureUrl.substringBeforeLast("?") // When size parameters are present, reddit might serve an invalid response
}

@Serializable
private data class TwitterUserInfo(
    val data: TwitterUserData,
) : OauthUserInfoResponse {
    override val id: String get() = data.id
    override val name: String get() = data.name
    override val pictureUrl: String get() = data.pictureUrl

    @Serializable
    class TwitterUserData(
        val id: String,
        val name: String,
        @SerialName("profile_image_url") val pictureUrl: String = "",
    )
}

interface OauthUserInfoResponse {
    val id: String
    val name: String
    val pictureUrl: String
}