package golf.adventofcode

import golf.adventofcode.database.MainDatabase
import golf.adventofcode.endpoints.api.EditUserApi
import golf.adventofcode.endpoints.api.UploadSolutionApi
import golf.adventofcode.endpoints.web.*
import golf.adventofcode.plugins.configureHTTP
import golf.adventofcode.plugins.configureSecurity
import golf.adventofcode.plugins.sessionAuthenticationName
import golf.adventofcode.tokenizer.analyzerThread
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val container = System.getenv("CONTAINER") ?: "local"
lateinit var mainDatabase: MainDatabase

fun main() {
    println("Connecting to database...")
    mainDatabase = MainDatabase(System.getenv("MONGO_URL") ?: "mongodb://localhost:27017/advent-of-code-golf")
    analyzerThread.start()

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
