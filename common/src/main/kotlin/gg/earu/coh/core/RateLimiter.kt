package gg.earu.coh.core

import java.util.UUID

/** Per-player minimum-interval gate, measured in server ticks. Violations are dropped silently. */
class RateLimiter {
    private val nextAllowed = HashMap<UUID, Long>()

    fun tryAcquire(id: UUID, nowTick: Long, minIntervalTicks: Long): Boolean {
        val next = nextAllowed[id] ?: Long.MIN_VALUE
        if (nowTick < next) return false
        nextAllowed[id] = nowTick + minIntervalTicks
        return true
    }

    fun forget(id: UUID) {
        nextAllowed.remove(id)
    }
}
