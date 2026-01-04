package org.golfcoder.database.pgpayloads

import org.golfcoder.expectedoutputaggregator.*
import org.jetbrains.exposed.v1.core.Table

object ExpectedOutputTable : Table("expected_output") {
    val year = integer("year")
    val day = integer("day")
    val part = integer("part")
    val input = text("input")
    val output = text("output")
    val sourceEnum = enumerationByName("sourceEnum", length = 20, Source::class)

    override val primaryKey = PrimaryKey(year, day, part, sourceEnum)
}

enum class Source {
    FORNWALL,
    FORNWALL_RUST,
    SEVEN_REBUX,
    KATE,
    SHAHATA,
    SIM
    ;

    val aggregator: ExpectedOutputAggregator
        get() = when (this) {
            FORNWALL -> FornwallAggregator()
            FORNWALL_RUST -> FornwallRustAggregator()
            SEVEN_REBUX -> SevenRebuxAggregator()
            KATE -> KateAggregator()
            SHAHATA -> ShahataAggregator()
            SIM -> SimAggregator()
        }
}