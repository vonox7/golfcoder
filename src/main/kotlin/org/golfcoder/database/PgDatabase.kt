package org.golfcoder.database

import io.ktor.http.*
import io.r2dbc.spi.ConnectionFactoryOptions
import org.golfcoder.database.pgpayloads.ExpectedOutputTable
import org.jetbrains.exposed.v1.migration.r2dbc.MigrationUtils
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
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
    ).apply {
        // Test the connection
        transactionManager.newTransaction().exec("SELECT 1")
        println("PostgreSQL connection successful")

        // Migrate the database
        suspendTransaction {
            migrateDb()
        }
    }
}

// We directly migrate everything without flyway for simplicity.
// This means that in case of errors during migration a manual intervention is required.
suspend fun R2dbcTransaction.migrateDb() {
    val tables = arrayOf(
        ExpectedOutputTable
    )

    val statements = MigrationUtils.statementsRequiredForDatabaseMigration(*tables)

    if (statements.isEmpty()) {
        println("No database migrations required")
    } else {
        println("Applying database migrations:\n${statements.joinToString("\n")}")
        statements.forEach {
            exec(it)
        }
    }
}