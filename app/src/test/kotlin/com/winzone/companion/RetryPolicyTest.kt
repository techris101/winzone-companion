package com.winzone.companion

import com.winzone.companion.data.remote.RetryPolicy
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.SocketTimeoutException

class RetryPolicyTest {

    @Test
    fun `isRetryableException identifies network errors and 5xx correctly`() {
        assertTrue(RetryPolicy.isRetryableException(IOException("Network error")))
        assertTrue(RetryPolicy.isRetryableException(SocketTimeoutException("Timeout")))

        val response500 = mockk<HttpResponse>()
        every { response500.status } returns HttpStatusCode.InternalServerError
        val serverException = ServerResponseException(response500, "500 Internal Server Error")
        assertTrue(RetryPolicy.isRetryableException(serverException))

        val response429 = mockk<HttpResponse>()
        every { response429.status } returns HttpStatusCode.TooManyRequests
        val rateLimitException = ClientRequestException(response429, "429 Rate Limited")
        assertTrue(RetryPolicy.isRetryableException(rateLimitException))

        val response400 = mockk<HttpResponse>()
        every { response400.status } returns HttpStatusCode.BadRequest
        val badRequestException = ClientRequestException(response400, "400 Bad Request")
        assertFalse(RetryPolicy.isRetryableException(badRequestException))
    }

    @Test
    fun `retryWithBackoff succeeds on first attempt`() = runTest {
        var attempts = 0
        val result = RetryPolicy.retryWithBackoff(maxAttempts = 3, initialDelayMs = 10) {
            attempts++
            "success"
        }
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(1, attempts)
    }

    @Test
    fun `retryWithBackoff retries transient failures and eventually succeeds`() = runTest {
        var attempts = 0
        val result = RetryPolicy.retryWithBackoff(maxAttempts = 4, initialDelayMs = 10) {
            attempts++
            if (attempts < 3) {
                throw IOException("Temporary failure")
            }
            "recovered"
        }
        assertTrue(result.isSuccess)
        assertEquals("recovered", result.getOrNull())
        assertEquals(3, attempts)
    }

    @Test
    fun `retryWithBackoff fails immediately on non-retryable exception`() = runTest {
        var attempts = 0
        val result = RetryPolicy.retryWithBackoff(maxAttempts = 4, initialDelayMs = 10) {
            attempts++
            throw IllegalArgumentException("Non retryable")
        }
        assertTrue(result.isFailure)
        assertEquals(1, attempts)
    }
}
