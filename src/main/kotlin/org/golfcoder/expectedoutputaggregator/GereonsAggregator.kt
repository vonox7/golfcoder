package org.golfcoder.expectedoutputaggregator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator.AggregatorResult.Failure
import org.golfcoder.httpClient
import org.golfcoder.mainDatabase

class GereonsAggregator : ExpectedOutputAggregator {
    override suspend fun load(year: Int, day: Int): ExpectedOutputAggregator.AggregatorResult {
        if (year != 2024) {
            return Failure.YearNotInSource
        }

        val source = ExpectedOutput.Source.GEREONS

        val testcaseCode = httpClient.get(
            "https://raw.githubusercontent.com/gereons/AoC2024/main/Tests/Day${String.format("%02d", day)}Tests.swift"
        ).takeIf { it.status == HttpStatusCode.OK }?.bodyAsText() ?: run {
            return Failure.NotYetAvailable
        }

        if ("fileprivate let testInput = \"\"\"\n\"\"\"" in testcaseCode) {
            return Failure.NotYetAvailable
        }

        val testInput = testcaseCode
            .substringAfter("let testInput = \"\"\"", missingDelimiterValue = "")
            .substringBefore("\"\"\"", missingDelimiterValue = "")
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.takeIf { "func " !in it } // Sanity check to make sure we don't parse the actual code

        if (testInput == null) {
            return Failure.DifferentFormat
        } else if (testInput.length > 100000) {
            return Failure.TooLongInput
        } else if (testInput.length < 10) {
            return Failure.NotYetAvailable
        }

        (1..2).forEach { part ->
            val output = Regex(
                """.*testInput\s*.\s*#expect\(day\.part$part\(\) == ([0-9]+)\).*""",
                RegexOption.DOT_MATCHES_ALL
            )
                .matchEntire(testcaseCode)
                ?.groupValues
                ?.get(1)
                ?.toLongOrNull()
                ?: run {
                    return Failure.DifferentFormat
                }

            mainDatabase.getSuspendingCollection<ExpectedOutput>().insertOne(
                ExpectedOutput().apply {
                    _id = generateId(year.toString(), day.toString(), part.toString(), source.name)
                    this.year = year
                    this.day = day
                    this.part = part
                    this.source = source
                    this.input = testInput
                    this.output = output.toString()
                },
                upsert = true
            )
        }
        return ExpectedOutputAggregator.AggregatorResult.Success
    }
}