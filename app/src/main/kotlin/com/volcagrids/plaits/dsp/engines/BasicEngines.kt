package com.volcagrids.plaits.dsp.engines

import com.volcagrids.plaits.dsp.EngineParameters
import com.volcagrids.plaits.dsp.PlaitsDSP
import kotlin.math.cos
import kotlin.math.exp

/**
 * Noise Engine
 * White noise with filter and modulation
 */
class NoiseEngine : Engine() {
    companion object {
        private const val NOISE_SCALE = 1.0f / 4294967296.0f
    }

    private var state = 0x12345678L
    private var filterState = 0.0f

    override fun init() {
        state = 0x12345678L
        filterState = 0.0f
        postProcessingSettings.outGain = -1.0f  // Negative = use limiter
        postProcessingSettings.auxGain = -1.0f
        postProcessingSettings.alreadyEnveloped = false
    }

    override fun reset() {
        state = 0x12345678L
        filterState = 0.0f
    }

    override fun loadUserData(userData: ByteArray?) {
        // No user data for noise engine
    }

    override fun render(
        parameters: EngineParameters,
        out: FloatArray,
        aux: FloatArray,
        size: Int,
        alreadyEnveloped: BooleanRef
    ) {
        // Timbre controls filter cutoff
        val cutoff = 0.01f + parameters.timbre * parameters.timbre * 20.0f
        val resonance = parameters.morph * 0.8f

        // Harmonics controls color/brightness
        val color = parameters.harmonics

        for (i in 0 until size) {
            // Generate white noise
            val noise = nextNoise()

            // Low-pass filter
            val filterCoeff = cutoff.coerceIn(0.001f, 0.999f)
            filterState = filterState + filterCoeff * (noise - filterState)

            // Mix dry/wet based on color
            val dryAmount = 1.0f - color
            val wetAmount = color
            val filtered = filterState * wetAmount + noise * dryAmount

            // Apply resonance boost
            val withResonance = filtered * (1.0f + resonance * 0.5f)

            out[i] = withResonance * 0.5f
            aux[i] = noise * 0.3f
        }

        alreadyEnveloped.value = false
    }

    private fun nextNoise(): Float {
        // Linear congruential generator for white noise
        state = (state * 6364136223846793005L + 1442695040888963407L)
        return (state.toInt().toLong() * NOISE_SCALE * 2.0f - 1.0f).toFloat()
    }
}

/**
 * Wavetable Engine
 * Scans through wavetables with different waveforms
 */
class WavetableEngine : Engine() {
    companion object {
        const val TABLE_SIZE = 256
        const val NUM_TABLES = 8
    }

    private var phase = 0.0f
    private var previousWavetableIndex = 0.0f

    // Pre-computed wavetables
    private val wavetables = Array(NUM_TABLES) { FloatArray(TABLE_SIZE) }

    override fun init() {
        generateWavetables()
        phase = 0.0f
        previousWavetableIndex = 0.0f
        postProcessingSettings.outGain = 0.6f
        postProcessingSettings.auxGain = 0.6f
        postProcessingSettings.alreadyEnveloped = false
    }

    override fun reset() {
        phase = 0.0f
        previousWavetableIndex = 0.0f
    }

    override fun loadUserData(userData: ByteArray?) {
        // Could load custom wavetables
    }

    override fun render(
        parameters: EngineParameters,
        out: FloatArray,
        aux: FloatArray,
        size: Int,
        alreadyEnveloped: BooleanRef
    ) {
        val freq = PlaitsDSP.noteToFrequency(parameters.note).coerceAtMost(0.25f)

        // Timbre selects wavetable position (0 to NUM_TABLES-1)
        val wavetableIndex = parameters.timbre * (NUM_TABLES - 1).toFloat()

        // Morph adds spectral variation within table
        val spectralMod = parameters.morph * 0.2f

        for (i in 0 until size) {
            phase += freq
            if (phase >= 1.0f) phase -= 1.0f

            // Interpolate between wavetables
            val tableIndex = wavetableIndex.toInt()
            val tableFrac = wavetableIndex - tableIndex
            val tableIndex2 = (tableIndex + 1).coerceAtMost(NUM_TABLES - 1)

            // Read from wavetables with linear interpolation
            val tablePos = ((phase * TABLE_SIZE).toInt() and (TABLE_SIZE - 1))
            val sample1 = wavetables[tableIndex][tablePos]
            val sample2 = wavetables[tableIndex2][tablePos]
            val sample = sample1 + (sample2 - sample1) * tableFrac

            out[i] = sample * 0.8f
            aux[i] = sample * 0.4f
        }

        alreadyEnveloped.value = false
    }

    private fun generateWavetables() {
        // Generate different wavetables with varying harmonic content
        for (table in 0 until NUM_TABLES) {
            val numHarmonics = table + 1
            for (i in 0 until TABLE_SIZE) {
                val phase = i.toFloat() / TABLE_SIZE
                var sample = 0.0f
                for (h in 1..numHarmonics) {
                    val amplitude = 1.0f / h
                    sample += (amplitude * kotlin.math.cos((phase * h * 2.0f * kotlin.math.PI).toDouble()).toFloat())
                }
                wavetables[table][i] = sample / numHarmonics
            }
        }
    }
}
