package com.volcagrids.midi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.volcagrids.ui.MainActivity
import com.volcagrids.engine.PatternGenerator
import com.volcagrids.engine.OutputMode
import com.volcagrids.engine.PhysicsEngine
import com.volcagrids.engine.MorphEngine
import com.volcagrids.engine.LogicEngine
import com.volcagrids.engine.LogicMode
import com.volcagrids.engine.Scene
import com.volcagrids.ui.FeedbackManager
import com.volcagrids.ui.components.EuclideanSHConfig
import com.volcagrids.midi.VolcaParameter
import com.volcagrids.engine.EnvelopeSequencer
import com.volcagrids.engine.ParameterAssignment
import com.volcagrids.engine.EnvelopeShape
import com.volcagrids.engine.PolyrhythmEngine
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@androidx.compose.foundation.ExperimentalFoundationApi
class MidiSequencerService : Service() {
    val engineA = PatternGenerator()
    val engineB = PatternGenerator()
    val midiManager = VolcaDrumMidiManager()

    val physicsA = PhysicsEngine()
    val physicsB = PhysicsEngine()
    val morphEngine = MorphEngine()
    val logicEngine = LogicEngine()

    val logicModesA = Array(3) { LogicMode.NONE }
    val logicModesB = Array(3) { LogicMode.NONE }
    var morphFactor: Float = 0f
    var lastMorphFactor: Float = -1f
    var scene1: Scene? = null
    var scene2: Scene? = null

    // Performance Macros
    var crossLogicMode: Int = 0 // 0=None, 1=AND, 2=OR, 3=XOR
    var globalProbability: Float = 1f // 1.0 = 100% chance to play
    var glitchAmount: Float = 0f // 0.0 = cleanly quantized

    // Mod Matrix State
    val lfoConfigs = Array(4) { com.volcagrids.ui.components.LFOConfig(it) }
    val modulationSlots = mutableListOf<com.volcagrids.ui.components.ModulationSlot>()
    val euclideanSHConfigs = Array(6) { com.volcagrids.ui.components.EuclideanSHConfig(channel = it) }

    // Track last triggered step for Euclidean S&H to prevent MIDI flooding
    private var lastEuclideanStep = -1

    // Parameter Envelope Sequencers (6 parts)
    val envelopeSequencers = List(6) { EnvelopeSequencer() }
    val paramAssignments = mutableListOf(
        ParameterAssignment(0, 51, 127, 0, EnvelopeShape.SMOOTH, false),  // DRIVE
        ParameterAssignment(1, 49, 100, 20, EnvelopeShape.SMOOTH, false),  // BIT_REDUCTION
        ParameterAssignment(2, 117, 80, 40, EnvelopeShape.EXPONENTIAL, false),  // DECAY
        ParameterAssignment(3, 118, 60, 30, EnvelopeShape.SMOOTH, false),  // BODY
        ParameterAssignment(4, 26, 50, 64, EnvelopeShape.STEPPED, false),  // PITCH
        ParameterAssignment(5, 29, 40, 64, EnvelopeShape.RANDOM, false)  // MOD_AMOUNT
    )

    // Polyrhythm Engine
    val polyrhythmEngine = PolyrhythmEngine()
    var polyrhythmEnabled = false
    var polyrhythmPlaying = false  // Set by ViewModel when in POLYRHYTHM mode

    // Android Lifecycle & CPU WakeLock
    private var wakeLock: PowerManager.WakeLock? = null

    // Real-time sequencer loop
    @Volatile private var isRunning = false
    private var sequencerThread: Thread? = null

    private var bpm = 120.0
    private var isPlaying = false

    var isExternalClock = false
    private var inputSyncPort: android.media.midi.MidiOutputPort? = null

