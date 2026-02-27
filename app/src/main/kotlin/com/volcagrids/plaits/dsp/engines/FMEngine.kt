package com.volcagrids.plaits.dsp.engines

import com.volcagrids.plaits.dsp.EngineParameters
import com.volcagrids.plaits.dsp.PlaitsDSP.noteToFrequency
import com.volcagrids.plaits.dsp.PlaitsDSP.semitonesToRatio
import kotlin.math.cos
import kotlin.math.sin

/**
 * FM Engine
 * Classic 2-operator FM synthesis
 * Carrier and modulator with feedback
 */
class FMEngine : Engine() {
    companion object {
        const val MAX_FREQUENCY: Float = 0.25f
    }

    private var carrierPhase = 0.0f
    private var modulatorPhase = 0.0f
    private var subPhase = 0.0f

    private var previousCarrierFrequency = 0.0f
    private var previousModulatorFrequency = 0.0f
    private var previousAmount = 0.0f
    private var previousFeedback = 0.0f
    private var previousSample = 0.0f

    private var subFir = 0.0f
    private var carrierFir = 0.0f

    override fun init() {
        reset()
        postProcessingSettings.outGain = 0.6f
        postProcessingSettings.auxGain = 0.6f
        postProcessingSettings.alreadyEnveloped = false
    }

    override fun reset() {
        carrierPhase = 0.0f
        modulatorPhase = 0.0f
        subPhase = 0.0f
        previousCarrierFrequency = 0.0f
        previousModulatorFrequency = 0.0f
        previousAmount = 0.0f
        previousFeedback = 0.0f
        previousSample = 0.0f
        subFir = 0.0f
        carrierFir = 0.0f
    }

    override fun loadUserData(userData: ByteArray?) {
        // No user data for basic FM engine
    }

    override fun render(
        parameters: EngineParameters,
        out: FloatArray,
        aux: FloatArray,
        size: Int,
        alreadyEnveloped: BooleanRef
    ) {
        // Interpolate parameters
        val carrierFreq = noteToFrequency(parameters.note).coerceAtMost(MAX_FREQUENCY)
        val modulatorFreq = carrierFreq * semitonesToRatio((parameters.harmonics * 48.0f).toFloat())
        val modulatorAmount = (parameters.timbre * 800.0f).toFloat()
        val feedback = (parameters.morph * 0.5f).toFloat()

        for (i in 0 until size) {
            // Interpolate frequencies smoothly
            val currentCarrierFreq = carrierFreq
            val currentModulatorFreq = modulatorFreq

            // Update phases
            carrierPhase += currentCarrierFreq
            modulatorPhase += currentModulatorFreq

            // Wrap phases
            if (carrierPhase >= 1.0f) carrierPhase -= 1.0f
            if (carrierPhase < 0.0f) carrierPhase += 1.0f
            if (modulatorPhase >= 1.0f) modulatorPhase -= 1.0f
            if (modulatorPhase < 0.0f) modulatorPhase += 1.0f

            // FM synthesis with feedback
            val modulatorSignal = sin((modulatorPhase * 2.0f * kotlin.math.PI).toFloat())
            val feedbackSignal = previousSample * feedback
            val modulatedPhase = modulatorSignal + feedbackSignal
            val carrierSignal = sin(((carrierPhase + modulatorAmount * modulatedPhase) * 2.0f * kotlin.math.PI).toFloat())

            // Sub-oscillator (one octave down)
            subPhase += currentCarrierFreq * 0.5f
            if (subPhase >= 1.0f) subPhase -= 1.0f
            val subSignal = sin(subPhase * (2.0f * kotlin.math.PI).toFloat()) * 0.5f

            // Mix carrier and sub
            val sample = carrierSignal + subSignal

            out[i] = sample * 0.5f
            aux[i] = modulatorSignal * 0.3f

            previousSample = sample
        }

        alreadyEnveloped.value = false
    }

    /**
     * Simple sine approximation using Taylor series
     */
    private fun sin(x: Float): Float {
        return kotlin.math.sin(x * 2.0f * kotlin.math.PI).toFloat()
    }

    private fun exp(x: Float): Float {
        return kotlin.math.exp(x).toFloat()
    }
}
