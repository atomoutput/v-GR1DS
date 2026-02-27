package com.volcagrids.plaits.dsp.oscillator

/**
 * Variable Saw Oscillator
 * Saw with variable slope or notch
 */
class VariableSawOscillator {
    companion object {
        const val MAX_FREQUENCY: Float = 0.25f
        const val NOTCH_DEPTH: Float = 0.2f
    }

    // Oscillator state
    private var phase = 0.0f
    private var nextSample = 0.0f
    private var previousPw = 0.5f
    private var high = false

    // Parameters (for interpolation)
    private var frequency = 0.01f
    private var pw = 0.5f
    private var waveshape = 0.0f

    fun init() {
        phase = 0.0f
        nextSample = 0.0f
        previousPw = 0.5f
        high = false
        frequency = 0.01f
        pw = 0.5f
        waveshape = 0.0f
    }

    fun render(
        frequency: Float,
        pw: Float,
        waveshape: Float,
        out: FloatArray,
        size: Int
    ) {
        var freq = frequency
        var pw = pw
        var waveshape = waveshape

        if (freq >= MAX_FREQUENCY) freq = MAX_FREQUENCY

        if (freq >= 0.25f) {
            pw = 0.5f
        } else {
            pw = pw.coerceIn(freq * 2.0f, 1.0f - 2.0f * freq)
        }

        val fm = ParameterInterpolator(this.frequency, freq, size)
        val pwm = ParameterInterpolator(this.pw, pw, size)
        val waveshapeMod = ParameterInterpolator(this.waveshape, waveshape, size)

        var nextSampleLocal = nextSample

        for (i in 0 until size) {
            var thisSample = nextSampleLocal
            nextSampleLocal = 0.0f

            val currentFreq = fm.next()
            val currentPw = pwm.next()
            val currentWaveshape = waveshapeMod.next()

            val triangleAmount = currentWaveshape
            val notchAmount = 1.0f - currentWaveshape
            val slopeUp = 1.0f / currentPw
            val slopeDown = 1.0f / (1.0f - currentPw)

            phase += currentFreq

            if (!high && phase >= currentPw) {
                val triangleStep = (slopeUp + slopeDown) * currentFreq * triangleAmount
                val notch = (NOTCH_DEPTH + 1.0f - currentPw) * notchAmount
                val t = (phase - currentPw) / (previousPw - currentPw + currentFreq)

                thisSample += notch * PolyBLEP.thisBlepSample(t)
                nextSampleLocal += notch * PolyBLEP.nextBlepSample(t)
                thisSample -= triangleStep * PolyBLEP.thisIntegratedBlepSample(t)
                nextSampleLocal -= triangleStep * PolyBLEP.nextIntegratedBlepSample(t)
                high = true
            } else if (phase >= 1.0f) {
                phase -= 1.0f
                val triangleStep = (slopeUp + slopeDown) * currentFreq * triangleAmount
                val notch = (NOTCH_DEPTH + 1.0f) * notchAmount
                val t = phase / currentFreq

                thisSample -= notch * PolyBLEP.thisBlepSample(t)
                nextSampleLocal -= notch * PolyBLEP.nextBlepSample(t)
                thisSample += triangleStep * PolyBLEP.thisIntegratedBlepSample(t)
                nextSampleLocal += triangleStep * PolyBLEP.nextIntegratedBlepSample(t)
                high = false
            }

            nextSampleLocal += computeNaiveSample(
                phase, currentPw, slopeUp, slopeDown,
                triangleAmount, notchAmount
            )
            previousPw = currentPw

            out[i] = (2.0f * thisSample - 1.0f) / (1.0f + NOTCH_DEPTH)
        }

        nextSample = nextSampleLocal
    }

    private fun computeNaiveSample(
        phase: Float,
        pw: Float,
        slopeUp: Float,
        slopeDown: Float,
        triangleAmount: Float,
        notchAmount: Float
    ): Float {
        val notchSaw = if (phase < pw) {
            phase
        } else {
            1.0f + NOTCH_DEPTH
        }
        val triangle = if (phase < pw)
            phase * slopeUp
        else
            1.0f - (phase - pw) * slopeDown

        return notchSaw * notchAmount + triangle * triangleAmount
    }
}
