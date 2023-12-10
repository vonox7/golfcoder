package org.golfcoder.expectedoutputaggregator

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import org.golfcoder.Sysinfo
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.endpoints.api.UploadSolutionApi
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

    private suspend fun loadAll() {
        val year = 2023

        UploadSolutionApi.DAYS_RANGE.forEach { day ->
            load(year, day)
        }
    }

    private suspend fun load(year: Int, day: Int) {
        ExpectedOutput.Source.entries.forEach { source ->
            try {
                source.aggregator.load(year, day)
            } catch (exception: Exception) {
                println("Could not load expected output for day $day from $source: $exception")
            }
        }
    }

    // Load every 10 minutes the past, current and next day (to avoid timezone issues and to give Fornwall time to solve the problem)
    suspend fun loadContinuously() {
        while (true) {
            try {
                val now = Calendar.getInstance()
                (now.get(Calendar.DAY_OF_MONTH) - 1..now.get(Calendar.DAY_OF_MONTH) + 1)
                    .intersect(UploadSolutionApi.DAYS_RANGE)
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