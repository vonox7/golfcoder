package org.golfcoder.utils

import java.util.*

fun Date.relativeToNow(now: Date = Date()): String {
    var difference = (now.time - time) / 1000
    val unitMap = mapOf("second" to 60, "minute" to 60, "hour" to 24, "day" to 30, "month" to 12, "year" to 10000)
    for ((unit, divider) in unitMap) {
        if (difference < divider * 2) { // show e.g. 2h to 48h
            return "$difference $unit${if (difference > 1) "s" else ""} ago"
        }
        difference /= divider
    }
    throw IllegalStateException("Should not happen, difference: $difference")
}
