package org.golfcoder.database.pgpayloads

import org.golfcoder.database.ExpectedOutput
import org.jetbrains.exposed.v1.core.Table

object ExpectedOutputTable : Table("expectedOutput") {
    val year = integer("year")
    val day = integer("day")
    val part = integer("part")
    val input = text("input")
    val output = text("output")
    val sourceEnum = enumerationByName("sourceEnum", length = 20, ExpectedOutput.Source::class)

    override val primaryKey = PrimaryKey(year, day, part, sourceEnum)
}