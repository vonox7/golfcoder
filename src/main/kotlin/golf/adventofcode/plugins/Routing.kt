package golf.adventofcode.plugins

import golf.adventofcode.tokens.PythonTokenizer
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/tmp-python-tokenizer") { // TODO remove tmp
            call.respondText(PythonTokenizer().getTokenCount("print('Hello, world!')").toString())
        }
        staticResources("/static", "static")
    }
}
