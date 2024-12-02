package org.golfcoder.expectedoutputaggregator

import io.sentry.Sentry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import org.golfcoder.Sysinfo
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.endpoints.api.UploadSolutionApi
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator.AggregatorResult.Failure.YearNotInSource
import org.golfcoder.mainDatabase
import java.util.*
import kotlin.time.Duration.Companion.minutes

object ExpectedOutputAggregatorLoader {
    // Locally we want to load the expected output only once to have a less noisy local development experience.
    // On production each time the server starts.
    suspend fun loadOnStartup() {
        if (!Sysinfo.isLocal ||
            mainDatabase.getSuspendingCollection<ExpectedOutput>().distinct(ExpectedOutput::source).count() !=
            ExpectedOutput.Source.entries.size
        ) {
            loadAll()
            loadContinuously()
        }
    }

    suspend fun loadAll() {
        UploadSolutionApi.YEARS_RANGE.forEach { year ->
            val results = UploadSolutionApi.DAYS_RANGE.map { day ->
                day to load(year, day)
            }
            println("Loaded expected output:" + results.joinToString("") { (day, results) ->
                "\n$year/$day: " + results.filter { it.value !is YearNotInSource }.toList()
                    .joinToString { (source, result) -> "$result(${source.name})" }
            })
        }
        println("Loaded expected output for all years and days")
    }

    private suspend fun load(
        year: Int,
        day: Int,
    ): Map<ExpectedOutput.Source, ExpectedOutputAggregator.AggregatorResult> {
        return ExpectedOutput.Source.entries.associateWith { source ->
            try {
                source.aggregator.load(year, day)
            } catch (exception: Exception) {
                Sentry.captureException(exception)
                println("Could not load expected output for $year/$day from $source: $exception")
                ExpectedOutputAggregator.AggregatorResult.Failure.UnknownError(exception.toString())
            }
        }
    }

    // Load every 10 minutes the past, current and next day (to avoid timezone issues and to give Fornwall time to solve the problem)
    suspend fun loadContinuously() {
        while (true) {
            try {
                val now = Calendar.getInstance()
                if (now.get(Calendar.MONTH) == Calendar.DECEMBER) {
                    (now.get(Calendar.DAY_OF_MONTH) - 1..now.get(Calendar.DAY_OF_MONTH) + 1)
                        .intersect(UploadSolutionApi.DAYS_RANGE)
                        .map { day ->
                            val results = load(now.get(Calendar.YEAR), day)
                            if (results.none { it.value is ExpectedOutputAggregator.AggregatorResult.Success }) {
                                println("Could not load expected output for day $day from any source")
                            }
                        }
                }
            } catch (e: Exception) {
                Sentry.captureException(e)
            }

            delay(10.minutes)
        }
    }
}