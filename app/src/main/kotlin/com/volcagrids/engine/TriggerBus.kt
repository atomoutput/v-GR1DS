package com.volcagrids.engine

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Trigger Event - Represents a drum trigger with timing information
 */
data class TriggerEvent(
    val channel: Int,      // 0-5 for 6 parts
    val intensity: Float,  // 0.0-1.0 velocity
    val timestamp: Long    // Nanoseconds for precise timing
)

/**
 * TriggerBus - Thread-safe event bus for drum triggers
 * 
 * Allows sequencer thread to post triggers without blocking,
 * and UI thread to consume them for visual feedback.
 */
class TriggerBus {
    private val queue = ConcurrentLinkedQueue<TriggerEvent>()
    
    /**
     * Post a trigger event (called from sequencer thread)
     * Non-blocking, thread-safe
     */
    fun post(channel: Int, intensity: Float) {
        queue.offer(TriggerEvent(channel, intensity, System.nanoTime()))
    }
    
    /**
     * Process all pending events (called from UI thread)
     * Returns list of events since last call
     */
    fun process(): List<TriggerEvent> {
        val events = mutableListOf<TriggerEvent>()
        var event = queue.poll()
        while (event != null) {
            events.add(event)
            event = queue.poll()
        }
        return events
    }
    
    /**
     * Clear all pending events
     */
    fun clear() {
        queue.clear()
    }
    
    /**
     * Get current queue size (for debugging)
     */
    fun size(): Int = queue.size
}
