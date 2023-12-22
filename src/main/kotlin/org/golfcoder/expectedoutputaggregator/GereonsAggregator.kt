package org.golfcoder.expectedoutputaggregator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.httpClient
import org.golfcoder.mainDatabase

class GereonsAggregator : ExpectedOutputAggregator {
    override suspend fun load(year: Int, day: Int): ExpectedOutputAggregator.AggregatorResult {
        if (year != 2023) {
            return ExpectedOutputAggregator.AggregatorResult.Failure.YearNotInSource
        }

        val source = ExpectedOutput.Source.GEREONS

        val testcaseCode = httpClient.get(
            "https://raw.githubusercontent.com/gereons/AoC2023/main/Tests/Day${String.format("%02d", day)}Tests.swift"
        ).takeIf { it.status == HttpStatusCode.OK }?.bodyAsText() ?: run {
            return ExpectedOutputAggregator.AggregatorResult.Failure.NotYetAvailable
        }

        val testInput = testcaseCode
            .substringAfter("let testInput = \"\"\"")
            .substringBefore("\"\"\"")
            .trim()
            .takeIf { it.isNotEmpty() }

        if (testInput == null) {
            return ExpectedOutputAggregator.AggregatorResult.Failure.DifferentFormat
        }

        (1..2).forEach { part ->
            val output = Regex(
                """.*testInput\s*.\s*XCTAssertEqual\(day\.part$part\(\), ([0-9]+)\).*""",
                RegexOption.DOT_MATCHES_ALL
            )
                .matchEntire(testcaseCode)
                ?.groupValues
                ?.get(1)
                ?.toLongOrNull()
                ?: run {
                    return ExpectedOutputAggregator.AggregatorResult.Failure.DifferentFormat
                }

            mainDatabase.getSuspendingCollection<ExpectedOutput>().insertOne(
                ExpectedOutput().apply {
                    _id = generateId(year.toString(), day.toString(), part.toString(), source.name)
                    this.year = year
                    this.day = day
                    this.part = part
                    this.source = source
                    this.input = testcaseCode
                    this.output = output.toString()
                },
                upsert = true
            )
        }
        return ExpectedOutputAggregator.AggregatorResult.Success
    }
}