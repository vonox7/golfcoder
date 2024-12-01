package org.golfcoder.expectedoutputaggregator

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.golfcoder.database.ExpectedOutput
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator.AggregatorResult.Failure
import org.golfcoder.expectedoutputaggregator.ExpectedOutputAggregator.AggregatorResult.Success
import org.golfcoder.httpClient
import org.golfcoder.mainDatabase

class KateAggregator : ExpectedOutputAggregator {
  override suspend fun load(year: Int, day: Int): ExpectedOutputAggregator.AggregatorResult {
    val source = ExpectedOutput.Source.KATE

    val file = httpClient.get(
      "https://raw.githubusercontent.com/katemihalikova/advent-of-code/refs/heads/latest/$year/" +
          "${String.format("%02d", day)}.js"
    ).takeIf { it.status == HttpStatusCode.OK }?.bodyAsText() ?: run {
      return Failure.NotYetAvailable
    }

    if ("// == ASSERTS ==\n" !in file) {
      return Failure.DifferentFormat
    }
    val assertions = file.substringAfter("// == ASSERTS ==\n")

    (1..2).forEach { part ->
      val match = Regex(
        """.*console\.assert\(part$part\(\s*[`"]([^`"]+)[`"]\s*\) === ([0-9]+)\).*""",
        RegexOption.DOT_MATCHES_ALL
      )
        .matchEntire(assertions)
        ?.groupValues

      if (match == null) {
        return Failure.DifferentFormat
      }
      val input = match[1].takeIf { it.isNotBlank() }
      val output = match[2].toLongOrNull()

      if (input == null || output == null) {
        return Failure.DifferentFormat
      }

      if (input.length > 2000) {
        return Failure.TooLongInput
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