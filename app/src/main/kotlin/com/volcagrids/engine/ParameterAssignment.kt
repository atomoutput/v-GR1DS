package com.volcagrids.engine

import com.volcagrids.midi.VolcaParameter

/**
 * Parameter Assignment - Maps envelope generator to Volca Drum CC parameter
 */
data class ParameterAssignment(
    val partIndex: Int = 0,           // 0-5 (Volca parts)
    val ccNumber: Int = 51,           // CC number (e.g., 51 = DRIVE)
    val range: Int = 127,             // Modulation depth (0-127)
    val offset: Int = 0,              // Base value (0-127)
    val shape: EnvelopeShape = EnvelopeShape.SMOOTH,
    val enabled: Boolean = false
) {
    /**
     * Get VolcaParameter enum from CC number
     */
    fun getParameter(): VolcaParameter? {
        return VolcaParameter.entries.find { it.cc == ccNumber }
    }
    
    /**
     * Get display name for parameter
     */
    fun getDisplayName(): String {
        return getParameter()?.name?.replace("_", " ") ?: "CC $ccNumber"
    }
}

/**
 * Helper to create assignment from VolcaParameter
 */
fun createAssignment(
    partIndex: Int,
    parameter: VolcaParameter,
    range: Int = 127,
    offset: Int = 0,
    shape: EnvelopeShape = EnvelopeShape.SMOOTH
): ParameterAssignment {
    return ParameterAssignment(
        partIndex = partIndex,
        ccNumber = parameter.cc,
        range = range,
        offset = offset,
        shape = shape,
        enabled = true
    )
}
