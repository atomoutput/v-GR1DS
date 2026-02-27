package com.volcagrids.plaits.dsp

import com.volcagrids.plaits.dsp.engines.*
import com.volcagrids.plaits.dsp.engines.BooleanRef
import com.volcagrids.plaits.dsp.engines.Engine

/**
 * Main Plaits Voice
 * Combines all engines with LPG and post-processing
 * This is the main interface for audio rendering
 */
class PlaitsVoice {
    companion object {
        const val NUM_ENGINES = 24
        const val MAX_BLOCK_SIZE = 24
        const val TRIGGER_LOW = 0
        const val TRIGGER_RISING_EDGE = 1
        const val TRIGGER_UNPATCHED = 2
        const val TRIGGER_HIGH = 4
    }

    // All engines
    private val engines = arrayOfNulls<Engine>(NUM_ENGINES)

    // LPG and envelopes
    private val lpg = com.volcagrids.plaits.dsp.fx.LowPassGate()
    private val decayEnvelope = DecayEnvelope()
    private val lpgEnvelope = LPGEnvelope()

    // Output buffers
    private val outBuffer = FloatArray(MAX_BLOCK_SIZE)
    private val auxBuffer = FloatArray(MAX_BLOCK_SIZE)

    // State
    private var previousEngineIndex = -1
    private var reloadUserData = false

    // Quantizer for engine selection
    private var engineCV = 0.0f

    init {
        initEngines()
    }

    private fun initEngines() {
        // Register engines (simplified - only core engines for now)
        registerEngine(0, VirtualAnalogEngine(), outGain = 0.8f, auxGain = 0.8f)
        registerEngine(1, FMEngine(), outGain = 0.6f, auxGain = 0.6f)
        registerEngine(2, WavetableEngine(), outGain = 0.6f, auxGain = 0.6f)
        registerEngine(3, NoiseEngine(), outGain = -1.0f, auxGain = -1.0f)

        // Initialize LPG
        lpg.init()
        decayEnvelope.init()
        lpgEnvelope.init()
    }

    private fun registerEngine(index: Int, engine: Engine, outGain: Float, auxGain: Float) {
        engines[index] = engine
        engine.init()
        engine.postProcessingSettings.outGain = outGain
        engine.postProcessingSettings.auxGain = auxGain
    }

    /**
     * Reload user data for current engine
     */
    fun reloadUserData() {
        reloadUserData = true
    }

