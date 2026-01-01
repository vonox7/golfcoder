package org.golfcoder.expectedoutputaggregator

import org.golfcoder.database.pgpayloads.ExpectedOutputTable
import org.golfcoder.database.pgpayloads.Source
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

    suspend fun save(year: Int, day: Int, part: Int, source: Source, input: String, output: String) {
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
