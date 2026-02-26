package com.volcagrids

import android.content.Context
import android.content.SharedPreferences
import com.volcagrids.engine.LogicMode
import com.volcagrids.engine.OutputMode

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "v_GR1D_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        // MIDI Settings
        const val KEY_MIDI_OUTPUT_DEVICE_ID = "midi_output_device_id"
        const val KEY_MIDI_OUTPUT_PORT_INDEX = "midi_output_port_index"
        const val KEY_MIDI_INPUT_DEVICE_ID = "midi_input_device_id"
        const val KEY_MIDI_INPUT_PORT_INDEX = "midi_input_port_index"
        const val KEY_EXTERNAL_SYNC = "external_sync"

        // Engine A Settings
        const val KEY_ENGINE_A_X = "engine_a_x"
        const val KEY_ENGINE_A_Y = "engine_a_y"
        const val KEY_ENGINE_A_DENSITY_0 = "engine_a_density_0"
        const val KEY_ENGINE_A_DENSITY_1 = "engine_a_density_1"
        const val KEY_ENGINE_A_DENSITY_2 = "engine_a_density_2"
        const val KEY_ENGINE_A_RANDOMNESS = "engine_a_randomness"
        const val KEY_ENGINE_A_PART_MODE_0 = "engine_a_part_mode_0"
        const val KEY_ENGINE_A_PART_MODE_1 = "engine_a_part_mode_1"
        const val KEY_ENGINE_A_PART_MODE_2 = "engine_a_part_mode_2"
        const val KEY_ENGINE_A_LOGIC_MODE_0 = "engine_a_logic_mode_0"
        const val KEY_ENGINE_A_LOGIC_MODE_1 = "engine_a_logic_mode_1"
        const val KEY_ENGINE_A_LOGIC_MODE_2 = "engine_a_logic_mode_2"

        // Engine B Settings
        const val KEY_ENGINE_B_X = "engine_b_x"
        const val KEY_ENGINE_B_Y = "engine_b_y"
        const val KEY_ENGINE_B_DENSITY_0 = "engine_b_density_0"
        const val KEY_ENGINE_B_DENSITY_1 = "engine_b_density_1"
        const val KEY_ENGINE_B_DENSITY_2 = "engine_b_density_2"
        const val KEY_ENGINE_B_RANDOMNESS = "engine_b_randomness"
        const val KEY_ENGINE_B_PART_MODE_0 = "engine_b_part_mode_0"
        const val KEY_ENGINE_B_PART_MODE_1 = "engine_b_part_mode_1"
        const val KEY_ENGINE_B_PART_MODE_2 = "engine_b_part_mode_2"
        const val KEY_ENGINE_B_LOGIC_MODE_0 = "engine_b_logic_mode_0"
        const val KEY_ENGINE_B_LOGIC_MODE_1 = "engine_b_logic_mode_1"
        const val KEY_ENGINE_B_LOGIC_MODE_2 = "engine_b_logic_mode_2"

        // Global Settings
        const val KEY_BPM = "bpm"
        const val KEY_LINK_MODE = "link_mode"
        const val KEY_SWING = "swing"
        const val KEY_CLOCK_RESOLUTION = "clock_resolution"
        const val KEY_MORPH_FACTOR = "morph_factor"
        const val KEY_SCENE_A_INDEX = "scene_a_index"
        const val KEY_SCENE_B_INDEX = "scene_b_index"

        // Physics Settings
        const val KEY_PHYSICS_ENABLED = "physics_enabled"
        const val KEY_PHYSICS_FRICTION = "physics_friction"
        const val KEY_PHYSICS_ELASTIC = "physics_elastic"

        // Defaults
        const val DEFAULT_BPM = 120.0
        const val DEFAULT_DENSITY = 127
        const val DEFAULT_XY = 127
        const val DEFAULT_RANDOMNESS = 0
        const val DEFAULT_SWING = 50
        const val DEFAULT_MORPH_FACTOR = 0f
    }

    // MIDI Settings
    var midiOutputDeviceId: Int
        get() = prefs.getInt(KEY_MIDI_OUTPUT_DEVICE_ID, -1)
        set(value) = prefs.edit().putInt(KEY_MIDI_OUTPUT_DEVICE_ID, value).apply()

    var midiOutputPortIndex: Int
        get() = prefs.getInt(KEY_MIDI_OUTPUT_PORT_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_MIDI_OUTPUT_PORT_INDEX, value).apply()

    var midiInputDeviceId: Int
        get() = prefs.getInt(KEY_MIDI_INPUT_DEVICE_ID, -1)
        set(value) = prefs.edit().putInt(KEY_MIDI_INPUT_DEVICE_ID, value).apply()

    var midiInputPortIndex: Int
        get() = prefs.getInt(KEY_MIDI_INPUT_PORT_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_MIDI_INPUT_PORT_INDEX, value).apply()

    var externalSync: Boolean
        get() = prefs.getBoolean(KEY_EXTERNAL_SYNC, false)
        set(value) = prefs.edit().putBoolean(KEY_EXTERNAL_SYNC, value).apply()

    // Engine A Settings
    var engineAX: Int
        get() = prefs.getInt(KEY_ENGINE_A_X, DEFAULT_XY)
        set(value) = prefs.edit().putInt(KEY_ENGINE_A_X, value).apply()

    var engineAY: Int
        get() = prefs.getInt(KEY_ENGINE_A_Y, DEFAULT_XY)
        set(value) = prefs.edit().putInt(KEY_ENGINE_A_Y, value).apply()

    fun getEngineADensity(index: Int): Int {
        val key = when (index) {
            0 -> KEY_ENGINE_A_DENSITY_0
            1 -> KEY_ENGINE_A_DENSITY_1
            2 -> KEY_ENGINE_A_DENSITY_2
            else -> throw IllegalArgumentException("Invalid density index: $index")
        }
        return prefs.getInt(key, DEFAULT_DENSITY)
    }

    fun setEngineADensity(index: Int, value: Int) {
        val key = when (index) {
            0 -> KEY_ENGINE_A_DENSITY_0
            1 -> KEY_ENGINE_A_DENSITY_1
            2 -> KEY_ENGINE_A_DENSITY_2
            else -> throw IllegalArgumentException("Invalid density index: $index")
        }
        prefs.edit().putInt(key, value).apply()
    }

    var engineARandomness: Int
        get() = prefs.getInt(KEY_ENGINE_A_RANDOMNESS, DEFAULT_RANDOMNESS)
        set(value) = prefs.edit().putInt(KEY_ENGINE_A_RANDOMNESS, value).apply()

    fun getEngineAPartMode(index: Int): OutputMode {
        val key = when (index) {
            0 -> KEY_ENGINE_A_PART_MODE_0
            1 -> KEY_ENGINE_A_PART_MODE_1
            2 -> KEY_ENGINE_A_PART_MODE_2
            else -> throw IllegalArgumentException("Invalid part index: $index")
        }
        val name = prefs.getString(key, OutputMode.DRUMS.name) ?: OutputMode.DRUMS.name
        return OutputMode.valueOf(name)
    }

    fun setEngineAPartMode(index: Int, mode: OutputMode) {
        val key = when (index) {
            0 -> KEY_ENGINE_A_PART_MODE_0
            1 -> KEY_ENGINE_A_PART_MODE_1
            2 -> KEY_ENGINE_A_PART_MODE_2
            else -> throw IllegalArgumentException("Invalid part index: $index")
        }
        prefs.edit().putString(key, mode.name).apply()
    }

    fun getEngineALogicMode(index: Int): LogicMode {
        val key = when (index) {
            0 -> KEY_ENGINE_A_LOGIC_MODE_0
            1 -> KEY_ENGINE_A_LOGIC_MODE_1
            2 -> KEY_ENGINE_A_LOGIC_MODE_2
            else -> throw IllegalArgumentException("Invalid part index: $index")
        }
        val name = prefs.getString(key, LogicMode.NONE.name) ?: LogicMode.NONE.name
        return LogicMode.valueOf(name)
    }

    fun setEngineALogicMode(index: Int, mode: LogicMode) {
        val key = when (index) {
            0 -> KEY_ENGINE_A_LOGIC_MODE_0
            1 -> KEY_ENGINE_A_LOGIC_MODE_1
            2 -> KEY_ENGINE_A_LOGIC_MODE_2
            else -> throw IllegalArgumentException("Invalid part index: $index")
        }
        prefs.edit().putString(key, mode.name).apply()
    }

    // Engine B Settings
    var engineBX: Int
        get() = prefs.getInt(KEY_ENGINE_B_X, DEFAULT_XY)
        set(value) = prefs.edit().putInt(KEY_ENGINE_B_X, value).apply()

    var engineBY: Int
        get() = prefs.getInt(KEY_ENGINE_B_Y, DEFAULT_XY)
        set(value) = prefs.edit().putInt(KEY_ENGINE_B_Y, value).apply()

    fun getEngineBDensity(index: Int): Int {
        val key = when (index) {
            0 -> KEY_ENGINE_B_DENSITY_0
            1 -> KEY_ENGINE_B_DENSITY_1
            2 -> KEY_ENGINE_B_DENSITY_2
            else -> throw IllegalArgumentException("Invalid density index: $index")
        }
        return prefs.getInt(key, DEFAULT_DENSITY)
    }

    fun setEngineBDensity(index: Int, value: Int) {
        val key = when (index) {
            0 -> KEY_ENGINE_B_DENSITY_0
            1 -> KEY_ENGINE_B_DENSITY_1
            2 -> KEY_ENGINE_B_DENSITY_2
            else -> throw IllegalArgumentException("Invalid density index: $index")
        }
        prefs.edit().putInt(key, value).apply()
    }

    var engineBRandomness: Int
        get() = prefs.getInt(KEY_ENGINE_B_RANDOMNESS, DEFAULT_RANDOMNESS)
        set(value) = prefs.edit().putInt(KEY_ENGINE_B_RANDOMNESS, value).apply()

    fun getEngineBPartMode(index: Int): OutputMode {
        val key = when (index) {
            0 -> KEY_ENGINE_B_PART_MODE_0
            1 -> KEY_ENGINE_B_PART_MODE_1
            2 -> KEY_ENGINE_B_PART_MODE_2
            else -> throw IllegalArgumentException("Invalid part index: $index")
        }
        val name = prefs.getString(key, OutputMode.DRUMS.name) ?: OutputMode.DRUMS.name
        return OutputMode.valueOf(name)
    }

    fun setEngineBPartMode(index: Int, mode: OutputMode) {
        val key = when (index) {
            0 -> KEY_ENGINE_B_PART_MODE_0
            1 -> KEY_ENGINE_B_PART_MODE_1
            2 -> KEY_ENGINE_B_PART_MODE_2
            else -> throw IllegalArgumentException("Invalid part index: $index")
        }
        prefs.edit().putString(key, mode.name).apply()
    }

    fun getEngineBLogicMode(index: Int): LogicMode {
        val key = when (index) {
            0 -> KEY_ENGINE_B_LOGIC_MODE_0
            1 -> KEY_ENGINE_B_LOGIC_MODE_1
            2 -> KEY_ENGINE_B_LOGIC_MODE_2
            else -> throw IllegalArgumentException("Invalid part index: $index")
        }
        val name = prefs.getString(key, LogicMode.NONE.name) ?: LogicMode.NONE.name
        return LogicMode.valueOf(name)
    }

    fun setEngineBLogicMode(index: Int, mode: LogicMode) {
        val key = when (index) {
            0 -> KEY_ENGINE_B_LOGIC_MODE_0
            1 -> KEY_ENGINE_B_LOGIC_MODE_1
            2 -> KEY_ENGINE_B_LOGIC_MODE_2
            else -> throw IllegalArgumentException("Invalid part index: $index")
        }
        prefs.edit().putString(key, mode.name).apply()
    }

    // Global Settings
    var bpm: Double
        get() = prefs.getFloat(KEY_BPM, DEFAULT_BPM.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(KEY_BPM, value.toFloat()).apply()

    var linkMode: Boolean
        get() = prefs.getBoolean(KEY_LINK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_LINK_MODE, value).apply()

    var swing: Int
        get() = prefs.getInt(KEY_SWING, DEFAULT_SWING)
        set(value) = prefs.edit().putInt(KEY_SWING, value).apply()

    var clockResolution: Int
        get() = prefs.getInt(KEY_CLOCK_RESOLUTION, 2) // 24 PPQN default
        set(value) = prefs.edit().putInt(KEY_CLOCK_RESOLUTION, value).apply()

    var morphFactor: Float
        get() = prefs.getFloat(KEY_MORPH_FACTOR, DEFAULT_MORPH_FACTOR)
        set(value) = prefs.edit().putFloat(KEY_MORPH_FACTOR, value).apply()

    var sceneAIndex: Int
        get() = prefs.getInt(KEY_SCENE_A_INDEX, -1)
        set(value) = prefs.edit().putInt(KEY_SCENE_A_INDEX, value).apply()

    var sceneBIndex: Int
        get() = prefs.getInt(KEY_SCENE_B_INDEX, -1)
        set(value) = prefs.edit().putInt(KEY_SCENE_B_INDEX, value).apply()

    // Physics Settings
    var physicsEnabled: Boolean
        get() = prefs.getBoolean(KEY_PHYSICS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PHYSICS_ENABLED, value).apply()

    var physicsFriction: Float
        get() = prefs.getFloat(KEY_PHYSICS_FRICTION, 0.98f)
        set(value) = prefs.edit().putFloat(KEY_PHYSICS_FRICTION, value).apply()

    var physicsElastic: Float
        get() = prefs.getFloat(KEY_PHYSICS_ELASTIC, 0.9f)
        set(value) = prefs.edit().putFloat(KEY_PHYSICS_ELASTIC, value).apply()

    // Generic MIDI Trigger Settings (6 parts)
    fun getTriggerNote(channel: Int): Int {
        return prefs.getInt("trigger_note_$channel", 60) // Default all parts to C3 (60)
    }

    fun saveTriggerNote(channel: Int, note: Int) {
        prefs.edit().putInt("trigger_note_$channel", note).apply()
    }

    // Clear all preferences (factory reset)
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
