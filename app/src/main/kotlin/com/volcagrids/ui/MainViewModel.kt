package com.volcagrids.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import com.volcagrids.AppPreferences
import com.volcagrids.engine.LogicMode
import com.volcagrids.engine.OutputMode
import com.volcagrids.engine.EnvelopeShape
import com.volcagrids.engine.ParameterAssignment
import com.volcagrids.engine.EnvelopeSequencer
import com.volcagrids.engine.PolyrhythmEngine
import com.volcagrids.midi.MidiSequencerService
import com.volcagrids.midi.VolcaParameter
import com.volcagrids.ui.components.*
import com.volcagrids.plaits.PlaitsTrack
import com.volcagrids.audio.PlaitsAudioEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

/**
 * Sequencer Mode - DRUMS (Grids), EUCLIDEAN, or POLYRHYTHM
 */
enum class SequencerMode {
    DRUMS,
    EUCLIDEAN,
    POLYRHYTHM
}

private const val TAG = "MainViewModel"

@androidx.compose.foundation.ExperimentalFoundationApi
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)
    
    var service: MidiSequencerService? = null

    var engineAX by mutableStateOf(prefs.engineAX)
    var engineAY by mutableStateOf(prefs.engineAY)
    var engineBX by mutableStateOf(prefs.engineBX)
    var engineBY by mutableStateOf(prefs.engineBY)
    
    // Flag to prevent infinite recursion in link mode
    @Volatile private var isUpdatingLinkedEngine = false

    val densitiesA = mutableStateListOf(
        prefs.getEngineADensity(0),
        prefs.getEngineADensity(1),
        prefs.getEngineADensity(2)
    )
    val densitiesB = mutableStateListOf(
        prefs.getEngineBDensity(0),
        prefs.getEngineBDensity(1),
        prefs.getEngineBDensity(2)
    )

    val logicModesA = mutableStateListOf(
        prefs.getEngineALogicMode(0),
        prefs.getEngineALogicMode(1),
        prefs.getEngineALogicMode(2)
    )
    val logicModesB = mutableStateListOf(
        prefs.getEngineBLogicMode(0),
        prefs.getEngineBLogicMode(1),
        prefs.getEngineBLogicMode(2)
    )

    val partModesA = mutableStateListOf(
        prefs.getEngineAPartMode(0),
        prefs.getEngineAPartMode(1),
        prefs.getEngineAPartMode(2)
    )
    val partModesB = mutableStateListOf(
        prefs.getEngineBPartMode(0),
        prefs.getEngineBPartMode(1),
        prefs.getEngineBPartMode(2)
    )

    var morphFactor by mutableStateOf(prefs.morphFactor)
    var isPlaying by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var showMidiConfig by mutableStateOf(false)
    var isExtSync by mutableStateOf(prefs.externalSync)
    var linkMode by mutableStateOf(prefs.linkMode)
    var swing by mutableStateOf(prefs.swing)
    var clockResolution by mutableStateOf(prefs.clockResolution)
    var bpm by mutableStateOf(prefs.bpm)

    // Volca Sound Design State
    var waveguideDecay by mutableStateOf(64)
    var waveguideBody by mutableStateOf(64)
    var waveguideTune by mutableStateOf(64)
    val morphValues = mutableStateListOf(64, 64, 64, 64, 64, 64)
    var showVolcaDrawer by mutableStateOf(false)

    // Physics State - velocity stored separately to avoid sync issues
    // NOTE: Physics impulses now go directly to service engine (Fix 2.1)
    var physicsEnabled by mutableStateOf(false)
    var physicsFriction by mutableStateOf(0.98f)
    var physicsElasticity by mutableStateOf(0.9f)

    // Gesture Recording State
    var isRecordingGesture by mutableStateOf(false)
    var isPlayingGesture by mutableStateOf(false)
    var currentGesture by mutableStateOf<RecordedGesture?>(null)
    val recordedGestures = mutableStateListOf<RecordedGesture>()
    var selectedGestureIndex by mutableStateOf(-1)
    var gesturePlaybackProgress by mutableStateOf(0f)
    private var gestureStartTime = 0L
    private val gesturePoints = mutableListOf<GesturePoint>()

    // LFO State
    val lfos = mutableStateListOf(
        LFOConfig(0, LFOWaveform.SINE, 1f, 0.5f, 0f, LFODestination.ENGINE_A_X, null, false),
        LFOConfig(1, LFOWaveform.TRIANGLE, 0.5f, 0.3f, 0.25f, LFODestination.ENGINE_A_Y, null, false),
        LFOConfig(2, LFOWaveform.SAW, 0.25f, 0.2f, 0.5f, LFODestination.ENGINE_B_X, null, false),
        LFOConfig(3, LFOWaveform.SQUARE, 0.1f, 0.4f, 0.75f, LFODestination.ENGINE_B_Y, null, false)
    )
    private var lfoPhase = 0f

    // Euclidean S&H State
    val euclideanSHConfigs = mutableStateListOf(
        EuclideanSHConfig(16, 5, VolcaParameter.DRIVE, 0, 127, 0, false),
        EuclideanSHConfig(16, 3, VolcaParameter.BIT_REDUCTION, 0, 127, 5, false)
    )

    // Sequencer Mode: DRUMS, EUCLIDEAN, or POLYRHYTHM
    private var _sequencerMode = mutableStateOf(SequencerMode.DRUMS)
    var sequencerMode: SequencerMode
        get() = _sequencerMode.value
        set(value) {
            _sequencerMode.value = value
            // CRITICAL FIX: Use let to ensure service is not null before accessing
            service?.let { srv ->
                if (value == SequencerMode.POLYRHYTHM) {
                    // Entering POLY mode: enable polyrhythm, disable Grids
                    polyrhythmPlaying = isPlaying
                    srv.polyrhythmPlaying = isPlaying
                    srv.polyrhythmEngine.reset()
                } else {
                    // Leaving POLY mode: disable polyrhythm, enable Grids
                    polyrhythmPlaying = false
                    srv.polyrhythmPlaying = false
                    srv.polyrhythmEngine.reset()
                }
            } ?: android.util.Log.w(TAG, "Service not bound when changing sequencer mode to $value")
        }
    
    
    // Generic MIDI Trigger Notes (6 Channels)
    val triggerNotes = mutableStateListOf(
        prefs.getTriggerNote(0),
        prefs.getTriggerNote(1),
        prefs.getTriggerNote(2),
        prefs.getTriggerNote(3),
        prefs.getTriggerNote(4),
        prefs.getTriggerNote(5)
    )

    fun updateTriggerNote(channel: Int, note: Int) {
        if (channel in 0..5) {
            triggerNotes[channel] = note
            prefs.saveTriggerNote(channel, note)
            service?.midiManager?.triggerNotes?.set(channel, note)
        }
    }

    // Polyrhythm State - ALL operations go through service engine
    var showRatioSelector by mutableStateOf(false)
    var currentPolyrhythmRatio by mutableStateOf("3:2")
    var isPolyrhythmMuteA by mutableStateOf(false)
    var isPolyrhythmMuteB by mutableStateOf(false)
    var isPolyrhythmSoloA by mutableStateOf(false)
    var isPolyrhythmSoloB by mutableStateOf(false)
    var polyrhythmPlaying by mutableStateOf(false)

    // Parameter Sequencer State
    var showParameterSequencer by mutableStateOf(false)
    
    // Advanced Overlays State
    var showModMatrix by mutableStateOf(false)
    var showPerformanceLayer by mutableStateOf(false)
    var showSystemTools by mutableStateOf(false)

    // Mod Matrix Data State
    var lfoConfigs = mutableStateListOf(
        com.volcagrids.ui.components.LFOConfig(0),
        com.volcagrids.ui.components.LFOConfig(1),
        com.volcagrids.ui.components.LFOConfig(2),
        com.volcagrids.ui.components.LFOConfig(3)
    )
    var modulationSlots = mutableStateListOf<com.volcagrids.ui.components.ModulationSlot>(
        com.volcagrids.ui.components.ModulationSlot(
            partIndex = 0, 
            slotIndex = 0, 
            source = com.volcagrids.ui.components.ModSource.LEVEL_BD, 
            destination = com.volcagrids.midi.VolcaParameter.DRIVE, 
            amount = 96, 
            enabled = true
        ),
        com.volcagrids.ui.components.ModulationSlot(
            partIndex = 5, 
            slotIndex = 0, 
            source = com.volcagrids.ui.components.ModSource.LEVEL_HH, 
            destination = com.volcagrids.midi.VolcaParameter.BIT_REDUCTION, 
            amount = 96, 
            enabled = true
        )
    )

    // Performance Macros
    private var _crossLogicMode = mutableStateOf(0)
    var crossLogicMode: Int
        get() = _crossLogicMode.value
        set(value) {
            _crossLogicMode.value = value
            service?.crossLogicMode = value
        }

    private var _glitchAmount = mutableStateOf(0f)
    var glitchAmount: Float
        get() = _glitchAmount.value
        set(value) {
            _glitchAmount.value = value
            service?.glitchAmount = value
        }

    private var _globalProbability = mutableStateOf(1f)
    var globalProbability: Float
        get() = _globalProbability.value
        set(value) {
            _globalProbability.value = value
            service?.globalProbability = value
        }

    var currentParamPart by mutableStateOf(0)
    var paramLinkedToMain by mutableStateOf(true)  // Link XY to main sequencer
    
    // Separate state for UI (since EnvelopeSequencer properties aren't observable)
    var paramEnvelopeX by mutableStateOf(127)
    var paramEnvelopeY by mutableStateOf(127)
    var paramEnvelopeValue by mutableStateOf(64)
    var paramEnvelopeRange by mutableStateOf(127)
    var paramEnvelopeOffset by mutableStateOf(0)
    var paramEnvelopeShape by mutableStateOf(EnvelopeShape.SMOOTH)
    var paramEnvelopeCC by mutableStateOf(51)  // Default to DRIVE
    
    // Real-time modulation visualization state
    var modulationPreviewValues = mutableStateListOf<Float>(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
    var modulationTargetValues = mutableStateListOf<Float>(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
    var modulationStepPositions = mutableStateListOf<Float>(0f, 0f, 0f, 0f, 0f, 0f)
    var showModulationPreview by mutableStateOf(-1)  // Index of part showing preview, -1 = hidden

    val paramAssignments = mutableStateListOf(
        ParameterAssignment(0, 51, 127, 0, EnvelopeShape.SMOOTH, false),  // DRIVE
        ParameterAssignment(1, 49, 100, 20, EnvelopeShape.SMOOTH, false),  // BIT_REDUCTION
        ParameterAssignment(2, 117, 80, 40, EnvelopeShape.EXPONENTIAL, false),  // DECAY
        ParameterAssignment(3, 118, 60, 30, EnvelopeShape.SMOOTH, false),  // BODY
        ParameterAssignment(4, 26, 50, 64, EnvelopeShape.STEPPED, false),  // PITCH
        ParameterAssignment(5, 29, 40, 64, EnvelopeShape.RANDOM, false)  // MOD_AMOUNT
    )
    val paramEnvelopeSequencers = List(6) { EnvelopeSequencer() }

    // Plaits Synthesizer Integration
    val plaitsAudioEngine by lazy { PlaitsAudioEngine() }
    val plaitsTracks = mutableStateListOf(
        PlaitsTrack(0, "Part 1"),
        PlaitsTrack(1, "Part 2"),
        PlaitsTrack(2, "Part 3"),
        PlaitsTrack(3, "Part 4"),
        PlaitsTrack(4, "Part 5"),
        PlaitsTrack(5, "Part 6"),
        PlaitsTrack(6, "Poly")  // For polyrhythm mode
    )
    var plaitsEnabled by mutableStateOf(false)
    var currentPlaitsTrackIndex by mutableStateOf(0)
    var showPlaitsOverlay by mutableStateOf(false)
    
    // Update UI state from sequencer
    fun updateParamUIState(part: Int) {
        paramEnvelopeX = paramEnvelopeSequencers[part].x
        paramEnvelopeY = paramEnvelopeSequencers[part].y
        paramEnvelopeValue = paramEnvelopeSequencers[part].getValue()
        paramEnvelopeRange = paramAssignments[part].range
        paramEnvelopeOffset = paramAssignments[part].offset
        paramEnvelopeShape = paramAssignments[part].shape
        paramEnvelopeCC = paramAssignments[part].ccNumber
    }
    
    // Update real-time modulation visualization (called from sequencer thread via service)
    fun updateModulationVisualization(part: Int, currentValue: Int, targetValue: Int, step: Int) {
        if (part in 0..5) {
            modulationPreviewValues[part] = currentValue / 127f
            modulationTargetValues[part] = targetValue / 127f
            modulationStepPositions[part] = step / 32f
        }
    }

    // Sync envelope sequencer positions with engine positions (only if linked)
    fun syncParamSequencerPositions() {
        if (paramLinkedToMain) {
            paramEnvelopeSequencers[0].setPosition(engineAX, engineAY)
            paramEnvelopeSequencers[1].setPosition(engineBX, engineBY)
            paramEnvelopeSequencers[2].setPosition(engineAX, engineAY)
            paramEnvelopeSequencers[3].setPosition(engineBX, engineBY)
            paramEnvelopeSequencers[4].setPosition(engineAX, engineAY)
            paramEnvelopeSequencers[5].setPosition(engineBX, engineBY)
        }
        updateParamUIState(currentParamPart)
    }

    // Scene State
    val scenes = mutableStateListOf<SceneData>()
    var currentSceneA by mutableStateOf(-1)
    var currentSceneB by mutableStateOf(-1)

    // MIDI State
    var midiOutputDeviceId: Int
        get() = prefs.midiOutputDeviceId
        set(value) { prefs.midiOutputDeviceId = value }
    
    var midiOutputPortIndex: Int
        get() = prefs.midiOutputPortIndex
        set(value) { prefs.midiOutputPortIndex = value }
    
    var midiInputDeviceId: Int
        get() = prefs.midiInputDeviceId
        set(value) { prefs.midiInputDeviceId = value }
    
    var midiInputPortIndex: Int
        get() = prefs.midiInputPortIndex
        set(value) { prefs.midiInputPortIndex = value }

    val midiDevices = mutableStateListOf<android.media.midi.MidiDeviceInfo>()

    fun refreshDevices(context: android.content.Context) {
        val discovery = com.volcagrids.midi.MidiDiscovery(context)
        midiDevices.clear()
        midiDevices.addAll(discovery.getAvailableDevices())
    }

    fun connectDevice(context: android.content.Context, info: android.media.midi.MidiDeviceInfo) {
        val discovery = com.volcagrids.midi.MidiDiscovery(context)
        discovery.openDevice(info) { device ->
            service?.connectOutput(device, prefs.midiOutputPortIndex)
            service?.connectInputSync(device, prefs.midiInputPortIndex)
            prefs.midiOutputDeviceId = info.id
        }
    }

    fun onServiceConnected(srv: MidiSequencerService) {
        service = srv
        srv.setBPM(bpm)
        srv.setExternalSync(isExtSync)

        // CRITICAL: Ensure polyrhythm is DISABLED on startup (default mode is DRUMS)
        srv.polyrhythmPlaying = false
        srv.polyrhythmEngine.reset()

        // Sync current state to service
        srv.morphFactor = morphFactor
        srv.crossLogicMode = crossLogicMode
        srv.globalProbability = globalProbability
        srv.glitchAmount = glitchAmount

        srv.engineA.x = engineAX
        srv.engineA.y = engineAY
        srv.engineB.x = engineBX
        srv.engineB.y = engineBY
        densitiesA.forEachIndexed { i, d -> srv.engineA.densities[i] = d }
        densitiesB.forEachIndexed { i, d -> srv.engineB.densities[i] = d }
        logicModesA.forEachIndexed { i, m -> srv.logicModesA[i] = m }
        logicModesB.forEachIndexed { i, m -> srv.logicModesB[i] = m }
        partModesA.forEachIndexed { i, m -> srv.engineA.partModes[i] = m }
        partModesB.forEachIndexed { i, m -> srv.engineB.partModes[i] = m }

        // Initialize Plaits audio engine
        if (plaitsEnabled) {
            initPlaits()
        }
    }

    // Plaits Methods
    fun initPlaits() {
        plaitsAudioEngine.init()
        plaitsTracks.forEach { track ->
            plaitsAudioEngine.addTrack(track)
        }
        plaitsAudioEngine.start()

        // Connect Plaits tracks to service for trigger dispatch
        service?.plaitsTracks = plaitsTracks
    }

    fun togglePlaits(enabled: Boolean) {
        plaitsEnabled = enabled
        if (enabled && service != null) {
            initPlaits()
        } else {
            plaitsAudioEngine.stop()
        }
    }

    fun setCurrentPlaitsTrack(index: Int) {
        currentPlaitsTrackIndex = index.coerceIn(0, 6)
    }

    fun getCurrentPlaitsTrack(): PlaitsTrack? {
        return plaitsTracks.getOrNull(currentPlaitsTrackIndex)
    }

    fun setPlaitsEngine(trackIndex: Int, engineIndex: Int) {
        plaitsTracks.getOrNull(trackIndex)?.setEngine(engineIndex)
    }

    fun setPlaitsParameters(
        trackIndex: Int,
        harmonics: Float? = null,
        timbre: Float? = null,
        morph: Float? = null,
        decay: Float? = null
    ) {
        plaitsTracks.getOrNull(trackIndex)?.setParameters(harmonics, timbre, morph, decay)
    }

    fun triggerPlaitsTrack(trackIndex: Int, note: Float = 0.0f, velocity: Float = 0.8f) {
        if (plaitsEnabled) {
            plaitsTracks.getOrNull(trackIndex)?.triggerOn(note, velocity)
        }
    }

    fun releasePlaitsTrack(trackIndex: Int) {
        if (plaitsEnabled) {
            plaitsTracks.getOrNull(trackIndex)?.triggerOff()
        }
    }

    fun toggleExtSync(enabled: Boolean) {
        isExtSync = enabled
        prefs.externalSync = enabled
        service?.setExternalSync(enabled)
    }

    fun updateEngineA(x: Int, y: Int) {
        // Prevent infinite recursion in link mode
        if (isUpdatingLinkedEngine) return
        
        isUpdatingLinkedEngine = true
        try {
            engineAX = x
            engineAY = y
            prefs.engineAX = x
            prefs.engineAY = y
            service?.engineA?.x = x
            service?.engineA?.y = y

            // Link mode - update engine B proportionally
            if (linkMode) {
                engineBX = x
                engineBY = y
                prefs.engineBX = x
                prefs.engineBY = y
                service?.engineB?.x = x
                service?.engineB?.y = y
            }
        } finally {
            isUpdatingLinkedEngine = false
        }
    }

    fun updateEngineB(x: Int, y: Int) {
        // Prevent infinite recursion in link mode
        if (isUpdatingLinkedEngine) return
        
        isUpdatingLinkedEngine = true
        try {
            engineBX = x
            engineBY = y
            prefs.engineBX = x
            prefs.engineBY = y
            service?.engineB?.x = x
            service?.engineB?.y = y

            // Link mode - update engine A proportionally
            if (linkMode) {
                engineAX = x
                engineAY = y
                prefs.engineAX = x
                prefs.engineAY = y
                service?.engineA?.x = x
                service?.engineA?.y = y
            }
        } finally {
            isUpdatingLinkedEngine = false
        }
    }

    fun updateDensity(engineIndex: Int, partIndex: Int, value: Int) {
        if (engineIndex == 0) {
            densitiesA[partIndex] = value
            service?.engineA?.densities?.set(partIndex, value)
            prefs.setEngineADensity(partIndex, value)
        } else {
            densitiesB[partIndex] = value
            service?.engineB?.densities?.set(partIndex, value)
            prefs.setEngineBDensity(partIndex, value)
        }
    }

    fun cycleLogic(engineIndex: Int, partIndex: Int) {
        if (partIndex !in 0..2) return
        val currentList = if (engineIndex == 0) logicModesA else logicModesB
        val nextMode = when (currentList[partIndex]) {
            LogicMode.NONE -> LogicMode.NOT_KICK
            LogicMode.NOT_KICK -> LogicMode.NOT_SNARE
            LogicMode.NOT_SNARE -> LogicMode.AND_HH
            LogicMode.AND_HH -> LogicMode.XOR_PREV
            LogicMode.XOR_PREV -> LogicMode.NONE
        }
        currentList[partIndex] = nextMode
        if (engineIndex == 0) prefs.setEngineALogicMode(partIndex, nextMode)
        else prefs.setEngineBLogicMode(partIndex, nextMode)
        service?.let {
            if (engineIndex == 0) it.logicModesA[partIndex] = nextMode
            else it.logicModesB[partIndex] = nextMode
        }
    }

    fun cycleMode(engineIndex: Int, partIndex: Int) {
        if (partIndex !in 0..2) return
        val currentList = if (engineIndex == 0) partModesA else partModesB
        val nextMode = if (currentList[partIndex] == OutputMode.DRUMS)
            OutputMode.EUCLIDEAN
        else OutputMode.DRUMS
        currentList[partIndex] = nextMode
        if (engineIndex == 0) prefs.setEngineAPartMode(partIndex, nextMode)
        else prefs.setEngineBPartMode(partIndex, nextMode)
        service?.let {
            if (engineIndex == 0) it.engineA.partModes[partIndex] = nextMode
            else it.engineB.partModes[partIndex] = nextMode
        }
    }

    fun togglePlay() {
        isPlaying = !isPlaying
        service?.let { svc ->
            if (isPlaying) {
                svc.start()
            } else {
                svc.stop()
            }
            // Sync polyrhythm state
            svc.polyrhythmPlaying = (sequencerMode == SequencerMode.POLYRHYTHM && isPlaying)
        }
    }

    fun setBPM(newBpm: Double) {
        bpm = newBpm.coerceIn(30.0, 300.0)  // Validate BPM range
        prefs.bpm = bpm
        service?.setBPM(bpm)
    }

    fun toggleLinkMode() {
        linkMode = !linkMode
        prefs.linkMode = linkMode
    }

    // Volca Sound Design Methods
    fun setWaveguide(decay: Int, body: Int, tune: Int) {
        waveguideDecay = decay
        waveguideBody = body
        waveguideTune = tune
        service?.midiManager?.setResonator(decay, body)
            ?: Log.w(TAG, "setWaveguide: Service not bound, MIDI command not sent")
    }

    fun setMorphValue(partIndex: Int, value: Int) {
        if (partIndex in 0..5) {
            morphValues[partIndex] = value
            service?.midiManager?.setLayerBalance(partIndex, value)
                ?: Log.w(TAG, "setMorphValue: Service not bound, MIDI command not sent")
        }
    }

    fun toggleVolcaDrawer() {
        showVolcaDrawer = !showVolcaDrawer
    }

    // Tap Tempo
    private var lastTapTime = 0L
    private var tapIntervalSum = 0L
    private var tapCount = 0

    fun onTapTempo() {
        val now = System.currentTimeMillis()
        if (lastTapTime > 0) {
            val interval = now - lastTapTime
            if (interval in 150..2000) {
                tapIntervalSum += interval
                tapCount++
                if (tapCount >= 3) {
                    val avgInterval = tapIntervalSum / tapCount
                    val newBpm = (60000.0 / avgInterval).coerceIn(30.0, 300.0)
                    setBPM(newBpm)
                    tapIntervalSum = 0
                    tapCount = 0
                }
            } else {
                tapIntervalSum = 0
                tapCount = 0
            }
        }
        lastTapTime = now
    }

    // Gesture Recording and Playback
    private val maxGesturePoints = 1000  // Prevent OOM
    private var gesturePlaybackJob: Job? = null
    
    fun startGestureRecording() {
        isRecordingGesture = true
        gesturePoints.clear()
        gestureStartTime = System.currentTimeMillis()
    }

    fun addGesturePoint(x: Float, y: Float) {
        if (isRecordingGesture) {
            if (gesturePoints.size >= maxGesturePoints) {
                gesturePoints.removeAt(0)  // Ring buffer
            }
            gesturePoints.add(GesturePoint(x, y, System.currentTimeMillis() - gestureStartTime))
        }
    }

    fun stopGestureRecording() {
        isRecordingGesture = false
        if (gesturePoints.isNotEmpty()) {
            val duration = gesturePoints.last().timestamp
            currentGesture = RecordedGesture(gesturePoints.toList(), duration, "Gesture_${recordedGestures.size + 1}")
        }
    }

    fun playGesture() {
        currentGesture?.let { gesture ->
            if (gesture.points.isEmpty()) return
            isPlayingGesture = true
            gesturePlaybackJob?.cancel()  // Cancel any existing playback

            // CRITICAL FIX: Use viewModelScope to prevent memory leaks
            // When ViewModel is cleared, all coroutines are automatically cancelled
            gesturePlaybackJob = viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                try {
                    while (isPlayingGesture) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val progress = (elapsed.toFloat() / gesture.duration).coerceIn(0f, 1f)
                        gesturePlaybackProgress = progress

                        // Find current point in gesture
                        val currentIndex = (progress * (gesture.points.size - 1)).toInt()
                            .coerceIn(0, gesture.points.size - 1)
                        val point = gesture.points[currentIndex]

                        // Apply gesture position to engine A
                        updateEngineA(point.x.toInt(), point.y.toInt())

                        if (progress >= 1f) {
                            // Loop: restart
                            gesturePlaybackProgress = 0f
                        }

                        kotlinx.coroutines.delay(16)  // ~60fps
                    }
                } finally {
                    // Cleanup when coroutine completes or is cancelled
                    isPlayingGesture = false
                    gesturePlaybackProgress = 0f
                }
            }
        }
    }

    fun stopGesture() {
        isPlayingGesture = false
        gesturePlaybackProgress = 0f
        gesturePlaybackJob?.cancel()
        gesturePlaybackJob = null
    }

    fun saveCurrentGesture() {
        currentGesture?.let {
            recordedGestures.add(it)
        }
    }

    fun clearGestures() {
        recordedGestures.clear()
        currentGesture = null
        stopGesture()
    }

    // LFO Processing
    fun updateLFOs(deltaTime: Float) {
        lfoPhase = (lfoPhase + deltaTime) % 1f  // Wrap to 0-1 range to prevent precision loss
        
        lfos.forEach { lfo ->
            if (!lfo.enabled) return@forEach
            
            val value = calculateLFOValue(lfo.waveform, lfoPhase * lfo.rate + lfo.phase)
            val modulatedValue = value * lfo.depth
            
            when (lfo.destination) {
                LFODestination.ENGINE_A_X -> {
                    val newX = ((127 + modulatedValue * 127).toInt().coerceIn(0, 255))
                    if (newX != engineAX) updateEngineA(newX, engineAY)
                }
                LFODestination.ENGINE_A_Y -> {
                    val newY = ((127 + modulatedValue * 127).toInt().coerceIn(0, 255))
                    if (newY != engineAY) updateEngineA(engineAX, newY)
                }
                LFODestination.ENGINE_B_X -> {
                    val newX = ((127 + modulatedValue * 127).toInt().coerceIn(0, 255))
                    if (newX != engineBX) updateEngineB(newX, engineBY)
                }
                LFODestination.ENGINE_B_Y -> {
                    val newY = ((127 + modulatedValue * 127).toInt().coerceIn(0, 255))
                    if (newY != engineBY) updateEngineB(engineBX, newY)
                }
                else -> {}
            }
        }
    }

    private fun calculateLFOValue(waveform: LFOWaveform, phase: Float): Float {
        val t = phase * 2 * Math.PI
        return when (waveform) {
            LFOWaveform.SINE -> Math.sin(t).toFloat()
            LFOWaveform.TRIANGLE -> (2 * Math.abs((t / (2 * Math.PI)) % 2 - 1) - 1).toFloat()
            LFOWaveform.SAW -> (2 * ((t / (2 * Math.PI)) % 2) - 1).toFloat()
            LFOWaveform.SQUARE -> if (Math.sin(t) > 0) 1f else -1f
            LFOWaveform.S_AND_H -> (Math.random().toFloat() * 2 - 1)
            LFOWaveform.STEP -> ((phase * 8).toInt() % 8) / 4f - 1f
        }
    }

    // Scene Management
    fun saveScene(index: Int) {
        val scene = SceneData(
            index = index,
            name = "Scene_${index + 1}",
            engineAX = engineAX,
            engineAY = engineAY,
            engineBX = engineBX,
            engineBY = engineBY,
            densitiesA = densitiesA.toList(),
            densitiesB = densitiesB.toList(),
            randomnessA = service?.engineA?.randomness ?: 0,
            randomnessB = service?.engineB?.randomness ?: 0,
            isSaved = true
        )
        
        if (scenes.any { it.index == index }) {
            val i = scenes.indexOfFirst { it.index == index }
            scenes[i] = scene
        } else {
            scenes.add(scene)
        }
    }

    fun loadSceneA(index: Int) {
        val scene = scenes.find { it.index == index } ?: return
        currentSceneA = index
        engineAX = scene.engineAX
        engineAY = scene.engineAY
        densitiesA.clear()
        densitiesA.addAll(scene.densitiesA)
        service?.engineA?.x = scene.engineAX
        service?.engineA?.y = scene.engineAY
        scene.densitiesA.forEachIndexed { i, d -> service?.engineA?.densities?.set(i, d) }
    }

    fun loadSceneB(index: Int) {
        val scene = scenes.find { it.index == index } ?: return
        currentSceneB = index
        engineBX = scene.engineBX
        engineBY = scene.engineBY
        densitiesB.clear()
        densitiesB.addAll(scene.densitiesB)
        service?.engineB?.x = scene.engineBX
        service?.engineB?.y = scene.engineBY
        scene.densitiesB.forEachIndexed { i, d -> service?.engineB?.densities?.set(i, d) }
    }

    fun deleteScene(index: Int) {
        scenes.removeAll { it.index == index }
        if (currentSceneA == index) currentSceneA = -1
        if (currentSceneB == index) currentSceneB = -1
    }

    // Physics methods removed - physics now handled entirely by service engine (Fix 2.1)
    // applyPhysicsToEngineA/B and applyPhysicsImpulseA/B removed

    // Parameter Sequencer Methods
    fun openParameterSequencer(partIndex: Int) {
        currentParamPart = partIndex
        showParameterSequencer = true
        syncParamSequencerPositions()
        updateParamUIState(partIndex)
    }

    fun closeParameterSequencer() {
        showParameterSequencer = false
    }

    // Advanced Overlays Toggles
    fun toggleModMatrix() {
        showModMatrix = !showModMatrix
    }

    fun togglePerformanceLayer() {
        showPerformanceLayer = !showPerformanceLayer
    }

    fun toggleSystemTools() {
        showSystemTools = !showSystemTools
    }

    // Mod Matrix Updates
    fun updateLFO(lfo: com.volcagrids.ui.components.LFOConfig) {
        val index = lfoConfigs.indexOfFirst { it.index == lfo.index }
        if (index != -1) {
            lfoConfigs[index] = lfo
        }
    }

    fun updateModulationSlot(slot: com.volcagrids.ui.components.ModulationSlot) {
        val existingIndex = modulationSlots.indexOfFirst { it.partIndex == slot.partIndex && it.slotIndex == slot.slotIndex }
        if (existingIndex != -1) {
            modulationSlots[existingIndex] = slot
        } else {
            modulationSlots.add(slot)
        }
    }

    fun toggleParamLink() {
        paramLinkedToMain = !paramLinkedToMain
        if (paramLinkedToMain) {
            // When linking, sync to current main position
            syncParamSequencerPositions()
        }
    }

    fun updateParamAssignment(partIndex: Int, assignment: ParameterAssignment) {
        if (partIndex in 0..5) {
            paramAssignments[partIndex] = assignment
            paramEnvelopeSequencers[partIndex].enabled = assignment.enabled
            paramEnvelopeSequencers[partIndex].shape = assignment.shape
            paramEnvelopeSequencers[partIndex].range = assignment.range
            paramEnvelopeSequencers[partIndex].offset = assignment.offset
            updateParamUIState(partIndex)
        }
    }

    fun updateParamCC(cc: Int) {
        paramEnvelopeCC = cc
        val current = paramAssignments[currentParamPart]
        updateParamAssignment(
            currentParamPart,
            current.copy(ccNumber = cc, enabled = true)
        )
    }

    fun updateParamPosition(x: Int, y: Int) {
        if (paramLinkedToMain) {
            // When linked, update main engine
            if (currentParamPart < 3) {
                updateEngineA(x, y)
            } else {
                updateEngineB(x, y)
            }
        } else {
            // When decoupled, only update param sequencer
            paramEnvelopeSequencers.forEach { it.setPosition(x, y) }
        }
        paramEnvelopeX = x
        paramEnvelopeY = y
    }

    fun copyParamFromEngineA() {
        val part = currentParamPart
        paramEnvelopeSequencers[part].setPosition(engineAX, engineAY)
        paramEnvelopeX = engineAX
        paramEnvelopeY = engineAY
        updateParamUIState(part)
    }

    fun copyParamFromEngineB() {
        val part = currentParamPart
        paramEnvelopeSequencers[part].setPosition(engineBX, engineBY)
        paramEnvelopeX = engineBX
        paramEnvelopeY = engineBY
        updateParamUIState(part)
    }

    // Polyrhythm Methods - True Euclidean Polyrhythms
    fun generateReich() = service?.polyrhythmEngine?.generateReich()
    fun generatePrimes() = service?.polyrhythmEngine?.generatePrimes()
    fun generateFibonacci() = service?.polyrhythmEngine?.generateFibonacci()
    fun generateKotekan() = service?.polyrhythmEngine?.generateKotekan()

    fun setPolyrhythmSteps(channel: Int, steps: Int) {
        val currentHits = service?.polyrhythmEngine?.getHits(channel) ?: 4
        service?.polyrhythmEngine?.setStepsHits(channel, steps, currentHits)
    }

    fun setPolyrhythmHits(channel: Int, hits: Int) {
        val currentSteps = service?.polyrhythmEngine?.getSteps(channel) ?: 16
        service?.polyrhythmEngine?.setStepsHits(channel, currentSteps, hits)
    }
    
    fun togglePolyrhythmMute(channel: Int) {
        val current = service?.polyrhythmEngine?.isMuted?.getOrNull(channel) ?: false
        service?.polyrhythmEngine?.setMute(channel, !current)
    }

    fun togglePolyrhythmSolo(channel: Int) {
        val current = service?.polyrhythmEngine?.isSoloed?.getOrNull(channel) ?: false
        service?.polyrhythmEngine?.setSolo(channel, !current)
    }

    // CRITICAL: Clean up resources when ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        // Cancel gesture playback to prevent memory leaks
        gesturePlaybackJob?.cancel()
        gesturePlaybackJob = null
        // Clear any pending callbacks
        com.volcagrids.ui.FeedbackManager.clear()
        // Stop and release Plaits audio engine
        plaitsAudioEngine.release()
    }
}
