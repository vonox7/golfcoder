package org.golfcoder.utils

import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.protocol.Request
import io.sentry.protocol.User
import org.golfcoder.plugins.UserSession


fun initSentry() {
  val sentryDsn = System.getenv("SENTRY_DSN")
  if (sentryDsn == null) {
    println("No SENTRY_DSN found, skipping Sentry initialization")
  } else {
    Sentry.init { options ->
      options.dsn = sentryDsn
      options.addInAppInclude("org.golfcoder") // Mark our own packages in stacktrace
      options.release = System.getenv("CONTAINER_VERSION") // Git commit hash
      options.serverName = System.getenv("CONTAINER") // Container name
    }
  }
}

val SentryPlugin = createApplicationPlugin("SentryPlugin") {
  on(CallFailed) { call, cause ->
    Sentry.captureEvent(SentryEvent().apply {
      this.request = Request().apply {
        this.method = call.request.httpMethod.value
        this.url = call.request.uri
        this.headers = call.request.headers.entries().associate { it.key to it.value.joinToString() }
      }

      this.user = call.sessions.get<UserSession>()?.let { userSession ->
        User().apply {
          this.id = userSession.userId
          this.name = userSession.displayName
          this.ipAddress = call.request.origin.remoteHost
        }
      } ?: User().apply {
        this.ipAddress = call.request.origin.remoteHost
      }

      if (cause !is NotFoundException) { // Don't report 404 errors, they are gracefully handled with StatusPages plugin
        this.throwable = cause
      }
    })
  }
}