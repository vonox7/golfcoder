package golf.adventofcode

object Sysinfo {
    val container = System.getenv("CONTAINER") ?: "local"

    val isLocal get() = container == "local"
    val isWeb get() = container.startsWith("web-")
    val isWorker get() = container.startsWith("worker-")
}