package golf.adventofcode.plugins

import golf.adventofcode.Sysinfo
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import java.time.Duration

fun Application.configureHTTP() {
    if (!Sysinfo.isLocal) {
        install(CachingHeaders) {
            options { _, outgoingContent ->
                val oneDayInSeconds = 24 * 60 * 60
                when (outgoingContent.contentType?.withoutParameters()) {
                    ContentType.Text.CSS -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = oneDayInSeconds))
                    ContentType.Application.JavaScript -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = oneDayInSeconds))
                    ContentType.Image.XIcon -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = oneDayInSeconds))
                    else -> null
                }
            }
        }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (Sysinfo.isLocal) {
                cause.printStackTrace()
            }
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    install(Compression) {
        default()
    }
    install(CORS) {
        allowHost("adventofcode.golf", schemes = listOf("https"))
        if (Sysinfo.isLocal) {
            allowHost("localhost:8030")
        }
        allowNonSimpleContentTypes = true
        allowCredentials = true
        maxAgeInSeconds = Duration.ofDays(1).seconds
    }
    install(DefaultHeaders)
    install(XForwardedHeaders)
    install(ContentNegotiation) {
        json()
    }
}
