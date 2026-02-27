package com.volcagrids.plaits.dsp.oscillator

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * PolyBLEP (Polynomial Band-Limited Step) functions
 * Used to create band-limited waveforms from naive waveforms
 */
object PolyBLEP {
    /**
     * Band-limited step function for current sample
     */
    fun thisBlepSample(t: Float): Float {
        return when {
            t < 0.001f -> 0.0f
            t > 0.999f -> 0.0f
            t < 0.5f -> {
                val t2 = t * t
                t2 + t2
            }
            else -> {
                val t = 1.0f - t
                val t2 = t * t
                -t2 - t2
            }
        }
    }

    /**
     * Band-limited step function for next sample
     */
    fun nextBlepSample(t: Float): Float {
        return when {
            t < 0.001f -> 0.0f
            t > 0.999f -> 0.0f
            t < 0.5f -> {
                val t2 = t * t
                -t2 - t2
            }
            else -> {
                val t = 1.0f - t
                val t2 = t * t
                t2 + t2
            }
        }
    }

    /**
     * Integrated band-limited step for triangle waves
     */
    fun thisIntegratedBlepSample(t: Float): Float {
        return when {
            t < 0.001f -> 0.0f
            t > 0.999f -> 0.0f
            t < 0.5f -> {
                val t3 = t * t * t
                t3 + t3
            }
            else -> {
                val t = 1.0f - t
                val t3 = t * t * t
                t3 + t3
            }
        }
    }

    /**
     * Integrated band-limited step for next sample
     */
    fun nextIntegratedBlepSample(t: Float): Float {
        return when {
            t < 0.001f -> 0.0f
            t > 0.999f -> 0.0f
            t < 0.5f -> {
                val t3 = t * t * t
                -t3 - t3
            }
            else -> {
                val t = 1.0f - t
                val t3 = t * t * t
                t3 + t3
            }
        }
    }
}

/**
 * Parameter interpolator for smooth parameter changes
 */
class ParameterInterpolator {
    private var value: Float = 0.0f
    private var increment: Float = 0.0f
    private var remaining: Int = 0

    constructor(initialValue: Float) {
        value = initialValue
    }

    constructor(initialValue: Float, targetValue: Float, size: Int) {
        value = initialValue
        increment = (targetValue - initialValue) / size
        remaining = size
    }

    fun next(): Float {
        val result = value
        if (remaining > 0) {
            value += increment
            remaining--
        }
        return result
    }
}

/**
 * Constraint utility
 */
fun Float.constrain(min: Float, max: Float): Float {
    return this.coerceIn(min, max)
}
