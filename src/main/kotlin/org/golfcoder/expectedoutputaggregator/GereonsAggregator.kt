package org.golfcoder.expectedoutputaggregator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.httpClient
import org.golfcoder.mainDatabase

class GereonsAggregator : ExpectedOutputAggregator {
    override suspend fun load(year: Int, day: Int) {
        if (year != 2023) return

        val source = ExpectedOutput.Source.GEREONS

        val testcaseCode = httpClient.get(
            "https://raw.githubusercontent.com/gereons/AoC2023/main/Tests/Day${String.format("%02d", day)}Tests.swift"
        ).bodyAsText()

        val testInput = testcaseCode
            .substringAfter("let testInput = \"\"\"")
            .substringBefore("\"\"\"")
            .trim()
            .takeIf { it.isNotEmpty() }

        if (testInput == null) {
            println("No input (yet) for day $day (year $year) from $source (or different test format)")
            return
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
                    println("Unknown input format for day $day (year $year) from $source")
                    return
                }

            mainDatabase.getSuspendingCollection<ExpectedOutput>().insertOne(
                ExpectedOutput().apply {
                    _id = generateId(year.toString(), day.toString(), part.toString(), source.name)
                    this.year = year
                    this.day = day
                    this.part = part
                    this.source = source
                    this.input = testcaseCode
                    this.output = output
                },
                upsert = true
            )

            println("Added expected output for day $day part $part from $source")
        }
    }
}