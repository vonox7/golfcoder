package org.golfcoder.plugins

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
import org.golfcoder.Sysinfo
import java.time.Duration

fun Application.configureHTTP() {
    if (!Sysinfo.isLocal) {
        install(CachingHeaders) {
            options { _, outgoingContent ->
                val oneDayInSeconds = 24 * 60 * 60
                val oneDayPublic =
                    CacheControl.MaxAge(maxAgeSeconds = oneDayInSeconds, visibility = CacheControl.Visibility.Public)
                when (outgoingContent.contentType?.withoutParameters()) {
                    ContentType.Text.CSS -> CachingOptions(oneDayPublic)
                    ContentType.Application.JavaScript -> CachingOptions(oneDayPublic)
                    ContentType.Image.XIcon -> CachingOptions(oneDayPublic)
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
        allowHost("golfcoder.org", schemes = listOf("https"))
        if (Sysinfo.isLocal) {
            allowHost("localhost:8030")
        }
        allowNonSimpleContentTypes = true
        allowCredentials = true
        maxAgeInSeconds = Duration.ofDays(1).seconds
    }
    install(DefaultHeaders) {
        // Security headers
        if (!Sysinfo.isLocal) {
            header("Strict-Transport-Security", "max-age=31536000; includeSubdomains; preload")
        }
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "SAMEORIGIN")
    }
    install(XForwardedHeaders)
    install(ContentNegotiation) {
        json()
    }
}