    /**
     * Render audio
     */
    fun render(
        patch: PlaitsPatch,
        modulations: PlaitsModulations,
        frames: ShortArray,
        size: Int
    ) {
        // Handle trigger
        val triggerValue = modulations.trigger
        val previousTriggerState = triggerState
        if (!previousTriggerState && triggerValue > 0.3f) {
            triggerState = true
            if (!modulations.levelPatched) {
                lpgEnvelope.trigger()
            }
            decayEnvelope.trigger()
            engineCV = modulations.engine
        } else if (triggerValue < 0.1f) {
            triggerState = false
        }

        if (!modulations.triggerPatched) {
            engineCV = modulations.engine
        }

        // Engine selection with hysteresis
        val engineIndex = quantizeEngine(patch.engine.toFloat(), engineCV)
        val engine = engines[engineIndex]

        // Handle engine change
        if (engineIndex != previousEngineIndex || reloadUserData) {
            engine?.loadUserData(null)
            engine?.reset()
            previousEngineIndex = engineIndex
            reloadUserData = false
        }

        if (engine == null) {
            // No engine loaded, output silence
            for (i in 0 until size) {
                frames[i * 2] = 0
                frames[i * 2 + 1] = 0
            }
            return
        }

        // Prepare engine parameters
        val params = EngineParameters()

        // Trigger
        params.trigger = if (triggerState) {
            if (!previousTriggerState) TRIGGER_RISING_EDGE else TRIGGER_HIGH
        } else {
            TRIGGER_LOW
        }

        // Note with modulation
        val note = (modulations.note + previousNote) * 0.5f
        previousNote = modulations.note

        val useInternalEnvelope = modulations.triggerPatched
        val internalEnvelopeAmount = if (useInternalEnvelope) {
            decayEnvelope.value() * 48.0f
        } else {
            1.0f
        }

        params.note = applyModulations(
            patch.note + note,
            patch.frequencyModulationAmount,
            modulations.frequencyPatched,
            modulations.frequency,
            useInternalEnvelope,
            internalEnvelopeAmount,
            1.0f,
            -119.0f,
            120.0f
        )

        params.timbre = applyModulations(
            patch.timbre,
            patch.timbreModulationAmount,
            modulations.timbrePatched,
            modulations.timbre,
            useInternalEnvelope,
            decayEnvelope.value(),
            0.0f,
            0.0f,
            1.0f
        )

        params.morph = applyModulations(
            patch.morph,
            patch.morphModulationAmount,
            modulations.morphPatched,
            modulations.morph,
            useInternalEnvelope,
            decayEnvelope.value(),
            0.0f,
            0.0f,
            1.0f
        )

        params.harmonics = (patch.harmonics + modulations.harmonics).coerceIn(0.0f, 1.0f)
        params.accent = if (modulations.levelPatched) {
            val compressed = 1.3f * modulations.level / (0.3f + kotlin.math.abs(modulations.level))
            compressed.coerceIn(0.0f, 1.0f)
        } else {
            0.8f
        }

        // Process decay envelope
        val shortDecay = (200.0f * 12) / PlaitsDSP.SAMPLE_RATE *
                semitonesToRatio(-96.0f * patch.decay)
        decayEnvelope.process(shortDecay * 2.0f)

        // Render engine
        val alreadyEnveloped = BooleanRef(false)
        engine.render(params, outBuffer, auxBuffer, size, alreadyEnveloped)

        // Post-processing with LPG
        val lpgBypass = alreadyEnveloped.value ||
                (!modulations.levelPatched && !modulations.triggerPatched)

        if (!lpgBypass) {
            val hf = patch.lpgColour
            val decayTail = (20.0f * 12) / PlaitsDSP.SAMPLE_RATE *
                    semitonesToRatio(-72.0f * patch.decay + 12.0f * hf) - shortDecay

            if (modulations.levelPatched) {
                lpgEnvelope.processLP(params.accent, shortDecay, decayTail, hf)
            } else {
                val attack = noteToFrequency(params.note) * 12 * 2.0f
                lpgEnvelope.processPing(attack, shortDecay, decayTail, hf)
            }
        } else {
            lpgEnvelope.init()
        }

        // Apply LPG and convert to 16-bit
        val ppSettings = engine.postProcessingSettings
        val lpgBypassFinal = lpgBypass

        for (i in 0 until size) {
            var sample = outBuffer[i]

            // Apply gain
            if (ppSettings.outGain < 0.0f) {
                // Use limiter for negative gain values
                sample = applyLimiter(sample, kotlin.math.abs(ppSettings.outGain))
            } else {
                sample *= ppSettings.outGain
            }

            // Apply LPG if not bypassed
            if (!lpgBypassFinal) {
                val lpgFreq = lpgEnvelope.frequency()
                val lpgGain = lpgEnvelope.gain()
                val hfBleed = lpgEnvelope.hfBleed()

                // Simple one-pole lowpass
                val filterCoeff = lpgFreq.coerceIn(0.001f, 0.5f)
                lpgState = lpgState + filterCoeff * (sample - lpgState)
                sample = lpgState * lpgGain * (1.0f - hfBleed) + sample * hfBleed
            }

            // Convert to 16-bit
            val clipped = sample.coerceIn(-1.0f, 1.0f)
            val outputSample = (clipped * 32767.0f).toInt().toShort()

            // Stereo output (same sample on both channels)
            frames[i * 2] = outputSample     // Left
            frames[i * 2 + 1] = outputSample // Right
        }
    }

    private var triggerState = false
    private var previousNote = 0.0f
    private var lpgState = 0.0f

    private fun quantizeEngine(targetEngine: Float, cv: Float): Int {
        // Simple quantization with hysteresis
        val value = if (cv > 0.01f || cv < -0.01f) cv else targetEngine
        return ((value * NUM_ENGINES).toInt()).coerceIn(0, NUM_ENGINES - 1)
    }

    private fun applyModulations(
        baseValue: Float,
        modulationAmount: Float,
        useExternalModulation: Boolean,
        externalModulation: Float,
        useInternalEnvelope: Boolean,
        envelope: Float,
        defaultInternalModulation: Float,
        minValue: Float,
        maxValue: Float
    ): Float {
        var modAmount = modulationAmount
        modAmount = kotlin.math.max(kotlin.math.abs(modAmount) - 0.05f, 0.05f)
        modAmount *= 1.05f

        val modulation = when {
            useExternalModulation -> externalModulation
            useInternalEnvelope -> envelope
            else -> defaultInternalModulation
        }

        var value = baseValue + modAmount * modulation
        return value.coerceIn(minValue, maxValue)
    }

    private fun applyLimiter(sample: Float, gain: Float): Float {
        val amplified = sample * gain
        return when {
            amplified < -1.0f -> -1.0f
            amplified > 1.0f -> 1.0f
            else -> amplified
        }
    }

    private fun semitonesToRatio(semitones: Float): Float {
        return exp(semitones * 0.057762265f).toFloat()
    }

    private fun noteToFrequency(midiNote: Float): Float {
        val note = (midiNote - 9.0f).coerceIn(-128.0f, 127.0f)
        return PlaitsDSP.A0 * 0.25f * semitonesToRatio(note)
    }

    private fun exp(x: Float): Float {
        return kotlin.math.exp(x.toDouble()).toFloat()
    }

    /**
     * Get active engine index
     */
    fun getActiveEngine(): Int = previousEngineIndex

    /**
     * Set engine directly
     */
    fun setEngine(index: Int) {
        engineCV = index.toFloat() / NUM_ENGINES
    }
}
