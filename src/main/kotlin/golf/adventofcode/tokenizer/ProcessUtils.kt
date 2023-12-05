package golf.adventofcode.tokenizer

import java.io.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


fun Array<String>.runCommand(
    workingDir: File = File("."),
    waitMilliSeconds: Long? = 1000,
    printOutput: Boolean = true
): String {
    val process = ProcessBuilder(*this)
        .directory(workingDir)
        .redirectErrorStream(true) // Get stderr too
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    // Create a Thread that waits for the timeout.
    // If the process was already terminated before the timeout do nothing, else destroy the process.
    val waitThread: Thread? = if (waitMilliSeconds != null) thread(name = "runCommand-${this.first()}") {
        try {
            if (!process.waitFor(waitMilliSeconds, TimeUnit.MILLISECONDS)) {
                process.destroy()
            }
        } catch (e: Exception) {
            println(e)
        }
    } else null

    val reader = BufferedReader(InputStreamReader(process.inputStream))

    val stringBuilder = StringBuilder()
    reader.use {
        try {
            reader.lines().forEachOrdered { line ->
                stringBuilder.appendLine(line)
                if (printOutput) {
                    println(line)
                }
            }
        } catch (e: UncheckedIOException) {
            if (e.cause is IOException && e.cause?.message == "Stream closed" && !process.isAlive) {
                // Ignore error. Happens when the process timed out, so we destroyed the process's thread in the waitThread
            } else {
                throw e
            }
        }
    }

    process.waitFor() // Wait until the process exited successfully or was destroyed by the waitThread
    waitThread?.join()

    if (printOutput) {
        println()
    }

    return stringBuilder.toString()
}