package org.golfcoder.expectedoutputaggregator

interface ExpectedOutputAggregator {
    suspend fun load(year: Int, day: Int): AggregatorResult

    sealed class AggregatorResult {
        data object Success : AggregatorResult()
        open class Failure : AggregatorResult() {
            data object YearNotInSource : Failure()
            data object NotYetAvailable : Failure()
            data object DifferentFormat : Failure()
            data class UnknownError(val errorMessage: String) : Failure()
        }
    }
}
