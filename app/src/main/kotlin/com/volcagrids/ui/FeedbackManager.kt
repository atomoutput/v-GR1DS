package com.volcagrids.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf

/**
 * FeedbackManager - Manages visual feedback for drum triggers
 *
 * Optimized for performance:
 * - Uses mutableStateListOf for efficient Compose observation
 * - onTrigger() posts state updates to the Main thread via Handler
 * - updateDecay() called from UI thread at 60fps via Compose LaunchedEffect
 * - NO synchronization needed - all access is on main thread
 */
object FeedbackManager {
    // 6 Channels (0-2: Deck A, 3-5: Deck B)
    // Efficient mutable list for Compose
    val triggerIntensities = mutableStateListOf(0f, 0f, 0f, 0f, 0f, 0f)

    // Decay timers stored as nanosecond timestamps
    // Simple array - no synchronization needed (main thread only)
    private val decayTimers = LongArray(6) { 0L }

    // Decay duration: 50ms = 50,000,000 nanoseconds
    private const val DECAY_DURATION_NS = 50_000_000L

    // UI Thread Dispatcher
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Trigger visual feedback (called from sequencer thread)
     * Posts to Main Looper - all state changes happen on UI thread
     */
    fun onTrigger(channel: Int, intensity: Float) {
        if (channel !in 0..5) return

        // Post to main thread - no synchronization needed
        mainHandler.post {
            triggerIntensities[channel] = intensity
            decayTimers[channel] = System.nanoTime() + DECAY_DURATION_NS
        }
    }

    /**
     * Update decay state (called from UI thread at 60fps)
     * All operations on main thread - no locks needed
     */
    fun updateDecay() {
        val now = System.nanoTime()

        // Check each channel for decay - direct array access, no locks
        for (channel in 0..5) {
            val decayTime = decayTimers[channel]
            if (decayTime > 0 && now >= decayTime) {
                triggerIntensities[channel] = 0f
                decayTimers[channel] = 0L
            }
        }
    }

    /**
     * Clear all active triggers (called when stopping playback)
     */
    fun clear() {
        mainHandler.post {
            for (i in 0..5) {
                triggerIntensities[i] = 0f
                decayTimers[i] = 0L
            }
        }
    }
}
