package com.volcagrids.plaits

import androidx.compose.runtime.mutableStateListOf
import com.volcagrids.plaits.dsp.PlaitsPatch
import com.volcagrids.plaits.dsp.PlaitsModulations
import com.volcagrids.plaits.dsp.PlaitsVoice

/**
 * Plaits Track
 * Wrapper for a single Plaits voice instance with parameter control
 * Each track can be assigned to one of the 6 Grids parts or Poly mode
 */
class PlaitsTrack(
    val trackIndex: Int,
    val name: String = "Track ${trackIndex + 1}"
) {
    // Core synthesis voice
    val voice = PlaitsVoice()

    // Patch settings
    val patch = PlaitsPatch()

    // Modulation inputs
    val modulations = PlaitsModulations()

    // Track state
    var enabled = false
    var muted = false
    var solo = false

    // Trigger routing - which Grids parts trigger this Plaits track
    val triggerSources = mutableStateListOf<TriggerSource>()

    // Volume and pan
    var volume = 1.0f
    var pan = 0.0f  // -1.0 (left) to 1.0 (right)

    // Pitch/note settings
    var baseNote = 0.0f  // MIDI note offset
    var velocitySensitivity = 0.8f

    // Trigger state
    private var triggerGate = false

    // Last rendered audio for visualization
    var lastOutputLevel = 0.0f
        private set

    /**
     * Set engine type (0-23)
     */
    fun setEngine(index: Int) {
        patch.engine = index.coerceIn(0, PlaitsVoice.NUM_ENGINES - 1)
        voice.setEngine(patch.engine)
    }

    /**
     * Get current engine type
     */
    fun getEngine(): Int = voice.getActiveEngine()

    /**
     * Trigger note on with velocity
     */
    fun triggerOn(note: Float = 0.0f, velocity: Float = 0.8f) {
        triggerGate = true
        modulations.trigger = 1.0f
        modulations.level = velocity * velocitySensitivity
        modulations.note = note + baseNote
    }

    /**
     * Trigger note off
     */
    fun triggerOff() {
        triggerGate = false
        modulations.trigger = 0.0f
    }

    /**
     * Check if this track should be triggered by a source
     */
    fun isTriggeredBy(source: TriggerSource): Boolean {
        return triggerSources.contains(source)
    }

    /**
     * Add a trigger source
     */
    fun addTriggerSource(source: TriggerSource) {
        if (!triggerSources.contains(source)) {
            triggerSources.add(source)
        }
    }

    /**
     * Remove a trigger source
     */
    fun removeTriggerSource(source: TriggerSource) {
        triggerSources.remove(source)
    }

    /**
     * Clear all trigger sources
     */
    fun clearTriggerSources() {
        triggerSources.clear()
    }

    /**
     * Set core parameters
     */
    fun setParameters(
        harmonics: Float? = null,
        timbre: Float? = null,
        morph: Float? = null,
        decay: Float? = null,
        lpgColour: Float? = null
    ) {
        harmonics?.let { patch.harmonics = it.coerceIn(0.0f, 1.0f) }
        timbre?.let { patch.timbre = it.coerceIn(0.0f, 1.0f) }
        morph?.let { patch.morph = it.coerceIn(0.0f, 1.0f) }
        decay?.let { patch.decay = it.coerceIn(0.0f, 1.0f) }
        lpgColour?.let { patch.lpgColour = it.coerceIn(0.0f, 1.0f) }
    }

    /**
     * Set modulation amounts
     */
    fun setModulations(
        fmAmount: Float? = null,
        timbreModAmount: Float? = null,
        morphModAmount: Float? = null
    ) {
        fmAmount?.let { patch.frequencyModulationAmount = it.coerceIn(-1.0f, 1.0f) }
        timbreModAmount?.let { patch.timbreModulationAmount = it.coerceIn(-1.0f, 1.0f) }
        morphModAmount?.let { patch.morphModulationAmount = it.coerceIn(-1.0f, 1.0f) }
    }

    /**
     * Apply external modulation (from LFOs, envelopes, etc.)
     */
    fun applyModulation(
        note: Float? = null,
        harmonics: Float? = null,
        timbre: Float? = null,
        morph: Float? = null
    ) {
        note?.let { modulations.note = it }
        harmonics?.let { modulations.harmonics = it }
        timbre?.let { modulations.timbre = it }
        morph?.let { modulations.morph = it }
    }

    /**
     * Render audio block
     * @param frames Output buffer (interleaved stereo: L, R, L, R, ...)
     * @param size Number of stereo frames to render
     */
    fun render(frames: ShortArray, size: Int) {
        if (!enabled || muted) {
            // Output silence
            for (i in 0 until size * 2) {
                frames[i] = 0
            }
            lastOutputLevel = 0.0f
            return
        }

        // Render voice
        voice.render(patch, modulations, frames, size)

        // Apply volume and pan
        applyVolumeAndPan(frames, size)

        // Calculate output level for visualization
        calculateOutputLevel(frames, size)
    }

    private fun applyVolumeAndPan(frames: ShortArray, size: Int) {
        val leftGain = volume * (1.0f - pan.coerceAtLeast(0.0f))
        val rightGain = volume * (1.0f + pan.coerceAtMost(0.0f))

        for (i in 0 until size) {
            val leftIdx = i * 2
            val rightIdx = i * 2 + 1

            val leftSample = (frames[leftIdx].toInt() / 32767.0f * leftGain * 32767).toInt()
            val rightSample = (frames[rightIdx].toInt() / 32767.0f * rightGain * 32767).toInt()

            frames[leftIdx] = leftSample.coerceIn(-32768, 32767).toShort()
            frames[rightIdx] = rightSample.coerceIn(-32768, 32767).toShort()
        }
    }

    private fun calculateOutputLevel(frames: ShortArray, size: Int) {
        var sum = 0.0
        for (i in 0 until size * 2) {
            val normalized = frames[i].toDouble() / 32767.0
            sum += normalized * normalized
        }
        val rms = kotlin.math.sqrt(sum / (size * 2))
        lastOutputLevel = rms.toFloat()
    }

    /**
     * Reset track state
     */
    fun reset() {
        voice.reloadUserData()
        triggerOff()
        lastOutputLevel = 0.0f
    }

    /**
     * Get engine name for display
     */
    fun getEngineName(): String {
        val engineIndex = getEngine()
        return when (engineIndex) {
            0 -> "Virtual Analog"
            1 -> "FM"
            2 -> "Wavetable"
            3 -> "Noise"
            else -> "Engine $engineIndex"
        }
    }

    /**
     * Copy state from another track
     */
    fun copyFrom(other: PlaitsTrack) {
        patch.engine = other.patch.engine
        patch.harmonics = other.patch.harmonics
        patch.timbre = other.patch.timbre
        patch.morph = other.patch.morph
        patch.decay = other.patch.decay
        patch.lpgColour = other.patch.lpgColour
        voice.setEngine(patch.engine)
    }
}

