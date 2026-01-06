package org.golfcoder

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.golfcoder.database.connectToPostgres
import org.golfcoder.database.testPostgresConnection
import org.golfcoder.endpoints.api.*
import org.golfcoder.endpoints.web.*
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregatorLoader
import org.golfcoder.plugins.configureHTTP
import org.golfcoder.plugins.configureSecurity
import org.golfcoder.plugins.sessionAuthenticationName
import org.golfcoder.tokenizer.TokenRecalculator
import org.golfcoder.utils.SentryPlugin
import org.golfcoder.utils.initSentry
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import java.util.*

lateinit var pgDatabase: R2dbcDatabase
val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            // DefaultJson
            encodeDefaults = true
            isLenient = true
            allowSpecialFloatingPointValues = true
            allowStructuredMapKeys = true
            prettyPrint = false
            useArrayPolymorphism = false

            // additional parameters
            ignoreUnknownKeys = true
        })
    }
}

fun main(): Unit = runBlocking {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    initSentry()

    println("Connecting to database...")
    pgDatabase = connectToPostgres()

    println("Starting ktor...")
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8030
    embeddedServer(
        Netty,
        port = port,
        host = if (Sysinfo.isLocal) "localhost" else "0.0.0.0",
        module = Application::ktorServerModule
    ).start(wait = false)
    println("Server started at http://localhost:$port")

    launch {
        TokenRecalculator.recalculateSolutions()
    }

    launch {
        ExpectedOutputAggregatorLoader.loadOnStartup()
    }

    launch {
        // During container restart we might interrupt score calculation.
        // So ensure that on startup all scores get recalculated.
        UploadSolutionApi.recalculateAllScores()
    }
}

private fun Application.ktorServerModule() {
    configureSecurity()
    configureHTTP()
    install(SentryPlugin)

    routing {
        get("/health") { healthCheck(call) }
        get("/") { call.respondRedirect("/${UploadSolutionApi.YEARS_RANGE.last}") }
        get("/favicon.ico") { call.respondRedirect("/static/images/favicon.ico", permanent = true) }

        authenticate(sessionAuthenticationName, optional = true) {
            get("/faq") { FaqView.getHtml(call) }
            get(Regex("/20(?<year>[0-9]{2})")) { LeaderboardYearView.getHtml(call) }
            get(Regex("/20(?<year>[0-9]{2})/day/(?<day>[0-9]{1,2})")) { LeaderboardDayView.getHtml(call) }
            get("/login") { LoginView.getHtml(call) }
            get("/logout") { LogoutView.doLogout(call) }

            post("/api/solution/upload") { UploadSolutionApi.post(call) }
        }

        authenticate(sessionAuthenticationName) {
            get("/user/edit") { EditUserView.getHtml(call) }
            get("/user/unlink/{provider}") { UnlinkLoginProviderView.getHtml(call) }
            get("/user/delete") { DeleteUserView.getHtml(call) }

            post("/api/user/edit") { EditUserApi.post(call) }
            post("/api/user/unlink/{provider}") { UnlinkLoginProviderApi.post(call) }
            post("/api/user/delete") { DeleteUserApi.post(call) }

            post("/api/admin/markSolutionAsCheated") { MarkSolutionAsCheatedApi.post(call) }
        }

        if (Sysinfo.isLocal) {
            get("/create-random-user") { DebugView.createRandomUser(call) }
        }

        get("/solution/{solutionFileName}") { SolutionView.download(call) }
        get("/template/{templateFileName}") { TemplateView.download(call) }
        staticResources("/static/css-{release}", "static/css")
        staticResources("/static/js-{release}", "static/js")
        staticResources("/static/images", "static/images")
    }
}

private suspend fun healthCheck(call: ApplicationCall) {
    try {
        testPostgresConnection()

        val treeSitterResponse = httpClient.get("http://localhost:8031").body<String>()
        require(treeSitterResponse.contains("tree-sitter"))

        call.respondText("OK")
    } catch (e: Exception) {
        call.respondText("NOT OK: ${e.message}", status = HttpStatusCode.ServiceUnavailable)
    }
}