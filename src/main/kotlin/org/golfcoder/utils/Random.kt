package org.golfcoder.utils

import java.util.*

private val random = Random()

fun randomId(): String {
    val bytes = ByteArray(size = 16) // 16 * 8 = 256 -> full entropy for sha256
    random.nextBytes(bytes)
    return bytes.sha256().take(32)
}