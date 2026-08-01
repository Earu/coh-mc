package gg.earu.coh

import gg.earu.coh.core.RateLimiter
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimiterTest {
    @Test
    fun `enforces minimum interval`() {
        val limiter = RateLimiter()
        val id = UUID.randomUUID()
        assertTrue(limiter.tryAcquire(id, 0, 6))
        assertFalse(limiter.tryAcquire(id, 5, 6))
        assertTrue(limiter.tryAcquire(id, 6, 6))
    }

    @Test
    fun `players are limited independently`() {
        val limiter = RateLimiter()
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        assertTrue(limiter.tryAcquire(a, 0, 6))
        assertTrue(limiter.tryAcquire(b, 0, 6))
    }

    @Test
    fun `forget resets the gate`() {
        val limiter = RateLimiter()
        val id = UUID.randomUUID()
        assertTrue(limiter.tryAcquire(id, 0, 6))
        limiter.forget(id)
        assertTrue(limiter.tryAcquire(id, 1, 6))
    }
}
