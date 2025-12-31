package org.golfcoder.database

import io.ktor.http.*
import io.r2dbc.spi.ConnectionFactoryOptions
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig

fun connectToPostgres(): R2dbcDatabase {
    // DATABASE_URL=postgresql://[user]:[password]@[url]:[port]/[dbname]
    val databaseUrl = System.getenv("DATABASE_URL")
        ?: throw IllegalStateException("DATABASE_URL environment variable is not set")
    val url = Url(databaseUrl)

    return R2dbcDatabase.connect(
        url = "r2dbc:postgresql://${url.host}:${url.port}/${url.encodedPath}",
        driver = "postgresql",
        user = url.user!!.substringBefore(":"),
        password = url.user?.substringAfter(":", "") ?: "",
        databaseConfig = R2dbcDatabaseConfig.Builder()
            .apply {
                connectionFactoryOptions {
                    option(ConnectionFactoryOptions.SSL, true)
                }
            }
    )
}
