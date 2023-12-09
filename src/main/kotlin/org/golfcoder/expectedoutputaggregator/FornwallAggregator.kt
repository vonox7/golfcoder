package org.golfcoder.expectedoutputaggregator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.httpClient
import org.golfcoder.mainDatabase

class FornwallAggregator : ExpectedOutputAggregator {
    override suspend fun load(year: Int, day: Int) {
        val source = ExpectedOutput.Source.FORNWALL

        val input = httpClient.get(
            "https://raw.githubusercontent.com/fornwall/advent-of-code/main/crates/core/src/" +
                    "year$year/day${String.format("%02d", day)}_input.txt"
        ).bodyAsText()

        if (input.isEmpty()) {
            println("No input (yet) for day $day (year $year) from $source")
            return
        }

        (1..2).forEach { part ->
            val output = httpClient.post("https://advent.fly.dev/solve/$year/$day/$part") {
                setBody(input)
            }.bodyAsText().toLong()

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

            println("Added expected output for day $day part $part from $source")
        }
    }
}