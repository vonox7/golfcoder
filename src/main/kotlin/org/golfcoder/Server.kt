package org.golfcoder

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import org.golfcoder.database.MainDatabase
import org.golfcoder.endpoints.api.EditUserApi
import org.golfcoder.endpoints.api.UploadSolutionApi
import org.golfcoder.endpoints.web.*
import org.golfcoder.plugins.configureHTTP
import org.golfcoder.plugins.configureSecurity
import org.golfcoder.plugins.sessionAuthenticationName

val container = System.getenv("CONTAINER") ?: "local"
lateinit var mainDatabase: MainDatabase
val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

suspend fun main() {
    println("Connecting to database...")
    mainDatabase = MainDatabase(System.getenv("MONGO_URL") ?: "mongodb://localhost:27017/golfcoder")

    // Wait for tree-sitter server to start
    if (!Sysinfo.isLocal) {
        println("Checking tree-sitter server...")
        while (true) {
            try {
                httpClient.get("http://localhost:8031").body<String>().contains("tree-sitter")
                break
            } catch (e: Exception) {
                println("Waiting for tree-sitter server to start...")
                delay(500)
            }
        }
    }

    println("Starting ktor...")
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 8030,
        host = if (Sysinfo.isLocal) "localhost" else "0.0.0.0",
        module = Application::ktorServerModule
    ).start(wait = true)
}

private fun Application.ktorServerModule() {
    configureSecurity()
    configureHTTP()

    routing {
        get("/") { call.respondRedirect("/2023") }
        get("/favicon.ico") { call.respondRedirect("/static/images/favicon.ico", permanent = true) }

        authenticate(sessionAuthenticationName, optional = true) {
            get(Regex("about")) { AboutView.getHtml(call) }
            get(Regex("20(?<year>[0-9]{2})")) { LeaderboardYearView.getHtml(call) }
            get(Regex("20(?<year>[0-9]{2})/day/(?<day>[0-9]{1,2})")) { LeaderboardDayView.getHtml(call) }
            get("/logout") { LogoutView.doLogout(call) }
        }

        authenticate(sessionAuthenticationName) {
            get("/user/edit") { EditUserView.getHtml(call) }

            post("/api/user/edit") { EditUserApi.post(call) }
            post("/api/solution/upload") { UploadSolutionApi.post(call) }
        }

        // TODO robots.txt, etc.

        if (Sysinfo.isLocal) {
            get("/create-random-user") { DebugView.createRandomUser(call) }
        }

        get("/template/{templateFileName}") { TemplateView.download(call) }
        staticResources("/static/css-${Sysinfo.release}", "static/css")
        staticResources("/static/js-${Sysinfo.release}", "static/js")
        staticResources("/static/images", "static/images")
    }
}
