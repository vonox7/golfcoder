package org.golfcoder.database

import io.ktor.http.*
import io.r2dbc.spi.ConnectionFactoryOptions
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.transactions.transactionManager

suspend fun connectToPostgres(): R2dbcDatabase {
    // DATABASE_URL=postgresql://[user]:[password]@[url]:[port]/[dbname]
    val databaseUrl = System.getenv("DATABASE_URL") ?: "postgresql://postgres@localhost:5432/golfcoder"
    val url = Url(databaseUrl)

    return R2dbcDatabase.connect(
        url = "r2dbc:postgresql://${url.host}:${url.port}${url.encodedPath}",
        driver = "postgresql",
        user = url.user!!,
        password = url.password ?: "",
        databaseConfig = R2dbcDatabaseConfig.Builder()
            .apply {
                connectionFactoryOptions {
                    if (url.parameters["ssl"] == "true") {
                        option(ConnectionFactoryOptions.SSL, true)
                    }
                }
            }
    ).also {
        it.transactionManager.newTransaction().exec("SELECT 1").also {
            println("PostgreSQL connection successful")
        }
    }
}
