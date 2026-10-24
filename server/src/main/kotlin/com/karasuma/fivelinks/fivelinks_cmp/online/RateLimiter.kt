package com.karasuma.fivelinks.fivelinks_cmp.online

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe sliding window rate limiter.
 */
class RateLimiter(
    val limit: Int,
    val windowMillis: Long = 60_000L,
) {
    private val buckets = ConcurrentHashMap<String, WindowBucket>()

    fun tryAcquire(key: String): Boolean {
        if (limit <= 0) return true
        val now = System.currentTimeMillis()
        val bucket = buckets.compute(key) { _, current ->
            if (current == null || now - current.windowStart >= windowMillis) {
                WindowBucket(windowStart = now, count = AtomicInteger(1))
            } else {
                current.count.incrementAndGet()
                current
            }
        }!!
        return bucket.count.get() <= limit
    }

    fun cleanup() {
        val now = System.currentTimeMillis()
        buckets.entries.removeIf { now - it.value.windowStart >= windowMillis * 2 }
    }

    private data class WindowBucket(
        val windowStart: Long,
        val count: AtomicInteger,
    )
}

