package org.golfcoder

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import org.golfcoder.database.ExpectedOutput
import java.util.*
import kotlin.time.Duration.Companion.minutes

object ExpectedOutputAggregator {
    suspend fun loadAll() {
        val year = 2023

        (1..24).forEach { day ->
            load(year, day)
        }
    }

    private suspend fun load(year: Int, day: Int) {
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
                    this.source = ExpectedOutput.Source.FORNWALL
                    this.input = input
                    this.output = output
                },
                upsert = true
            )

            println("Added expected output for day $day part $part from $source")
        }
    }

    // Load every 10 minutes the past, current and next day (to avoid timezone issues and to give Fornwall time to solve the problem)
    suspend fun loadContinuously() {
        while (true) {
            try {
                val now = Calendar.getInstance()
                (now.get(Calendar.DAY_OF_MONTH) - 1..now.get(Calendar.DAY_OF_MONTH) + 1)
                    .intersect(1..24)
                    .forEach { day ->
                        load(now.get(Calendar.YEAR), day)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            delay(10.minutes)
        }
    }
}