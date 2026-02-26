package com.volcagrids.midi

/**
 * MIDI CC Throttler - Prevents MIDI buffer overflow by rate-limiting CC messages.
 * 
 * The Volca Drum MIDI buffer can get choked during high-speed modulation.
 * This throttler ensures max CC messages per second stays within safe limits.
 */
class MIDIThrottler(
    private val maxCCsPerSecond: Int = 100,
    private val clock: SystemClock = DefaultSystemClock()
) {
    private data class CCMessage(
        val channel: Int,
        val cc: Int,
        val value: Int,
        val timestamp: Long
    )

    private val messageQueue = ArrayDeque<CCMessage>()
    private var lastProcessTime = 0L
    
    /**
     * Queue a CC message for throttled delivery.
     * Returns true if message was accepted, false if queue is full.
     */
    fun queueCC(channel: Int, cc: Int, value: Int): Boolean {
        val now = clock.currentTimeMillis()
        
        // Clean old messages from queue (older than 1 second)
        while (messageQueue.isNotEmpty() && now - messageQueue.first().timestamp > 1000) {
            messageQueue.removeFirst()
        }
        
        // Check if we're at capacity
        if (messageQueue.size >= maxCCsPerSecond) {
            return false // Drop message to prevent overflow
        }
        
        messageQueue.addLast(CCMessage(channel, cc, value, now))
        return true
    }

    /**
     * Process queued messages and return those ready to send.
     * Call this regularly (e.g., every 10-20ms) from your sequencer thread.
     */
    fun processQueue(): List<CCMessageToSend> {
        val now = clock.currentTimeMillis()
        val messagesToSend = mutableListOf<CCMessageToSend>()
        
        // Calculate how many messages we can send this frame
        // Spread messages evenly over the second
        val elapsedMs = now - lastProcessTime
        if (elapsedMs < 10) return emptyList() // Don't process too frequently
        
        val messagesPerFrame = (maxCCsPerSecond * elapsedMs) / 1000
        
        repeat(messagesPerFrame.toInt().coerceAtLeast(1)) {
            if (messageQueue.isNotEmpty()) {
                val msg = messageQueue.removeFirst()
                messagesToSend.add(
                    CCMessageToSend(msg.channel, msg.cc, msg.value)
                )
            }
        }
        
        lastProcessTime = now
        return messagesToSend
    }

    /**
     * Clear all queued messages (e.g., when stopping playback)
     */
    fun clear() {
        messageQueue.clear()
        lastProcessTime = clock.currentTimeMillis()
    }

    /**
     * Get current queue depth for diagnostics
     */
    fun getQueueSize(): Int = messageQueue.size

    /**
     * Get estimated messages per second being sent
     */
    fun getMessagesPerSecond(): Int {
        val now = clock.currentTimeMillis()
        val recentMessages = messageQueue.count { now - it.timestamp < 1000 }
        return recentMessages
    }
}

data class CCMessageToSend(
    val channel: Int,
    val cc: Int,
    val value: Int
)

/**
 * System clock interface for testability
 */
interface SystemClock {
    fun currentTimeMillis(): Long
}

class DefaultSystemClock : SystemClock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
