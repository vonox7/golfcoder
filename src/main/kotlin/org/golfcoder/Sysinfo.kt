package org.golfcoder

import java.util.*

object Sysinfo {
    val container = System.getenv("CONTAINER") ?: "local"
    val release =
        System.getenv("CONTAINER_VERSION")
            ?: "local${Date().time}" // Version of the container started, usually the Git commit SHA.

    val isLocal get() = container == "local"
}