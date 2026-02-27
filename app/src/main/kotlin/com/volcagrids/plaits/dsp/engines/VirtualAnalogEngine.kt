package com.volcagrids.plaits.dsp.engines

import com.volcagrids.plaits.dsp.EngineParameters
import com.volcagrids.plaits.dsp.PlaitsDSP.noteToFrequency
import com.volcagrids.plaits.dsp.PlaitsDSP.semitonesToRatio
import com.volcagrids.plaits.dsp.oscillator.VariableShapeOscillator
import com.volcagrids.plaits.dsp.oscillator.VariableSawOscillator
import com.volcagrids.plaits.dsp.oscillator.ParameterInterpolator

/**
 * Virtual Analog Engine
 * 2 variable shape oscillators with sync and crossfading
 * Variant 2: Square + Variable Saw with sync
 */
class VirtualAnalogEngine : Engine() {
    companion object {
        private val INTERVALS = floatArrayOf(0.0f, 7.01f, 12.01f, 19.01f, 24.01f)
    }

    private val primary = VariableShapeOscillator()
    private val auxiliary = VariableShapeOscillator()
    private val sync = VariableShapeOscillator()
    private val variableSaw = VariableSawOscillator()

    private var auxiliaryAmount = 0.0f
    private var xmodAmount = 0.0f
    private var tempBuffer = FloatArray(24)

    override fun init() {
        primary.init()
        auxiliary.init()
        auxiliary.setMasterPhase(0.25f)
        sync.init()
        variableSaw.init()
        auxiliaryAmount = 0.0f
        xmodAmount = 0.0f
        tempBuffer = FloatArray(24)

        postProcessingSettings.outGain = 0.8f
        postProcessingSettings.auxGain = 0.8f
        postProcessingSettings.alreadyEnveloped = false
    }

    override fun reset() {
        primary.init()
        auxiliary.init()
        sync.init()
        variableSaw.init()
        auxiliaryAmount = 0.0f
        xmodAmount = 0.0f
    }

    override fun loadUserData(userData: ByteArray?) {
        // No user data for this engine
    }

    override fun render(
        parameters: EngineParameters,
        out: FloatArray,
        aux: FloatArray,
        size: Int,
        alreadyEnveloped: BooleanRef
    ) {
        val syncAmount = parameters.timbre * parameters.timbre
        val auxiliaryDetune = computeDetuning(parameters.harmonics)
        val primaryFreq = noteToFrequency(parameters.note)
        val auxiliaryFreq = noteToFrequency(parameters.note + auxiliaryDetune)
        val primarySyncFreq = noteToFrequency(parameters.note + syncAmount * 48.0f)
        val auxiliarySyncFreq = noteToFrequency(
            parameters.note + auxiliaryDetune + syncAmount * 48.0f
        )

        var shape = parameters.morph * 1.5f
        shape = shape.coerceIn(0.0f, 1.0f)

        var pw = 0.5f + (parameters.morph - 0.66f) * 1.46f
        pw = pw.coerceIn(0.5f, 0.995f)

        // Render monster sync to AUX
        primary.render(primaryFreq, primarySyncFreq, pw, shape, out, size)
        auxiliary.render(auxiliaryFreq, auxiliarySyncFreq, pw, shape, aux, size)
        for (i in 0 until size) {
            aux[i] = (aux[i] - out[i]) * 0.5f
        }

        // Render double vari-shape to OUT
        var squarePw = 1.3f * parameters.timbre - 0.15f
        squarePw = squarePw.coerceIn(0.005f, 0.5f)

        val squareSyncRatio = if (parameters.timbre < 0.5f) {
            0.0f
        } else {
            (parameters.timbre - 0.5f) * (parameters.timbre - 0.5f) * 4.0f * 48.0f
        }

        var squareGain = (parameters.timbre * 8.0f).coerceAtMost(1.0f)

        var sawPw = if (parameters.morph < 0.5f) {
            parameters.morph + 0.5f
        } else {
            1.0f - (parameters.morph - 0.5f) * 2.0f
        }
        sawPw = (sawPw * 1.1f).coerceIn(0.005f, 1.0f)

        var sawShape = (10.0f - 21.0f * parameters.morph).coerceIn(0.0f, 1.0f)
        var sawGain = (8.0f * (1.0f - parameters.morph)).coerceIn(0.02f, 1.0f)

        val squareSyncFreq = noteToFrequency(parameters.note + squareSyncRatio)

        sync.render(primaryFreq, squareSyncFreq, squarePw, 1.0f, tempBuffer, size)
        variableSaw.render(auxiliaryFreq, sawPw, sawShape, out, size)

        val norm = 1.0f / squareGain.coerceAtLeast(sawGain)

        val squareGainMod = ParameterInterpolator(
            auxiliaryAmount,
            squareGain * 0.3f * norm,
            size
        )
        val sawGainMod = ParameterInterpolator(
            xmodAmount,
            sawGain * 0.5f * norm,
            size
        )

        for (i in 0 until size) {
            out[i] = out[i] * sawGainMod.next() + squareGainMod.next() * tempBuffer[i]
        }

        alreadyEnveloped.value = false
    }

    private fun computeDetuning(detune: Float): Float {
        var d = detune
        d = d * 2.05f - 1.025f
        d = d.coerceIn(-1.0f, 1.0f)

        val sign = if (d < 0.0f) -1.0f else 1.0f
        d = d * sign * 3.9999f

        val integral = d.toInt()
        val fractional = d - integral

        val a = INTERVALS[integral.coerceIn(0, 3)]
        val b = INTERVALS[(integral + 1).coerceIn(0, 4)]

        val squashed = squash(squash(fractional))
        return (a + (b - a) * squashed) * sign
    }

    private fun squash(x: Float): Float {
        return x * x * (3.0f - 2.0f * x)
    }
}
