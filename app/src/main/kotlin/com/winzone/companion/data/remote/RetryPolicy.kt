package com.winzone.companion.data.remote

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.random.Random

object RetryPolicy {

    suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 6,
        initialDelayMs: Long = 1000L,
        maxDelayMs: Long = 30000L,
        factor: Double = 2.0,
        jitterMs: Long = 250L,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelayMs

        for (attempt in 1..maxAttempts) {
            try {
                val result = block()
                return Result.success(result)
            } catch (t: Throwable) {
                val isRetryable = isRetryableException(t)
                Timber.w(t, "Attempt $attempt/$maxAttempts failed (retryable=$isRetryable)")

                if (!isRetryable || attempt == maxAttempts) {
                    return Result.failure(t)
                }

                val jitter = Random.nextLong(0, jitterMs + 1)
                val delayTime = (currentDelay + jitter).coerceAtMost(maxDelayMs)
                delay(delayTime)

                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
        return Result.failure(IllegalStateException("Exceeded max retry attempts"))
    }

    fun isRetryableException(t: Throwable): Boolean {
        return when (t) {
            is IOException,
            is SocketTimeoutException,
            is UnknownHostException -> true
            is ServerResponseException -> true // HTTP 5xx
            is ClientRequestException -> {
                val code = t.response.status.value
                code == 429 // Too Many Requests is retryable
            }
            else -> false
        }
    }
}
