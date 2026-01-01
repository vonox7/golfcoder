package org.golfcoder.expectedoutputaggregator

import org.golfcoder.database.ExpectedOutput
import org.golfcoder.database.pgpayloads.ExpectedOutputTable
import org.golfcoder.mainDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert

interface ExpectedOutputAggregator {
    suspend fun load(year: Int, day: Int): AggregatorResult

    sealed class AggregatorResult {
        data object Success : AggregatorResult()
        open class Failure : AggregatorResult() {
            data object YearNotInSource : Failure()
            data object NotYetAvailable : Failure()
            data object DifferentFormat : Failure()
            data object TooLongInput : Failure()
            data class UnknownError(val errorMessage: String) : Failure()
        }
    }

    suspend fun save(year: Int, day: Int, part: Int, source: ExpectedOutput.Source, input: String, output: String) {
        // Save to MongoDB
        mainDatabase.getSuspendingCollection<ExpectedOutput>().insertOne(
            ExpectedOutput().apply {
                _id = generateId(year.toString(), day.toString(), part.toString(), source.name)
                this.year = year
                this.day = day
                this.part = part
                this.source = source
                this.input = input
                this.output = output
            },
            upsert = true
        )

        // Save to PostgreSQL
        suspendTransaction {
            ExpectedOutputTable.upsert {
                it[this.year] = year
                it[this.day] = day
                it[this.part] = part
                it[this.input] = input
                it[this.output] = output
                it[this.sourceEnum] = source
            }
        }
    }
}
