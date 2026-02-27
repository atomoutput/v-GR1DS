package com.volcagrids.plaits.dsp.fx

import kotlin.math.exp
import kotlin.math.pow

/**
 * Low Pass Gate
 * Combines VCA and low-pass filter in one module
 * Emulates the behavior of vactrol-based LPG circuits
 */
class LowPassGate {
    private var state_ = 0.0f
    private var frequency_ = 0.0f

    fun init() {
        state_ = 0.0f
        frequency_ = 0.0f
    }

    fun process(
        gain: Float,
        frequency: Float,
        hfBleed: Float,
        `in`: FloatArray,
        out: ShortArray,
        size: Int,
        stride: Int
    ) {
        var outIdx = 0
        for (i in 0 until size) {
            // One-pole lowpass filter
            val f = frequency.coerceIn(0.001f, 0.999f)
            state_ = state_ + f * (`in`[i] - state_)

            // Apply gain and convert to 16-bit
            val sample = state_ * gain
            val clipped = sample.coerceIn(-1.0f, 1.0f)
            out[outIdx] = (clipped * 32767.0f).toInt().toShort()
            outIdx += stride
        }
    }

    fun processFloat(
        gain: Float,
        frequency: Float,
        hfBleed: Float,
        `in`: FloatArray,
        out: FloatArray,
        size: Int
    ) {
        var state = 0.0f
        for (i in 0 until size) {
            // One-pole lowpass filter with frequency modulation
            val f = frequency.coerceIn(0.001f, 0.499f)
            state = state + f * (`in`[i] - state)

            // Apply gain
            out[i] = state * gain
        }
    }
}
