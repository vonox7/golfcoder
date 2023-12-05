package golf.adventofcode

import golf.adventofcode.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

val container = System.getenv("CONTAINER") ?: "local"

fun main() {
    println("Starting ktor...")
    embeddedServer(Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 8030,
        host = System.getenv("HOST") ?: "0.0.0.0",
        module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureSerialization()
    configureTemplating()
    configureRouting()
}
