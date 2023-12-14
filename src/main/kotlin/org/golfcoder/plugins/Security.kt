package org.golfcoder.plugins

import com.moshbit.katerbase.child
import com.moshbit.katerbase.equal
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import io.ktor.util.reflect.*
import kotlinx.coroutines.flow.count
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.golfcoder.Sysinfo
import org.golfcoder.database.User
import org.golfcoder.endpoints.api.EditUserApi
import org.golfcoder.endpoints.web.LoginView
import org.golfcoder.endpoints.web.respondHtmlView
import org.golfcoder.httpClient
import org.golfcoder.mainDatabase
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
                    mainDatabase.getSuspendingCollection<User>()
                        .find(User::_id equal session.userId)
                        .selectedFields(User::_id)
                        .count() == 1
                }
            }
            challenge("/login") // Redirect to /login if validate failed
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
        postprocessLogin: (suspend (User, OauthUserInfoResponse) -> Unit)? = null,
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

                    val response = httpClient.get(userInfoUrl) {
                        headers {
                            append(HttpHeaders.Authorization, "Bearer ${principal.accessToken}")
                        }
                    }

                    val userInfo: OauthUserInfoResponse = try {
                        response.body(userInfoClassTypeInfo)
                    } catch (e: Exception) {
                        println("Error while parsing oauth2 user info: ${e.message}")
                        println("Response body: ${response.bodyAsText()}")
                        throw e
                    }

                    val currentSession = call.sessions.get<UserSession>()
                    val currentUser = currentSession?.let {
                        mainDatabase.getSuspendingCollection<User>()
                            .findOne(User::_id equal currentSession.userId)
                    }

                    val authenticatedUser: User // User which is now logged in, newly created, or linked to existing user

                    if (currentUser == null) {
                        // Login or register user
                        // Create user in db (if not exists), otherwise just update lastUsedOn
                        val date = Date()
                        authenticatedUser = mainDatabase.getSuspendingCollection<User>().findOneOrInsert(
                            User::oAuthDetails.child(User.OAuthDetails::provider) equal providerName,
                            User::oAuthDetails.child(User.OAuthDetails::providerUserId) equal userInfo.id
                        ) {
                            User().apply {
                                _id = randomId()
                                oAuthDetails = listOf(
                                    User.OAuthDetails(
                                        provider = providerName,
                                        providerUserId = userInfo.id,
                                        createdOn = date,
                                    )
                                )
                                createdOn = date
                                name = userInfo.name.take(EditUserApi.MAX_USER_NAME_LENGTH)
                                publicProfilePictureUrl = userInfo.pictureUrl
                            }
                        }
                    } else {
                        // Link oauth2 account to existing user
                        val existingUserWithThisProviderId = mainDatabase.getSuspendingCollection<User>().findOne(
                            User::oAuthDetails.child(User.OAuthDetails::provider) equal providerName,
                            User::oAuthDetails.child(User.OAuthDetails::providerUserId) equal userInfo.id
                        )
                        if (existingUserWithThisProviderId == null) {
                            // Link oauth2 account to existing user
                            authenticatedUser = mainDatabase.getSuspendingCollection<User>()
                                .updateOneAndFind(User::_id equal currentUser._id) {
                                    User::oAuthDetails push User.OAuthDetails(
                                        provider = providerName,
                                        providerUserId = userInfo.id,
                                        createdOn = Date(),
                                    )
                                    if (currentUser.publicProfilePictureUrl.isNullOrEmpty()) {
                                        User::publicProfilePictureUrl setTo userInfo.pictureUrl
                                    }
                                }!!
                        } else if (existingUserWithThisProviderId._id == currentUser._id) {
                            // User already exists and is already linked to this oauth2 account - nothing to do
                            authenticatedUser = existingUserWithThisProviderId
                        } else {
                            // User already exists and is linked to another oauth2 account - error
                            call.respondHtmlView("Account Link Error") {
                                h1 { +"Account Link Error" }
                                p { +"This ${LoginView.oauth2Providers[providerName]} account is already linked to another Golfcoder account." }
                            }
                            return@get
                        }
                    }

                    postprocessLogin?.invoke(authenticatedUser, userInfo)

                    call.sessions.set(UserSession(authenticatedUser._id, authenticatedUser.name))
                    call.respondRedirect("/")
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
        postprocessLogin = { user, userInfo ->
            EditUserApi.getAdventOfCodeRepositoryInfo((userInfo as GithubUserInfo).login)?.let { repoInfo ->
                mainDatabase.getSuspendingCollection<User>().updateOne(User::_id equal user._id) {
                    User::adventOfCodeRepositoryInfo setTo repoInfo
                }
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

class UserSession(val userId: String, val displayName: String) : Principal

@Serializable
private data class GoogleUserInfo(
    override val id: String,
    override val name: String,
    @SerialName("picture") override val pictureUrl: String,
) : OauthUserInfoResponse

@Serializable
private data class GithubUserInfo(
    @SerialName("id") val idLong: Long,
    val login: String, // e.g. vonox7, is changeable
    override val name: String,
    @SerialName("avatar_url") override val pictureUrl: String,
    // A bunch of more fields which we don't need
) : OauthUserInfoResponse {
    override val id: String
        get() = idLong.toString()
}

@Serializable
private data class RedditUserInfo(
    override val id: String,
    override val name: String,
    @SerialName("icon_img") override val pictureUrl: String,
    // A bunch of more fields which we don't need
) : OauthUserInfoResponse

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