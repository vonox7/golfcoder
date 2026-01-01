package org.golfcoder.expectedoutputaggregator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator.AggregatorResult.Failure
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator.AggregatorResult.Success
import org.golfcoder.httpClient

class SimAggregator : ExpectedOutputAggregator {
    override suspend fun load(year: Int, day: Int): ExpectedOutputAggregator.AggregatorResult {
        val file = httpClient.get(
            "https://raw.githubusercontent.com/sim642/adventofcode/refs/heads/master/src/test/scala/eu/" +
                    "sim642/adventofcode$year/Day${day}Test.scala"
        ).takeIf { it.status == HttpStatusCode.OK }?.bodyAsText() ?: run {
            return Failure.NotYetAvailable
        }

        if ("Part 1 input answer" !in file || "Part 2 input answer" !in file) {
            return Failure.DifferentFormat
        }

        (1..2).forEach { part ->
            val testcaseStartIndex = file.indexOf("Part $part input answer")
            val expectStartIndex = file.indexOf(" == ", startIndex = testcaseStartIndex)
            val endIndex = file.indexOf(")", startIndex = expectStartIndex)
            val outputRaw = file.substring(expectStartIndex + " == ".length, endIndex)
            val output = when {
                outputRaw.toLongOrNull() != null -> outputRaw
                outputRaw.removeSuffix("L").toLongOrNull() != null -> outputRaw.removeSuffix("L")
                outputRaw.startsWith("\"") && outputRaw.endsWith("\"") -> outputRaw.removeSurrounding("\"")
                else -> return Failure.DifferentFormat
            }

            val inputFile = httpClient.get(
                "https://raw.githubusercontent.com/sim642/adventofcode/refs/heads/master/src/main/" +
                        "resources/eu/sim642/adventofcode$year/day$day.txt"
            ).takeIf { it.status == HttpStatusCode.OK }?.bodyAsText() ?: run {
                return Failure.NotYetAvailable
            }

            save(year, day, part, ExpectedOutput.Source.SIM, inputFile.trim(), output)
        }
        return Success
    }
}