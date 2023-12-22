package org.golfcoder.utils

import java.util.*

val Date.relativeToNow: String
    get() {
        var difference = (Date().time - time) / 1000
        val unitMap = mapOf("second" to 60, "minute" to 60, "hour" to 24, "day" to 7, "month" to 30, "year" to 10000)
        for ((unit, divider) in unitMap) {
            if (difference < divider * 2) { // show e.g. 2h to 48h
                return "$difference $unit${if (difference > 1) "s" else ""} ago"
            }
            difference /= divider
        }
        throw IllegalStateException("Should not happen, difference: $difference")
    }
