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
  Sentry.init { options ->
    options.dsn = "https://db4206f768de71d93f1a38a408b895cd@o4508395080712192.ingest.de.sentry.io/4508395085889616"
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

      this.throwable = cause
    })
  }
}