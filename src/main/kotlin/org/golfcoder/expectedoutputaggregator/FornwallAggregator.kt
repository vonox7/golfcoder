package org.golfcoder.expectedoutputaggregator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator.AggregatorResult.Failure
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator.AggregatorResult.Success
import org.golfcoder.httpClient

class FornwallAggregator : ExpectedOutputAggregator {
    override suspend fun load(year: Int, day: Int): ExpectedOutputAggregator.AggregatorResult {
        val input = httpClient.get(
            "https://raw.githubusercontent.com/fornwall/advent-of-code/main/crates/core/src/" +
                    "year$year/day${String.format("%02d", day)}_input.txt"
        ).bodyAsText()

        if (input.isEmpty()) {
            return Failure.NotYetAvailable
        }

        if (input.length > 100000) {
            return Failure.TooLongInput
        } else if (input.length < 10) {
            return Failure.NotYetAvailable
        }

        (1..2).forEach { part ->
            val output = httpClient.post("https://advent.fly.dev/solve/$year/$day/$part") {
                setBody(input)
            }
                .takeIf { it.status == io.ktor.http.HttpStatusCode.OK }
                ?.bodyAsText()
                ?.takeIf { it.count() < 50 } // Just to make sure not to parse garbage
                ?: run {
                    return Failure.DifferentFormat
                }

            save(year, day, part, ExpectedOutput.Source.FORNWALL, input, output)
        }
        return Success
    }
}