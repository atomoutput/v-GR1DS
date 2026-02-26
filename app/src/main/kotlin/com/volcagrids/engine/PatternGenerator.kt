package com.volcagrids.engine

import kotlin.random.Random
import java.util.concurrent.atomic.AtomicIntegerArray

enum class OutputMode {
    EUCLIDEAN,
    DRUMS,
    POLYRHYTHM
}

class PatternGenerator {
    companion object {
        const val NUM_PARTS = 3
        const val PULSES_PER_STEP = 3 // 24 ppqn ; 8 steps per quarter note.
        const val STEPS_PER_PATTERN = 32
        
        private val drumMap = arrayOf(
            intArrayOf(10, 8, 0, 9, 11),
            intArrayOf(15, 7, 13, 12, 6),
            intArrayOf(18, 14, 4, 5, 3),
            intArrayOf(23, 16, 21, 1, 2),
            intArrayOf(24, 19, 17, 20, 22)
        )

        private fun u8Mix(a: Int, b: Int, mix: Int): Int {
            return (a * (255 - mix) + b * mix) shr 8
        }
    }

    @Volatile var x = 127
    @Volatile var y = 127
    @Volatile var randomness = 0
    val densities = AtomicIntegerArray(NUM_PARTS)
    var rawLevels = IntArray(NUM_PARTS) { 0 }
    var euclideanLengths = IntArray(NUM_PARTS) { 16 }
    var partModes = Array(NUM_PARTS) { OutputMode.DRUMS }
    
    private var pulse = 0
    @Volatile private var step = 0
    private var euclideanSteps = IntArray(NUM_PARTS) { 0 }
    @Volatile private var state = 0
    private var partPerturbation = IntArray(NUM_PARTS) { 0 }

    fun reset() {
        step = 0
        pulse = 0
        euclideanSteps.fill(0)
    }

    private fun readDrumMap(step: Int, instrument: Int, x: Int, y: Int): Int {
        val i = x shr 6
        val j = y shr 6
        val xi = (x shl 2) and 0xff
        val yi = (y shl 2) and 0xff

        val aMap = Resources.nodeTable[drumMap[i][j]]
        val bMap = Resources.nodeTable[drumMap[i + 1][j]]
        val cMap = Resources.nodeTable[drumMap[i][j + 1]]
        val dMap = Resources.nodeTable[drumMap[i + 1][j + 1]]

        val offset = (instrument * STEPS_PER_PATTERN) + step
        val a = aMap[offset]
        val b = bMap[offset]
        val c = cMap[offset]
        val d = dMap[offset]

        return u8Mix(u8Mix(a, b, xi), u8Mix(c, d, xi), yi)
    }

    private fun evaluatePart(i: Int) {
        val instrumentMask = 1 shl i
        
        if (partModes[i] == OutputMode.EUCLIDEAN) {
            // Euclidean Logic
            if (step % 2 != 0) return
            val length = (euclideanLengths[i] shr 3) + 1
            val density = (densities.get(i) shr 3)
            val address = (length - 1) * 32 + density
            
            while (euclideanSteps[i] >= length) {
                euclideanSteps[i] -= length
            }
            
            val patternBits = Resources.lut_res_euclidean[address]
            val stepMask = 1L shl euclideanSteps[i]
            
            if ((patternBits and stepMask) != 0L) {
                state = state or instrumentMask
                rawLevels[i] = 255
            } else {
                rawLevels[i] = 0
            }
        } else {
            // Topographic Logic
            var level = readDrumMap(step, i, x, y)
            level = (level + partPerturbation[i]).coerceAtMost(255)
            rawLevels[i] = level
            
            val threshold = 255 - densities.get(i)
            if (level > threshold) {
                state = state or instrumentMask
                // Check for accent (fixed threshold > 192)
                if (level > 192) {
                    state = state or (instrumentMask shl 3)
                }
            }
        }
    }

    private fun evaluate() {
        state = 0
        if (step == 0) {
            for (p in 0 until NUM_PARTS) {
                partPerturbation[p] = (Random.nextInt(256) * (randomness shr 2)) shr 8
            }
        }
        for (i in 0 until NUM_PARTS) {
            evaluatePart(i)
        }
    }

    fun tick(numPulses: Int): Int {
        evaluate()
        val currentOutput = state
        
        pulse += numPulses
        while (pulse >= PULSES_PER_STEP) {
            pulse -= PULSES_PER_STEP
            if (step % 2 == 0) {
                for (i in 0 until NUM_PARTS) {
                    euclideanSteps[i]++
                }
            }
            step++
        }
        
        if (step >= STEPS_PER_PATTERN) {
            step -= STEPS_PER_PATTERN
        }
        
        return currentOutput
    }

    fun getState(): Int = state
    fun getStep(): Int = step
}
