package org.golfcoder

import org.golfcoder.utils.relativeToNow
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class UnitTests {
    @Test
    fun testDateRelativeToNow() {
        // Set timezone to Berlin to avoid test failures due to winter/summertime
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))

        val now = Date.from(Instant.parse("2023-12-31T10:00:00Z"))
        assertEquals("2 seconds ago", Date.from(Instant.parse("2023-12-31T09:59:58Z")).relativeToNow(now))
        assertEquals("119 seconds ago", Date.from(Instant.parse("2023-12-31T09:58:01Z")).relativeToNow(now))
        assertEquals("2 minutes ago", Date.from(Instant.parse("2023-12-31T09:58:00Z")).relativeToNow(now))
        assertEquals("119 minutes ago", Date.from(Instant.parse("2023-12-31T08:01:00Z")).relativeToNow(now))
        assertEquals("2 hours ago", Date.from(Instant.parse("2023-12-31T08:00:00Z")).relativeToNow(now))
        assertEquals("47 hours ago", Date.from(Instant.parse("2023-12-29T11:00:00Z")).relativeToNow(now))
        assertEquals("2 days ago", Date.from(Instant.parse("2023-12-29T10:00:00Z")).relativeToNow(now))
        // +1h due to winter/summertime
        assertEquals("59 days ago", Date.from(Instant.parse("2023-11-01T11:00:00Z")).relativeToNow(now))
        assertEquals("2 months ago", Date.from(Instant.parse("2023-10-31T10:00:00Z")).relativeToNow(now))
        assertEquals("23 months ago", Date.from(Instant.parse("2022-01-30T10:00:00Z")).relativeToNow(now))
        assertEquals("2 years ago", Date.from(Instant.parse("2021-12-31T10:00:00Z")).relativeToNow(now))
        assertEquals("20 years ago", Date.from(Instant.parse("2003-12-31T10:00:00Z")).relativeToNow(now))
    }
}
