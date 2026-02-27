package com.volcagrids.plaits.dsp.engines

import com.volcagrids.plaits.dsp.EngineParameters

/**
 * Post-processing settings for engines
 */
data class PostProcessingSettings(
    var outGain: Float = 1.0f,
    var auxGain: Float = 1.0f,
    var alreadyEnveloped: Boolean = false
)

/**
 * Base class for all Plaits synthesis engines
 * Each engine implements a different synthesis method
 */
abstract class Engine {
    val postProcessingSettings = PostProcessingSettings()

    /**
     * Initialize the engine with a buffer allocator
     */
    abstract fun init()

    /**
     * Reset engine state
     */
    abstract fun reset()

    /**
     * Load user data (samples, patches, etc.)
     */
    abstract fun loadUserData(userData: ByteArray?)

    /**
     * Render audio
     * @param parameters Engine parameters (note, timbre, morph, etc.)
     * @param out Output buffer (main audio)
     * @param aux Output buffer (auxiliary audio)
     * @param size Number of samples to render
     * @param alreadyEnveloped Output: whether the signal already has an envelope
     */
    abstract fun render(
        parameters: EngineParameters,
        out: FloatArray,
        aux: FloatArray,
        size: Int,
        alreadyEnveloped: BooleanRef
    )
}

/**
 * Simple reference holder for boolean output parameter
 */
class BooleanRef(var value: Boolean = false)
