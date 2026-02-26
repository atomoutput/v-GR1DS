package com.volcagrids.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Polyrhythm Engine - True Polyrhythm via Phase Accumulation
 * 
 * Based on music theory:
 * - Each channel has independent STEPS (1-32) making up its geometric ring
 * - Each channel has HITS (1-STEPS) distributed across those slices via Euclidean math
 * - Speed is evaluated against a MASTER MEASURE LENGTH (e.g. 4 beats)
 * - Phase (0.0 to 1.0) continuously sweeps across the steps, and fires triggers if the current slice contains a Hit.
 */
class PolyrhythmEngine {
    companion object {
        enum class TimeDivision(val value: Float, val symbol: String, val description: String) {
            WHOLE(0.25f, "𝅝", "4× slower"),
            HALF(0.5f, "𝅗𝅥", "2× slower"),
            QUARTER(1.0f, "𝅘𝅥", "Normal"),
            EIGHTH(2.0f, "𝅘𝅥𝅮", "2× faster"),
            SIXTEENTH(4.0f, "𝅘𝅥𝅯", "4× faster"),
            TRIPLET(1.5f, "𝅘𝅥T", "Triplet"),
            QUINTUPLET(1.25f, "𝅘𝅥Q", "Quintuplet"),
            SEPTUPLET(1.75f, "𝅘𝅥S", "Septuplet")
        }
        val TIME_DIVISIONS = TimeDivision.entries
        const val PPQN = 24.0 // 24 Pulses Per Quarter Note standard
    }

    // Total geometric slices the phase ring is divided into (1-32)
    private val _steps = mutableStateOf(IntArray(6) { 16 })
    val steps: IntArray get() = _steps.value

    // Number of active triggers distributed across the steps (1-steps)
    private val _hits = mutableStateOf(IntArray(6) { 4 })
    val hits: IntArray get() = _hits.value
    
    // Pattern Time definitions (To support triplets/quintuplets)
    private val _timeDivisions = mutableStateOf(FloatArray(6) { 1.0f })
    val timeDivisions: FloatArray get() = _timeDivisions.value
    
    // True Polyrhythm Master Beat Length (Usually 4 for a 4/4 bar)
    @Volatile var masterBeats = 4
    
    // Continuous Phase (0.0 -> 1.0 bounds)
    @Volatile var phases = DoubleArray(6) { 0.0 }
        private set

    // Mute/Solo state
    var isMuted by mutableStateOf(BooleanArray(6) { false })
    var isSoloed by mutableStateOf(BooleanArray(6) { false })
    
    init {
        // Init typical musical Euclidean sequences
        setStepsHits(0, 16, 4) // Standard 4/4 
        setStepsHits(1, 16, 5) // Cinquillo / Bossa 
        setStepsHits(2, 16, 7) // Rumba
        setStepsHits(3, 8, 3)  // Tresillo
        setStepsHits(4, 12, 5) // Bembe
        setStepsHits(5, 12, 7) // Ashanti
    }
    
    fun setStepsHits(channel: Int, s: Int, h: Int) {
        if (channel !in 0..5) return
        val clampedSteps = s.coerceIn(1, 32)
        val clampedHits = h.coerceIn(1, clampedSteps)
        
        val newSteps = _steps.value.copyOf()
        val newHits = _hits.value.copyOf()
        
        newSteps[channel] = clampedSteps
        newHits[channel] = clampedHits
        
        _steps.value = newSteps
        _hits.value = newHits
    }
    
    fun setTimeDivision(channel: Int, division: TimeDivision) {
        if (channel !in 0..5) return
        val newDivisions = _timeDivisions.value.copyOf()
        newDivisions[channel] = division.value
        _timeDivisions.value = newDivisions
    }
    
    fun cycleTimeDivision(channel: Int) {
        if (channel !in 0..5) return
        val current = getTimeDivision(channel)
        val currentIndex = TIME_DIVISIONS.indexOf(current)
        val nextIndex = (currentIndex + 1) % TIME_DIVISIONS.size
        setTimeDivision(channel, TIME_DIVISIONS[nextIndex])
    }
    
    fun getTimeDivision(channel: Int): TimeDivision {
        if (channel !in 0..5) return TimeDivision.QUARTER
        val div = timeDivisions[channel]
        return TIME_DIVISIONS.find { it.value == div } ?: TimeDivision.QUARTER
    }
    
    fun reset() {
        phases = DoubleArray(6) { 0.0 }
    }
    
    fun setMute(channel: Int, muted: Boolean) {
        if (channel !in 0..5) return
        val newMutes = isMuted.copyOf()
        newMutes[channel] = muted
        isMuted = newMutes
    }
    
    fun setSolo(channel: Int, soloed: Boolean) {
        if (channel !in 0..5) return
        val newSolos = isSoloed.copyOf()
        if (soloed) {
            for (i in 0..5) newSolos[i] = (i == channel)
        } else {
            newSolos[channel] = false
        }
        isSoloed = newSolos
    }
    
