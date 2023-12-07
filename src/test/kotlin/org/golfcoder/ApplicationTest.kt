package org.golfcoder

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {}
        client.get("/2023").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
