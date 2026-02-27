package com.volcagrids.plaits.dsp

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow

/**
 * Utility DSP routines ported from Plaits
 */
object PlaitsDSP {
    const val SAMPLE_RATE: Float = 48000.0f
    const val CORRECTED_SAMPLE_RATE: Float = 47872.34f
    const val A0: Float = (440.0f / 8.0f) / CORRECTED_SAMPLE_RATE
    const val MAX_BLOCK_SIZE: Int = 24
    const val BLOCK_SIZE: Int = 12

    /**
     * Convert MIDI note to frequency
     */
    fun noteToFrequency(midiNote: Float): Float {
        var note = midiNote - 9.0f
        note = note.coerceIn(-128.0f, 127.0f)
        return A0 * 0.25f * semitonesToRatio(note)
    }

    /**
     * Convert semitones to frequency ratio
     */
    fun semitonesToRatio(semitones: Float): Float {
        return exp(semitones * 0.057762265f).toFloat()
    }

    /**
     * Convert frequency to MIDI note
     */
    fun frequencyToNote(frequency: Float): Float {
        val ratio = (frequency / 440.0f).toDouble()
        return (69.0 + 12.0 * (kotlin.math.ln(ratio) / kotlin.math.ln(2.0))).toFloat()
    }

    /**
     * Soft clipping
     */
    fun softClip(x: Float): Float {
        return when {
            x < -3.0f -> -1.0f
            x > 3.0f -> 1.0f
            else -> {
                val x2 = x * x
                x * (27.0f + x2) / (27.0f + 9.0f * x2)
            }
        }
    }

    /**
     * One-pole lowpass filter
     */
    fun onePole(current: Float, target: Float, coefficient: Float): Float {
        return current + coefficient * (target - current)
    }

    /**
     * Linear interpolation
     */
    fun lininterp(a: Float, b: Float, t: Float): Float {
        return a + t * (b - a)
    }

    /**
     * Square wave
     */
    fun square(phase: Float, pw: Float = 0.5f): Float {
        return if (phase < pw) 1.0f else -1.0f
    }

    /**
     * Sawtooth wave
     */
    fun saw(phase: Float): Float {
        return 2.0f * phase - 1.0f
    }

    /**
     * Triangle wave
     */
    fun triangle(phase: Float): Float {
        return 1.0f - 4.0f * kotlin.math.abs(phase - 0.5f)
    }

    /**
     * Sine wave approximation (faster than Math.sin)
     */
    fun sin(phase: Float): Float {
        val p = phase * 2.0f * PI
        return kotlin.math.sin(p).toFloat()
    }

    /**
     * Convert phase to radians
     */
    fun phaseToRadians(phase: Float): Float {
        return phase * 2.0f * PI.toFloat()
    }
}

/**
 * Trigger states
 */
enum class TriggerState {
    LOW,
    RISING_EDGE,
    UNPATCHED,
    HIGH
}

/**
 * Engine parameters structure
 */
data class EngineParameters(
    var trigger: Int = 0,
    var note: Float = 0.0f,
    var timbre: Float = 0.0f,
    var morph: Float = 0.0f,
    var harmonics: Float = 0.0f,
    var accent: Float = 0.8f
)

/**
 * Patch settings
 */
data class PlaitsPatch(
    var note: Float = 0.0f,
    var harmonics: Float = 0.5f,
    var timbre: Float = 0.5f,
    var morph: Float = 0.5f,
    var frequencyModulationAmount: Float = 0.0f,
    var timbreModulationAmount: Float = 0.0f,
    var morphModulationAmount: Float = 0.0f,
    var engine: Int = 0,
    var decay: Float = 0.5f,
    var lpgColour: Float = 0.5f
)

/**
 * Modulation inputs
 */
data class PlaitsModulations(
    var engine: Float = 0.0f,
    var note: Float = 0.0f,
    var frequency: Float = 0.0f,
    var harmonics: Float = 0.0f,
    var timbre: Float = 0.0f,
    var morph: Float = 0.0f,
    var trigger: Float = 0.0f,
    var level: Float = 0.0f,
    var frequencyPatched: Boolean = false,
    var timbrePatched: Boolean = false,
    var morphPatched: Boolean = false,
    var triggerPatched: Boolean = false,
    var levelPatched: Boolean = false
)