    /**
     * Tick called every MIDI Clock pulse (24 PPQN).
     * Calculates pure phase delta and triggers on boundaries.
     */
    fun tick(advancePhase: Boolean = true): BooleanArray {
        val triggers = BooleanArray(6)
        val anySoloed = isSoloed.any { it }
        
        for (ch in 0..5) {
            val shouldPlay = !isMuted[ch] && (!anySoloed || isSoloed[ch])
            val s = steps[ch]
            val h = hits[ch]
            
            // Calculate how far the phase advances this tick
            val pulsesRequired = masterBeats.toDouble() * PPQN
            val phaseDelta = timeDivisions[ch].toDouble() / pulsesRequired
            
            val previousPhase = phases[ch]
            if (advancePhase) phases[ch] += phaseDelta
            
            // Handle wrap-around seamlessly
            if (phases[ch] >= 1.0) {
                phases[ch] -= 1.0
            }

            // A phase spans from 0.0 to 1.0. 
            // We multiply by 'steps' to find which "slice index" (0 to steps-1) we are currently in.
            val currentSlice = (phases[ch] * s).toInt().coerceIn(0, s - 1)
            val previousSlice = (previousPhase * s).toInt().coerceIn(0, s - 1)
            
            // Evaluate both standard boundary crossing AND immediate tick 0 static evaluation
            val crossedSlice = currentSlice != previousSlice || (previousPhase > 0.9 && phases[ch] < 0.1)
            val isFirstTick = !advancePhase && previousPhase == 0.0
            
            if (crossedSlice || isFirstTick) {
                // Determine if this geographic slice contains an active Hit according to Euclidean math
                // Condition: (sliceIndex * hits) % steps < hits
                val isHitSlice = ((currentSlice * h) % s) < h
                
                if (isHitSlice && shouldPlay) {
                    triggers[ch] = true
                }
            } else if (previousPhase == 0.0 && shouldPlay && s == 1 && advancePhase) { 
                // Edge case for single step continuing
                triggers[ch] = true
            }
        }
        return triggers
    }
    
    fun getTimeDivisionSymbol(channel: Int): String {
        return getTimeDivision(channel).symbol
    }
    fun getSteps(channel: Int): Int {
        if (channel !in 0..5) return 16
        return steps[channel]
    }

    fun getHits(channel: Int): Int {
        if (channel !in 0..5) return 4
        return hits[channel]
    }

    // --- Algorithmic Multi-Channel Generators ---

    /**
     * Steve Reich "Phase Drifter"
     * Same Euclidean pattern on all channels, but fractional scaling causes them to shear apart over time.
     */
    fun generateReich() {
        for (i in 0..5) {
            setStepsHits(i, 12, 7) // Classic Bembé / Bell pattern
            
            // Stagger phase division minutely (1.00, 1.01, 1.02...)
            val newDivisions = _timeDivisions.value.copyOf()
            newDivisions[i] = 1.0f + (i * 0.01f)
            _timeDivisions.value = newDivisions
            
            phases[i] = 0.0
        }
    }

    /**
     * Prime Interference
     * Sets the channels to the first 6 prime numbers. Resolves after 30,030 hits.
     */
    fun generatePrimes() {
        val primes = intArrayOf(2, 3, 5, 7, 11, 13)
        for (i in 0..5) {
            val p = primes[i]
            // We set hits to approximately half the prime to maintain density
            val h = (p / 2).coerceAtLeast(1)
            setStepsHits(i, p, h)
            setTimeDivision(i, TimeDivision.QUARTER)
            phases[i] = 0.0
        }
    }

    /**
     * Fibonacci Cascades
     * Uses the Fibonacci sequence for incredibly organic polyrhythms.
     */
    fun generateFibonacci() {
        val fibSteps = intArrayOf(3, 5, 8, 13, 21, 34)
        val fibHits = intArrayOf(2, 3, 5, 8, 13, 21) // Previous fib number
        for (i in 0..5) {
            setStepsHits(i, fibSteps[i], fibHits[i])
            setTimeDivision(i, TimeDivision.QUARTER)
            phases[i] = 0.0
        }
    }

    /**
     * Kotekan Interlock (Balinese Gamelan)
     * Channels are grouped in pairs (0-1, 2-3, 4-5).
     * The second in the pair has inverted phase to play perfectly between the first's notes.
     */
    fun generateKotekan() {
        // High density patterns for Kotekan
        setStepsHits(0, 16, 7)
        setStepsHits(1, 16, 7)
        
        setStepsHits(2, 12, 5)
        setStepsHits(3, 12, 5)
        
        setStepsHits(4, 8, 3)
        setStepsHits(5, 8, 3)

        for (i in 0..5) {
            setTimeDivision(i, TimeDivision.QUARTER)
            // Even channels start at 0.0, Odd channels start exactly halfway offset
            phases[i] = if (i % 2 == 0) 0.0 else 0.5
        }
    }
}
