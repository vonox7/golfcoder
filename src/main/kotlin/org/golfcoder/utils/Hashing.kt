package org.golfcoder.utils

import java.security.MessageDigest

fun ByteArray.sha256(): String {
    val md = MessageDigest.getInstance("SHA-256")
    val hex = md.digest(this)
    return hex.joinToString("") { "%02x".format(it) }
}