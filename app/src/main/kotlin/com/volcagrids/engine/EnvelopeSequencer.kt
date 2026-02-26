package com.volcagrids.engine

import com.volcagrids.engine.Resources
import kotlin.math.abs

/**
 * Envelope Sequencer - Generates continuous parameter modulation envelopes
 * using the same 5x5 topographic map as the drum sequencer.
 * 
 * Instead of binary triggers (hit/no-hit), outputs continuous values (0-127)
 * that can be used to modulate Volca Drum parameters.
 */

enum class EnvelopeShape {
    SMOOTH,       // Linear interpolation between steps
    EXPONENTIAL,  // Curve for filter-like parameters
    STEPPED,      // Sample & hold - value changes only on step
    RANDOM        // Random S&H - new random value each step
}

class EnvelopeSequencer {
    companion object {
        const val STEPS_PER_PATTERN = 32
        const val PULSES_PER_STEP = 3  // 24 PPQN
        
        // Same drum map as PatternGenerator
        private val drumMap = arrayOf(
            intArrayOf(10, 8, 0, 9, 11),
            intArrayOf(15, 7, 13, 12, 6),
            intArrayOf(18, 14, 4, 5, 3),
            intArrayOf(23, 16, 21, 1, 2),
            intArrayOf(24, 19, 17, 20, 22)
        )
        
        private fun u8Mix(a: Int, b: Int, mix: Int): Int {
            return (a * (255 - mix) + b * mix) shr 8
        }
    }
    
    var x = 127  // Topographic X position (0-255)
    var y = 127  // Topographic Y position (0-255)
    var shape: EnvelopeShape = EnvelopeShape.SMOOTH
    var range = 127  // Modulation depth (0-127)
    var offset = 0   // Base value (0-127)
    var enabled = false
    
    private var step = 0
    private var pulse = 0
    private var currentValue = 64
    private var smoothedValue = 64f
    private var lastValue = 64
    
    /**
     * Read raw value from topographic map at current step
     */
    private fun readTopographicValue(step: Int): Int {
        val i = (x shr 6).coerceAtMost(3)
        val j = (y shr 6).coerceAtMost(3)
        val xi = (x shl 2) and 0xff
        val yi = (y shl 2) and 0xff
        
        val aMap = Resources.nodeTable[drumMap[i][j]]
        val bMap = Resources.nodeTable[drumMap[i + 1][j]]
        val cMap = Resources.nodeTable[drumMap[i][j + 1]]
        val dMap = Resources.nodeTable[drumMap[i + 1][j + 1]]
        
        val offset = step.coerceIn(0, 95)  // Node data is 96 bytes
        val a = aMap[offset].toInt()
        val b = bMap[offset].toInt()
        val c = cMap[offset].toInt()
        val d = dMap[offset].toInt()
        
        return u8Mix(u8Mix(a, b, xi), u8Mix(c, d, xi), yi)
    }
    
    /**
     * Apply envelope shape to value
     */
    private fun applyShape(rawValue: Int, step: Int): Int {
        return when (shape) {
            EnvelopeShape.SMOOTH -> rawValue
            EnvelopeShape.EXPONENTIAL -> {
                // Exponential curve for filter-like parameters
                val normalized = rawValue / 255f
                val exp = normalized * normalized * normalized  // Simple cubic curve
                (exp * 255).toInt()
            }
            EnvelopeShape.STEPPED -> {
                // Hold value for entire step
                if (pulse == 0) rawValue else lastValue
            }
            EnvelopeShape.RANDOM -> {
                // Random value on step change
                if (pulse == 0) (kotlin.random.Random.nextInt(256)) else lastValue
            }
        }
    }
    
    /**
     * Apply range and offset to scaled value
     */
    private fun applyRangeAndOffset(value: Int): Int {
        val scaled = (value * range) shr 8  // Scale by range
        return (offset + scaled).coerceIn(0, 127)
    }
    
    /**
     * Advance sequencer by one pulse
     */
    fun tick(pulses: Int = 1) {
        pulse += pulses
        
        while (pulse >= PULSES_PER_STEP) {
            pulse -= PULSES_PER_STEP
            step++
        }
        
        if (step >= STEPS_PER_PATTERN) {
            step -= STEPS_PER_PATTERN
        }
        
        // Calculate new value
        val rawValue = readTopographicValue(step)
        val shapedValue = applyShape(rawValue, step)
        
        // Store for next iteration
        lastValue = shapedValue
        
        // Apply smoothing
        smoothedValue = when (shape) {
            EnvelopeShape.SMOOTH, EnvelopeShape.EXPONENTIAL -> {
                // Linear interpolation towards target
                smoothedValue + (shapedValue - smoothedValue) * 0.3f
            }
            EnvelopeShape.STEPPED, EnvelopeShape.RANDOM -> {
                shapedValue.toFloat()
            }
        }
        
        currentValue = smoothedValue.toInt().coerceIn(0, 255)
    }
    
    /**
     * Get current envelope value (0-127)
     */
    fun getValue(): Int {
        return applyRangeAndOffset(currentValue shr 1)  // Scale 0-255 to 0-127
    }
    
    /**
     * Get current step (0-31)
     */
    fun getStep(): Int = step
    
    /**
     * Reset sequencer to beginning
     */
    fun reset() {
        step = 0
        pulse = 0
        smoothedValue = 64f
        lastValue = 64
    }
    
    /**
     * Set topographic position
     */
    fun setPosition(x: Int, y: Int) {
        this.x = x.coerceIn(0, 255)
        this.y = y.coerceIn(0, 255)
    }
}
