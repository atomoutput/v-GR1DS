package com.volcagrids.plaits.dsp.oscillator

import kotlin.math.abs

/**
 * Variable Shape Oscillator
 * Continuously variable waveform: triangle > saw > square
 * Both square and triangle have variable slope/pulse-width
 * Supports hard sync to a master frequency
 */
class VariableShapeOscillator {
    companion object {
        const val MAX_FREQUENCY: Float = 0.25f
    }

    // Oscillator state
    private var masterPhase = 0.0f
    private var slavePhase = 0.0f
    private var nextSample = 0.0f
    private var previousPw = 0.5f
    private var high = false

    // Parameters (for interpolation)
    private var masterFrequency = 0.0f
    private var slaveFrequency = 0.01f
    private var pw = 0.5f
    private var waveshape = 0.0f
    private var phaseModulation = 0.0f

    fun init() {
        masterPhase = 0.0f
        slavePhase = 0.0f
        nextSample = 0.0f
        previousPw = 0.5f
        high = false
        masterFrequency = 0.0f
        slaveFrequency = 0.01f
        pw = 0.5f
        waveshape = 0.0f
        phaseModulation = 0.0f
    }

    fun render(
        frequency: Float,
        pw: Float,
        waveshape: Float,
        out: FloatArray,
        size: Int
    ) {
        renderSync(false, 0.0f, frequency, pw, waveshape, 0.0f, out, size)
    }

    fun render(
        masterFrequency: Float,
        frequency: Float,
        pw: Float,
        waveshape: Float,
        out: FloatArray,
        size: Int
    ) {
        renderSync(true, masterFrequency, frequency, pw, waveshape, 0.0f, out, size)
    }

    private fun renderSync(
        enableSync: Boolean,
        masterFreq: Float,
        frequency: Float,
        pw: Float,
        waveshape: Float,
        phaseModAmount: Float,
        out: FloatArray,
        size: Int
    ) {
        var masterFreq = masterFreq
        var freq = frequency
        var pw = pw
        var waveshape = waveshape

        if (masterFreq >= MAX_FREQUENCY) masterFreq = MAX_FREQUENCY
        if (freq >= MAX_FREQUENCY) freq = MAX_FREQUENCY

        if (freq >= 0.25f) {
            pw = 0.5f
        } else {
            pw = pw.coerceIn(freq * 2.0f, 1.0f - 2.0f * freq)
        }

        val masterFm = ParameterInterpolator(this.masterFrequency, masterFreq, size)
        val fm = ParameterInterpolator(this.slaveFrequency, freq, size)
        val pwm = ParameterInterpolator(this.pw, pw, size)
        val waveshapeMod = ParameterInterpolator(this.waveshape, waveshape, size)
        val phaseMod = ParameterInterpolator(this.phaseModulation, phaseModAmount, size)

        var nextSampleLocal = nextSample

        for (i in 0 until size) {
            var reset = false
            var transitionDuringReset = false
            var resetTime = 0.0f

            var thisSample = nextSampleLocal
            nextSampleLocal = 0.0f

            val mFreq = masterFm.next()
            val sFreq = fm.next()
            val currentPw = pwm.next()
            val currentWaveshape = waveshapeMod.next()

            val squareAmount = (currentWaveshape - 0.5f).coerceAtLeast(0.0f) * 2.0f
            val triangleAmount = (1.0f - currentWaveshape * 2.0f).coerceAtLeast(0.0f)

            val slopeUp = 1.0f / currentPw
            val slopeDown = 1.0f / (1.0f - currentPw)

            // Handle sync
            if (enableSync) {
                masterPhase += mFreq
                if (masterPhase >= 1.0f) {
                    masterPhase -= 1.0f
                    resetTime = masterPhase / mFreq

                    var slavePhaseAtReset = slavePhase + (1.0f - resetTime) * sFreq
                    reset = true
                    if (slavePhaseAtReset >= 1.0f) {
                        slavePhaseAtReset -= 1.0f
                        transitionDuringReset = true
                    }
                    if (!high && slavePhaseAtReset >= currentPw) {
                        transitionDuringReset = true
                    }

                    val value = computeNaiveSample(
                        slavePhaseAtReset, currentPw, slopeUp, slopeDown,
                        triangleAmount, squareAmount
                    )
                    thisSample -= value * PolyBLEP.thisBlepSample(resetTime)
                    nextSampleLocal -= value * PolyBLEP.nextBlepSample(resetTime)
                }
            }

            slavePhase += sFreq

            while (transitionDuringReset || !reset) {
                if (!high) {
                    if (slavePhase < currentPw) break

                    val t = (slavePhase - currentPw) / (previousPw - currentPw + sFreq)
                    val triangleStep = (slopeUp + slopeDown) * sFreq * triangleAmount

                    thisSample += squareAmount * PolyBLEP.thisBlepSample(t)
                    nextSampleLocal += squareAmount * PolyBLEP.nextBlepSample(t)
                    thisSample -= triangleStep * PolyBLEP.thisIntegratedBlepSample(t)
                    nextSampleLocal -= triangleStep * PolyBLEP.nextIntegratedBlepSample(t)
                    high = true
                }

                if (high) {
                    if (slavePhase < 1.0f) break

                    slavePhase -= 1.0f
                    val t = slavePhase / sFreq
                    val triangleStep = (slopeUp + slopeDown) * sFreq * triangleAmount

                    thisSample -= (1.0f - triangleAmount) * PolyBLEP.thisBlepSample(t)
                    nextSampleLocal -= (1.0f - triangleAmount) * PolyBLEP.nextBlepSample(t)
                    thisSample += triangleStep * PolyBLEP.thisIntegratedBlepSample(t)
                    nextSampleLocal += triangleStep * PolyBLEP.nextIntegratedBlepSample(t)
                    high = false
                }
            }

            if (enableSync && reset) {
                slavePhase = resetTime * sFreq
                high = false
            }

            nextSampleLocal += computeNaiveSample(
                slavePhase, currentPw, slopeUp, slopeDown,
                triangleAmount, squareAmount
            )
            previousPw = currentPw

            out[i] = (2.0f * thisSample - 1.0f)
        }

        nextSample = nextSampleLocal
    }

    private fun computeNaiveSample(
        phase: Float,
        pw: Float,
        slopeUp: Float,
        slopeDown: Float,
        triangleAmount: Float,
        squareAmount: Float
    ): Float {
        val saw = phase
        val square = if (phase < pw) 0.0f else 1.0f
        val triangle = if (phase < pw)
            phase * slopeUp
        else
            1.0f - (phase - pw) * slopeDown

        var result = saw + (square - saw) * squareAmount
        result += (triangle - result) * triangleAmount
        return result
    }

    fun setMasterPhase(phase: Float) {
        masterPhase = phase
    }

    fun getMasterPhase(): Float = masterPhase
}
