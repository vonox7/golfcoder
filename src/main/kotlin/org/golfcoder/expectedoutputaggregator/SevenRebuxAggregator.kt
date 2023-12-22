package org.golfcoder.expectedoutputaggregator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.httpClient
import org.golfcoder.mainDatabase

class SevenRebuxAggregator : ExpectedOutputAggregator {
    override suspend fun load(year: Int, day: Int): ExpectedOutputAggregator.AggregatorResult {
        if (year != 2023) {
            return ExpectedOutputAggregator.AggregatorResult.Failure.YearNotInSource
        }

        val source = ExpectedOutput.Source.SEVEN_REBUX

        val dayString = "Day${String.format("%02d", day)}"

        val input = httpClient.get(
            "https://raw.githubusercontent.com/7rebux/advent-of-code-2023/main/src/main/resources/$dayString.txt"
        ).takeIf { it.status == HttpStatusCode.OK }?.bodyAsText() ?: run {
            return ExpectedOutputAggregator.AggregatorResult.Failure.NotYetAvailable
        }

        val testcaseCode = httpClient.get(
            "https://raw.githubusercontent.com/7rebux/advent-of-code-2023/main/src/test/kotlin/DaysTest.kt"
        ).bodyAsText()

        val part1output = testcaseCode
            .substringAfter("Answer($dayString, ")
            .substringBefore(", ")
            .trim()
            .toLongOrNull()

        val part2output = testcaseCode
            .substringAfter("Answer($dayString, ")
            .substringAfter(", ")
            .substringBefore(")")
            .trim()
            .toLongOrNull()

        if (part1output == null || part2output == null) {
            return ExpectedOutputAggregator.AggregatorResult.Failure.DifferentFormat
        }

        mapOf(1 to part1output, 2 to part2output).forEach { (part, output) ->
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
        }

        return ExpectedOutputAggregator.AggregatorResult.Success
    }
}