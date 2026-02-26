package com.volcagrids.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf

/**
 * FeedbackManager - Manages visual feedback for drum triggers
 * 
 * Thread-safe design:
 * - onTrigger() delegates state updates to the Main thread via Handler
 * - updateDecay() called from UI thread at 60fps via Compose LaunchedEffect
 */
object FeedbackManager {
    // 6 Channels (0-2: Deck A, 3-5: Deck B)
    // Stores flash intensity (0.0f - 1.0f)
    val triggerIntensities = mutableStateListOf(0f, 0f, 0f, 0f, 0f, 0f)
    
    // Decay timers stored as nanosecond timestamps
    private val decayTimers = mutableMapOf<Int, Long>()
    
    // Decay duration: 50ms = 50,000,000 nanoseconds
    private const val DECAY_DURATION_NS = 50_000_000L
    
    // UI Thread Dispatcher
    private val mainHandler = Handler(Looper.getMainLooper())
    
    /**
     * Trigger visual feedback (called from sequencer thread)
     * Safe across threads as it posts to the Main Looper
     */
    fun onTrigger(channel: Int, intensity: Float) {
        if (channel !in 0..5) return
        
        mainHandler.post {
            triggerIntensities[channel] = intensity
            decayTimers[channel] = System.nanoTime() + DECAY_DURATION_NS
        }
    }
    
    /**
     * Update decay state (called from UI thread at 60fps)
     * Checks timers and fades out expired triggers
     */
    fun updateDecay() {
        val now = System.nanoTime()
        
        // Check each channel for decay
        for (channel in 0..5) {
            val decayTime = decayTimers[channel]
            if (decayTime != null && now >= decayTime) {
                triggerIntensities[channel] = 0f
                decayTimers.remove(channel)
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
            }
            decayTimers.clear()
        }
    }
    
    /**
     * Get number of active decays (for debugging)
     */
    fun activeDecayCount(): Int = decayTimers.size
}
