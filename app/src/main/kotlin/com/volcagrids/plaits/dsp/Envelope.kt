package com.volcagrids.plaits.dsp

import kotlin.math.exp
import kotlin.math.pow

/**
 * Simple decay envelope for drums/percussion
 */
class DecayEnvelope {
    private var value_ = 0.0f
    private var increment_ = 0.0f

    fun init() {
        value_ = 0.0f
        increment_ = 0.0f
    }

    fun trigger() {
        value_ = 1.0f
    }

    fun process(decay: Float) {
        value_ -= decay
        if (value_ < 0.0f) {
            value_ = 0.0f
        }
    }

    fun processPing(attack: Float, decay1: Float, decay2: Float, hf: Float) {
        if (value_ < 0.001f) {
            increment_ = attack
        }
        value_ += increment_
        if (increment_ > 0.0f) {
            if (value_ >= 1.0f) {
                value_ = 1.0f
                increment_ = -decay1
            }
        } else {
            if (value_ <= 0.0f) {
                value_ = 0.0f
                increment_ = 0.0f
            } else {
                increment_ += decay2 * (1.0f - hf) * 0.0003f
            }
        }
    }

    fun value(): Float = value_

    fun setValue(value: Float) {
        value_ = value
    }
}

/**
 * LPG (Low Pass Gate) envelope
 * Combines VCA and VCF envelope with one-pole characteristics
 */
class LPGEnvelope {
    private var gain_ = 0.0f
    private var frequency_ = 0.0f
    private var hfBleed_ = 0.0f

    fun init() {
        gain_ = 0.0f
        frequency_ = 0.0f
        hfBleed_ = 0.0f
    }

    fun trigger() {
        gain_ = 1.0f
    }

    fun processLP(level: Float, shortDecay: Float, longDecay: Float, hf: Float) {
        // Process gain envelope
        val gainTarget = level
        val gainCoeff = if (level > gain_) 0.03f else shortDecay
        gain_ = gain_ + gainCoeff * (gainTarget - gain_)

        // Process frequency envelope
        val freqTarget = 0.01f + level * (1.0f - hf)
        val freqCoeff = if (freqTarget > frequency_) 0.03f else shortDecay * 2.0f
        frequency_ = frequency_ + freqCoeff * (freqTarget - frequency_)

        hfBleed_ = hf * 0.5f
    }

    fun processPing(attack: Float, shortDecay: Float, longDecay: Float, hf: Float) {
        if (gain_ < 0.001f) {
            gain_ = attack
        }
        gain_ -= shortDecay
        if (gain_ <= 0.0f) {
            gain_ = 0.0f
        }

        frequency_ = gain_ * (1.0f - hf)
        hfBleed_ = hf * 0.5f
    }

    fun gain(): Float = gain_
    fun frequency(): Float = frequency_
    fun hfBleed(): Float = hfBleed_
}

/**
 * Attack-release envelope
 */
class AREnvelope {
    private var value_ = 0.0f
    private var state_ = false

    fun init() {
        value_ = 0.0f
        state_ = false
    }

    fun trigger() {
        state_ = true
    }

    fun release() {
        state_ = false
    }

    fun process(attack: Float, release: Float): Float {
        val target = if (state_) 1.0f else 0.0f
        val coeff = if (state_) attack else release
        value_ = value_ + coeff * (target - value_)
        return value_
    }

    fun value(): Float = value_
}