    // Foreground Service Notification
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "v_gr1d_sequencer_channel"
        private const val NOTIFICATION_ID = 1001
    }

    // Real-time MIDI Clock Receiver
    private val clockReceiver = object : android.media.midi.MidiReceiver() {
        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
            if (!isExternalClock) return
            
            for (i in 0 until count) {
                val byte = msg[offset + i].toInt() and 0xFF
                when (byte) {
                    0xF8 -> { // MIDI Timing Clock
                        if (isPlaying) tick()
                    }
                    0xFA, 0xFB -> { // MIDI Start / Continue
                        isPlaying = true
                    }
                    0xFC -> { // MIDI Stop
                        isPlaying = false
                    }
                }
            }
        }
    }

    fun setExternalSync(enabled: Boolean) {
        this.isExternalClock = enabled
        if (enabled) stop() // Stop internal scheduler
    }

    fun connectOutput(device: android.media.midi.MidiDevice, portIndex: Int) {
        val port = device.openInputPort(portIndex)
        if (port != null) {
            midiManager.setInputPort(port)
        }
    }

    fun connectInputSync(device: android.media.midi.MidiDevice, portIndex: Int) {
        inputSyncPort = device.openOutputPort(portIndex)
        inputSyncPort?.connect(clockReceiver)
    }

    fun setBPM(newBpm: Double) {
        bpm = newBpm
    }

    fun start() {
        if (isPlaying || isExternalClock) return // Don't start scheduler if slaved
        isPlaying = true
        wakeLock?.acquire() // Hold wake lock for up to 24 hours while playing
        
        // Reset engines to start from step 0
        engineA.reset()
        engineB.reset()
        polyrhythmEngine.reset()
        
        // Fix 4.0: Dispatch step 0 immediately so we don't miss the downbeat
        dispatchTriggers(engineA.tick(0), engineB.tick(0), isFirstTick = true)
        
        // Update notification (non-blocking)
        updateNotification()
        
        if (sequencerThread == null || !isRunning) {
            isRunning = true
            sequencerThread = Thread {
                var lastTime = System.nanoTime()
                var accumulator = 0L
                var wasPlaying = false
                while (isRunning) {
                    val now = System.nanoTime()
                    val dt = now - lastTime
                    lastTime = now

                    if (isPlaying && !isExternalClock) {
                        val tickIntervalNs = (60_000_000_000L / (bpm * 24)).toLong()
                        if (!wasPlaying) {
                            accumulator = tickIntervalNs // Force immediate tick on start
                            lastTime = now // Discard the massive `dt` built up while paused
                            wasPlaying = true
                        } else {
                            accumulator += dt
                        }
                        
                        if (accumulator >= tickIntervalNs) {
                            if (accumulator > tickIntervalNs * 4) accumulator = tickIntervalNs * 4
                            
                            while (accumulator >= tickIntervalNs) {
                                tick()
                                accumulator -= tickIntervalNs
                            }
                        } else {
                            val sleepNs = tickIntervalNs - accumulator
                            if (sleepNs > 1_000_000) {
                                Thread.sleep(sleepNs / 1_000_000, (sleepNs % 1_000_000).toInt())
                            } else {
                                Thread.yield()
                            }
                        }
                    } else {
                        wasPlaying = false
                        accumulator = 0L
                        Thread.sleep(10)
                    }
                }
            }.apply {
                name = "SequencerThread"
                priority = Thread.MAX_PRIORITY
                isDaemon = true
                start()
            }
        }
    }

    fun stop() {
        isPlaying = false
        if (wakeLock?.isHeld == true) wakeLock?.release()
        midiManager.clearCCQueue()
        updateNotification() // Update notification to show stopped state
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "v_GR1D Sequencer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sequencer running notification"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Build and display the foreground service notification
     * Industrial aesthetic: [>> RUNNING] / [|| HALTED]
     */
    private fun updateNotification() {
        createNotificationChannel()

        val statusText = if (isPlaying) "[>> RUNNING]" else "[PAUSED]"
        val bpmText = "BPM:" + bpm.toInt().toString() + "  STEP:" + engineA.getStep().toString()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("v_GR1D")
            .setContentText("$statusText | $bpmText")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true) // No sound
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
    
    // Advanced Modulation Updates
    fun updateLFO(lfo: com.volcagrids.ui.components.LFOConfig) {
        val index = lfoConfigs.indexOfFirst { it.index == lfo.index }
        if (index != -1) lfoConfigs[index] = lfo
    }

    fun updateModulationSlot(slot: com.volcagrids.ui.components.ModulationSlot) {
        val existingIndex = modulationSlots.indexOfFirst { it.partIndex == slot.partIndex && it.slotIndex == slot.slotIndex }
        if (existingIndex != -1) {
            modulationSlots[existingIndex] = slot
        } else {
            modulationSlots.add(slot)
        }
    }

    private fun tick() {
        // 0. Calculate Morphed State (only when morph factor changes)
        if (morphFactor != lastMorphFactor) {
            lastMorphFactor = morphFactor
            val s1 = scene1
            val s2 = scene2
            if (s1 != null && s2 != null) {
                val morphed = morphEngine.interpolate(s1, s2, morphFactor)
                engineA.x = morphed.engineAX
                engineA.y = morphed.engineAY
                engineA.densities.set(0, morphed.densitiesA[0])
                engineA.densities.set(1, morphed.densitiesA[1])
                engineA.densities.set(2, morphed.densitiesA[2])
                engineA.randomness = morphed.randomnessA

                engineB.x = morphed.engineBX
                engineB.y = morphed.engineBY
                engineB.densities.set(0, morphed.densitiesB[0])
                engineB.densities.set(1, morphed.densitiesB[1])
                engineB.densities.set(2, morphed.densitiesB[2])
                engineB.randomness = morphed.randomnessB
            }
        }

        // 1. Update Physics (only if enabled)
        // Physics engine position IS the engine position (Fix 2.1)
        if (physicsA.enabled || physicsB.enabled) {
            // Sync engine positions to physics if physics has impulse
            if (physicsA.vx != 0f || physicsA.vy != 0f) {
                physicsA.x = engineA.x.toFloat()
                physicsA.y = engineA.y.toFloat()
            }
            if (physicsB.vx != 0f || physicsB.vy != 0f) {
                physicsB.x = engineB.x.toFloat()
                physicsB.y = engineB.y.toFloat()
            }
            
            // Update physics simulation
            physicsA.update()
            physicsB.update()
            
            // Apply physics positions to engines
            engineA.x = physicsA.x.toInt().coerceIn(0, 255)
            engineA.y = physicsA.y.toInt().coerceIn(0, 255)
            engineB.x = physicsB.x.toInt().coerceIn(0, 255)
            engineB.y = physicsB.y.toInt().coerceIn(0, 255)
        }

        // 2. Tick Grids
        val outA = engineA.tick(1)
        val outB = engineB.tick(1)

        dispatchTriggers(outA, outB, isFirstTick = false)
    }

    private fun dispatchTriggers(initialOutA: Int, initialOutB: Int, isFirstTick: Boolean) {
        var outA = initialOutA
        var outB = initialOutB

        // 2a. Performance Macro: Glitch
        if (glitchAmount > 0f && kotlin.random.Random.nextFloat() < glitchAmount) {
            // "Stutter" by reading old values or forcing intense bursts
            if (kotlin.random.Random.nextBoolean()) {
                outA = outA shl 1 // Bitshift corruption
                outB = outB shl 1
            } else {
                // Read again to fast-forward phase
                outA = engineA.tick(1)
                outB = engineB.tick(1)
            }
        }

        // 2b. Logic Bridge: Cross-Modulate Engines A and B
        when (crossLogicMode) {
            1 -> { // AND
                val combined = outA and outB
                outA = combined
                outB = combined
            }
            2 -> { // OR
                val combined = outA or outB
                outA = combined
                outB = combined
            }
            3 -> { // XOR
                val combined = outA xor outB
                outA = combined
                outB = combined
            }
        }

        // 2c. Performance Macro: Global Probability 
        if (globalProbability < 1f) {
            if (kotlin.random.Random.nextFloat() > globalProbability) outA = 0
            if (kotlin.random.Random.nextFloat() > globalProbability) outB = 0
        }

        // 3. Dispatch Triggers with Logic Filter
        for (i in 0..2) {
            if (logicEngine.applyLogic(outA, i, logicModesA[i])) {
                val isAccent = (outA and (1 shl (i + 3))) != 0
                midiManager.sendTrigger(i, isAccent)
                // Visual feedback - NON-BLOCKING direct state update
                com.volcagrids.ui.FeedbackManager.onTrigger(i, if (isAccent) 1.0f else 0.6f)
            }
        }
        for (i in 0..2) {
            if (logicEngine.applyLogic(outB, i, logicModesB[i])) {
                val isAccent = (outB and (1 shl (i + 3))) != 0
                midiManager.sendTrigger(i + 3, isAccent)
                // Visual feedback - NON-BLOCKING direct state update
                com.volcagrids.ui.FeedbackManager.onTrigger(i + 3, if (isAccent) 1.0f else 0.6f)
            }
        }

        // 4. Algorithmic Modulation (Migrated to UI ModMatrix defaults)

        // 5. Euclidean S&H - Optimized (only when enabled)
        val currentStep = engineA.getStep()
        if (currentStep != lastEuclideanStep) {
            lastEuclideanStep = currentStep
            for (config in euclideanSHConfigs) {
                if (!config.enabled) continue
                if (checkEuclideanHit(currentStep, config.steps, config.hits)) {
                    midiManager.queueCC(config.channel, config.targetCC.cc, 
                        kotlin.random.Random.nextInt(config.minValue, config.maxValue))
                }
            }
        }

        // 6. Parameter Envelope Sequencers - Only tick enabled ones
        for (i in 0..5) {
            if (paramAssignments[i].enabled) {
                if (!isFirstTick) envelopeSequencers[i].tick()
                midiManager.queueCC(i, paramAssignments[i].ccNumber, envelopeSequencers[i].getValue())
            }
        }

        // 7. Polyrhythm Engine - 6 independent channels (Fix 2.2 - proper polyrhythm)
        // Each channel has its own ratio, creating true interlocking polyrhythms
        if (polyrhythmEnabled || polyrhythmPlaying) {
            // Tick returns BooleanArray with hit state for each of 6 channels
            // Supplying !isFirstTick controls whether the phase advances
            val hits = polyrhythmEngine.tick(!isFirstTick)
            
            // Each channel triggers independently based on its own ratio
            for (ch in 0..5) {
                if (hits[ch]) {
                    midiManager.sendTrigger(ch, false)
                }
            }
        }

        // 8. Process Advanced Mod Matrix
        processModMatrix(currentStep)

        // 9. Process throttled CC queue
        midiManager.processCCQueue()
    }

    private fun processModMatrix(currentStep: Int) {
        // 1. Calculate and Route LFOs
        for (i in 0..3) {
            val config = lfoConfigs[i]
            if (!config.enabled) continue
            
            val cycleSteps = 64f - (config.rate * 63f) 
            val phase = (currentStep % cycleSteps) / cycleSteps
            
            val rawValue = when (config.waveform) {
                com.volcagrids.ui.components.LFOWaveform.SINE -> (Math.sin(phase * Math.PI * 2) * 0.5 + 0.5).toFloat()
                com.volcagrids.ui.components.LFOWaveform.TRIANGLE -> if (phase < 0.5f) phase * 2f else 2f - (phase * 2f)
                com.volcagrids.ui.components.LFOWaveform.SAW -> phase
                com.volcagrids.ui.components.LFOWaveform.SQUARE -> if (phase < 0.5f) 1f else 0f
                com.volcagrids.ui.components.LFOWaveform.S_AND_H -> kotlin.random.Random.nextFloat()
                com.volcagrids.ui.components.LFOWaveform.STEP -> if (phase < 0.5f) 1f else 0f
            }
            
            val depthFactor = config.depth
            val modulationAmount = (rawValue * depthFactor * 127f).toInt()
            
            when (config.destination) {
                com.volcagrids.ui.components.LFODestination.NONE -> {}
                com.volcagrids.ui.components.LFODestination.ENGINE_A_X -> engineA.x = (engineA.x + modulationAmount - 64).coerceIn(0, 255)
                com.volcagrids.ui.components.LFODestination.ENGINE_A_Y -> engineA.y = (engineA.y + modulationAmount - 64).coerceIn(0, 255)
                com.volcagrids.ui.components.LFODestination.ENGINE_A_RANDOMNESS -> engineA.randomness = (engineA.randomness + modulationAmount).coerceIn(0, 255)
                com.volcagrids.ui.components.LFODestination.ENGINE_B_X -> engineB.x = (engineB.x + modulationAmount - 64).coerceIn(0, 255)
                com.volcagrids.ui.components.LFODestination.ENGINE_B_Y -> engineB.y = (engineB.y + modulationAmount - 64).coerceIn(0, 255)
                com.volcagrids.ui.components.LFODestination.ENGINE_B_RANDOMNESS -> engineB.randomness = (engineB.randomness + modulationAmount).coerceIn(0, 255)
                com.volcagrids.ui.components.LFODestination.CC_CUSTOM -> {
                    val destCc = config.targetCC?.cc ?: continue
                    midiManager.queueCC(0, destCc, modulationAmount.coerceIn(0, 127))
                }
            }
        }

        // 2. Map sequencer engines to CCs via Modulation Slots
        for (slot in modulationSlots) {
            if (!slot.enabled) continue
            val destCc = slot.destination?.cc ?: continue
            
            // Amount is 0-127, Center is 64
            val amountCentered = (slot.amount - 64) / 63f
            
            val sourceVal = when (slot.source) {
                com.volcagrids.ui.components.ModSource.NONE -> 0f
                com.volcagrids.ui.components.ModSource.LEVEL_BD -> engineA.rawLevels[0] / 255f
                com.volcagrids.ui.components.ModSource.LEVEL_SD -> engineA.rawLevels[1] / 255f
                com.volcagrids.ui.components.ModSource.LEVEL_HH -> engineA.rawLevels[2] / 255f
                com.volcagrids.ui.components.ModSource.DENSITY_BD -> engineA.densities[0] / 255f
                com.volcagrids.ui.components.ModSource.DENSITY_SD -> engineA.densities[1] / 255f
                com.volcagrids.ui.components.ModSource.DENSITY_HH -> engineA.densities[2] / 255f
                com.volcagrids.ui.components.ModSource.X_AXIS -> engineA.x / 255f
                com.volcagrids.ui.components.ModSource.Y_AXIS -> engineA.y / 255f
                com.volcagrids.ui.components.ModSource.RANDOM -> kotlin.random.Random.nextFloat()
            }
            
            val modulationOffset = (sourceVal * amountCentered * 63f).toInt()
            val finalCcValue = (64 + modulationOffset).coerceIn(0, 127)
            
            midiManager.queueCC(slot.partIndex, destCc, finalCcValue)
        }
    }
    // DO NOT call updateNotification() here - it blocks the main thread!
    // Notification is updated only in start() and stop()

    /**
     * Check Euclidean hit without allocating List - uses math directly
     */
    private fun checkEuclideanHit(step: Int, steps: Int, hits: Int): Boolean {
        if (hits == 0 || hits >= steps) return hits > 0
        val position = (step % steps) * hits
        return (position % steps) < hits
    }

    inner class LocalBinder : android.os.Binder() {
        fun getService(): MidiSequencerService = this@MidiSequencerService
    }

    private val binder = LocalBinder()
    
    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VolcaGrids::MidiSequencerWakeLock")
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        sequencerThread?.interrupt()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        stop()
    }
}