/**
 * Engine type enumeration for easier selection
 */
enum class PlaitsEngineType(val index: Int, val displayName: String) {
    VIRTUAL_ANALOG(0, "Virtual Analog"),
    FM(1, "FM"),
    WAVETABLE(2, "Wavetable"),
    NOISE(3, "Noise"),
    // Additional engines would be added here
    ;

    companion object {
        fun fromIndex(index: Int): PlaitsEngineType {
            return values().find { it.index == index } ?: VIRTUAL_ANALOG
        }
    }
}

/**
 * Trigger source for Plaits tracks
 * Maps Grids engine parts to Plaits triggers
 */
enum class TriggerSource(val displayName: String) {
    ENGINE_A_PART1("Engine A - Part 1"),
    ENGINE_A_PART2("Engine A - Part 2"),
    ENGINE_A_PART3("Engine A - Part 3"),
    ENGINE_B_PART1("Engine B - Part 1"),
    ENGINE_B_PART2("Engine B - Part 2"),
    ENGINE_B_PART3("Engine B - Part 3"),
    POLYRHYTHM_1("Polyrhythm 1"),
    POLYRHYTHM_2("Polyrhythm 2"),
    POLYRHYTHM_3("Polyrhythm 3"),
    POLYRHYTHM_4("Polyrhythm 4"),
    POLYRHYTHM_5("Polyrhythm 5"),
    POLYRHYTHM_6("Polyrhythm 6"),
    MIDI_IN("MIDI Input"),
    INTERNAL("Internal Clock")
}
