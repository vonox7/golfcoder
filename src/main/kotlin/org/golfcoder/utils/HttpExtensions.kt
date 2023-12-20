package org.golfcoder.utils

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.util.reflect.*


suspend inline fun <reified T> HttpResponse.bodyOrPrintException(): T {
    return try {
        body<T>()
    } catch (e: Exception) {
        val exception = Exception("Failed to parse response from ${request.url}: ${bodyAsText()}", e)
        exception.printStackTrace()
        throw exception
    }
}

suspend fun <T> HttpResponse.bodyOrPrintException(typeInfo: TypeInfo): T {
    return try {
        body(typeInfo)
    } catch (e: Exception) {
        val exception = Exception("Failed to parse response from ${request.url}: ${bodyAsText()}", e)
        exception.printStackTrace()
        throw exception
    }
}
