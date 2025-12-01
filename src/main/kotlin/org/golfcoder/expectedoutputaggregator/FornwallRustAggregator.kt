package org.golfcoder.expectedoutputaggregator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator.AggregatorResult.Failure
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator.AggregatorResult.Success
import org.golfcoder.httpClient
import org.golfcoder.mainDatabase

class FornwallRustAggregator : ExpectedOutputAggregator {
    override suspend fun load(year: Int, day: Int): ExpectedOutputAggregator.AggregatorResult {
        val source = ExpectedOutput.Source.FORNWALL_RUST

        if (year < 2022) {
            return Failure.YearNotInSource // TODO support inlined testInput
        }

        val file = httpClient.get(
            "https://raw.githubusercontent.com/fornwall/advent-of-code/refs/heads/main/crates/core/src/" +
                    "year$year/day${String.format("%02d", day)}.rs"
        ).takeIf { it.status == HttpStatusCode.OK }?.bodyAsText() ?: run {
            return Failure.NotYetAvailable
        }

        if ("#[test]" !in file) {
            return Failure.DifferentFormat
        }

        var assertions = file.substringAfterLast("#[test]")

        // Ensure we take only the first "let test_input" in assertions
        val firstTestInputDeclaration = assertions.indexOf("let test_input = ")
        val secondTestInputDeclaration =
            assertions.indexOf("let test_input = ", startIndex = firstTestInputDeclaration + 1)
        if (secondTestInputDeclaration != -1) {
            assertions = assertions.substring(0, secondTestInputDeclaration)
        }

        (1..2).forEach { part ->
            val partString = if (part == 1) "one" else "two"
            val input = Regex(
                """.*let test_input = "([^"]+)";.*""",
                RegexOption.DOT_MATCHES_ALL
            )
                .matchEntire(assertions)
                ?.groupValues
                ?.get(1)
                ?.takeIf { it.isNotBlank() }

            val output = Regex(
                """.*test_part_$partString(_no_allocations)?!\(test_input => ([0-9_]+)\);.*""",
                RegexOption.DOT_MATCHES_ALL
            )
                .matchEntire(assertions)
                ?.groupValues
                ?.get(2)
                ?.replace("_", "")
                ?.toLongOrNull()

            if (input == null || output == null) {
                return Failure.DifferentFormat
            }

            if (input.length > 20000) {
                return Failure.TooLongInput
            } else if (input.length < 10) {
                return Failure.NotYetAvailable
            }

            mainDatabase.getSuspendingCollection<ExpectedOutput>().insertOne(
                ExpectedOutput().apply {
                    _id = generateId(year.toString(), day.toString(), part.toString(), source.name)
                    this.year = year
                    this.day = day
                    this.part = part
                    this.source = source
                    this.input = input
                    this.output = output.toString()
                },
                upsert = true
            )
        }
        return Success
    }
}