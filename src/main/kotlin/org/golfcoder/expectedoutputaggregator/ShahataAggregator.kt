package org.golfcoder.expectedoutputaggregator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator.AggregatorResult.Failure
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator.AggregatorResult.Success
import org.golfcoder.httpClient

class ShahataAggregator : ExpectedOutputAggregator {
    override suspend fun load(year: Int, day: Int): ExpectedOutputAggregator.AggregatorResult {

        val file = httpClient.get(
            "https://raw.githubusercontent.com/shahata/adventofcode-solver/refs/heads/main/src/$year/" +
                    "day${String.format("%02d", day)}.test.js"
        ).takeIf { it.status == HttpStatusCode.OK }?.bodyAsText() ?: run {
            return Failure.NotYetAvailable
        }

        if ("it should work for part 1 input" !in file || "it should work for part 2 input" !in file) {
            return Failure.DifferentFormat
        }

        (1..2).forEach { part ->
            val testcaseStartIndex = file.indexOf("it should work for part $part input")
            val expectStartIndex = file.indexOf("toEqual(", startIndex = testcaseStartIndex)
            val endIndex = file.indexOf(")", startIndex = expectStartIndex)
            val output = file.substring(expectStartIndex + "toEqual(".length, endIndex)
            if (output == "undefined") {
                return Failure.NotYetAvailable
            }
            if (output.toLongOrNull() == null && (output.firstOrNull() != '"' || output.lastOrNull() != '"')) {
                return Failure.DifferentFormat
            }

            val inputFile = httpClient.get(
                "https://raw.githubusercontent.com/shahata/adventofcode-solver/refs/heads/main/src/$year/" +
                        "day${String.format("%02d", day)}.txt"
            ).takeIf { it.status == HttpStatusCode.OK }?.bodyAsText() ?: run {
                return Failure.NotYetAvailable
            }

            save(year, day, part, ExpectedOutput.Source.SHAHATA, inputFile.trim(), output)
        }
        return Success
    }
}