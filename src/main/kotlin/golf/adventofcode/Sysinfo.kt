package golf.adventofcode

object Sysinfo {
    val container = System.getenv("CONTAINER") ?: "local"
    val release =
        System.getenv("CONTAINER_VERSION") ?: "local" // Version of the container started, usually the Git commit SHA.

    val isLocal get() = container == "local"
    val isWeb get() = container.startsWith("web-")
    val isPrimaryWeb get() = container == "web-1"
    val isWorker get() = container.startsWith("worker-")
    val isOneOff get() = container.startsWith("one-off-")
}