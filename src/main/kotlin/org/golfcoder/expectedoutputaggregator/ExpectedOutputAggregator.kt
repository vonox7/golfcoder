package org.golfcoder.expectedoutputaggregator

interface ExpectedOutputAggregator {
    suspend fun load(year: Int, day: Int)
}
