package com.volcagrids.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.volcagrids.engine.LogicMode
import com.volcagrids.engine.OutputMode
import com.volcagrids.midi.MidiSequencerService
import com.volcagrids.ui.components.*
import com.volcagrids.ui.theme.*
@androidx.compose.foundation.ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(application)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MidiSequencerService.LocalBinder
            val sequencerService = binder.getService()
            viewModel.service = sequencerService

            // Sync initial state from prefs to service
            syncServiceWithPrefs(sequencerService)
        }
        override fun onServiceDisconnected(arg0: ComponentName) { 
            viewModel.service = null 
        }
    }

    private fun syncServiceWithPrefs(sequencerService: MidiSequencerService) {
        // Sync engine positions
        sequencerService.engineA.x = viewModel.engineAX
        sequencerService.engineA.y = viewModel.engineAY
        sequencerService.engineB.x = viewModel.engineBX
        sequencerService.engineB.y = viewModel.engineBY
        
        // Sync densities
        for (i in 0..2) {
            sequencerService.engineA.densities[i] = viewModel.densitiesA[i]
            sequencerService.engineB.densities[i] = viewModel.densitiesB[i]
        }
        
        // Sync part modes
        for (i in 0..2) {
            sequencerService.engineA.partModes[i] = viewModel.partModesA[i]
            sequencerService.engineB.partModes[i] = viewModel.partModesB[i]
        }
        
        // Sync logic modes
        for (i in 0..2) {
            sequencerService.logicModesA[i] = viewModel.logicModesA[i]
            sequencerService.logicModesB[i] = viewModel.logicModesB[i]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MidiSequencerService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            MaterialTheme(colorScheme = RasterColorScheme, typography = RasterTypography) {
                // Decay update loop for LED feedback - runs at 60fps
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(16)  // ~60fps
                        com.volcagrids.ui.FeedbackManager.updateDecay()
                    }
                }

                Box(modifier = Modifier.fillMaxSize().background(RasterBlack)) {
                    RasterInterface(viewModel)

                    if (viewModel.showMidiConfig) {
                        MidiConfigOverlayCompact(
                            viewModel = viewModel,
                            onClose = { viewModel.showMidiConfig = false }
                        )
                    }

                    if (viewModel.showSettings) {
                        SettingsOverlayCompact(
                            viewModel = viewModel,
                            onClose = { viewModel.showSettings = false }
                        )
                    }

                    // Volca Sound Design Drawer
                    if (viewModel.showVolcaDrawer) {
                        VolcaDrawer(
                            isVisible = viewModel.showVolcaDrawer,
                            onDismiss = { viewModel.showVolcaDrawer = false },
                            waveguideDecay = viewModel.waveguideDecay,
                            waveguideBody = viewModel.waveguideBody,
                            waveguideTune = viewModel.waveguideTune,
                            onWaveguideChange = { decay, body, tune ->
                                viewModel.setWaveguide(decay, body, tune)
                            },
                            morphValues = viewModel.morphValues,
                            onMorphChange = { partIndex, value ->
                                viewModel.setMorphValue(partIndex, value)
                            },
                            accentColor = RasterSignalA
                        )
                    }

                    // Parameter Sequencer Overlay
                    ParameterSequencerOverlay(
                        isVisible = viewModel.showParameterSequencer,
                        currentPart = viewModel.currentParamPart,
                        currentX = viewModel.paramEnvelopeX,
                        currentY = viewModel.paramEnvelopeY,
                        currentValue = viewModel.paramEnvelopeValue,
                        currentRange = viewModel.paramEnvelopeRange,
                        currentOffset = viewModel.paramEnvelopeOffset,
                        currentShape = viewModel.paramEnvelopeShape,
                        currentCC = viewModel.paramEnvelopeCC,
                        isLinked = viewModel.paramLinkedToMain,
                        engineAX = viewModel.engineAX,
                        engineAY = viewModel.engineAY,
                        engineBX = viewModel.engineBX,
                        engineBY = viewModel.engineBY,
                        onDismiss = { viewModel.closeParameterSequencer() },
                        onPartChange = { part ->
                            viewModel.currentParamPart = part
                            viewModel.updateParamUIState(part)
                        },
                        onCCChange = { cc ->
                            viewModel.updateParamCC(cc)
                        },
                        onLinkToggle = {
                            viewModel.toggleParamLink()
                        },
                        onShapeChange = { shape ->
                            viewModel.paramEnvelopeShape = shape
                            val current = viewModel.paramAssignments[viewModel.currentParamPart]
                            viewModel.updateParamAssignment(
                                viewModel.currentParamPart,
                                current.copy(shape = shape)
                            )
                        },
                        onRangeChange = { range ->
                            viewModel.paramEnvelopeRange = range
                            val current = viewModel.paramAssignments[viewModel.currentParamPart]
                            viewModel.updateParamAssignment(
                                viewModel.currentParamPart,
                                current.copy(range = range)
                            )
                        },
                        onOffsetChange = { offset ->
                            viewModel.paramEnvelopeOffset = offset
                            val current = viewModel.paramAssignments[viewModel.currentParamPart]
                            viewModel.updateParamAssignment(
                                viewModel.currentParamPart,
                                current.copy(offset = offset)
                            )
                        },
                        onPositionChange = { x, y ->
                            viewModel.updateParamPosition(x, y)
                        },
                        onCopyFromA = { viewModel.copyParamFromEngineA() },
                        onCopyFromB = { viewModel.copyParamFromEngineB() }
                    )

                    // Advanced Overlays
                    ModMatrixOverlay(
                        viewModel = viewModel,
                        isVisible = viewModel.showModMatrix,
                        onClose = { viewModel.showModMatrix = false }
                    )
                    
                    PerformanceOverlay(
                        viewModel = viewModel,
                        isVisible = viewModel.showPerformanceLayer,
                        onClose = { viewModel.showPerformanceLayer = false }
                    )
                    
                    SystemToolsOverlay(
                        viewModel = viewModel,
                        isVisible = viewModel.showSystemTools,
                        onClose = { viewModel.showSystemTools = false }
                    )

                    // Ratio Selector removed - now using PolyrhythmModeUI with 6 sliders
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}

// Factory for AndroidViewModel with Application parameter
class MainViewModelFactory(
    private val application: android.app.Application
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(application) as T
    }
}

// Removed legacy MidiConfigOverlay

@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun RasterInterface(viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(RasterBlack).padding(8.dp)) {
        // Branding Header
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("v_GR1D", style = RasterTypography.headlineLarge, color = RasterActive)
            Text("// GRID_MATRIX_v2.0", style = RasterTypography.labelSmall, color = RasterGrid)
        }

        // Show Polyrhythm UI in POLY mode - TRUE POLYRHYTHM with ratios
        if (viewModel.sequencerMode == SequencerMode.POLYRHYTHM) {
            val service = viewModel.service
            if (service != null) {
                PolyrhythmModeUI(
                    modifier = Modifier.fillMaxSize(),
                    engine = service.polyrhythmEngine,
                    isPlaying = viewModel.isPlaying,
                    onPlayToggle = viewModel::togglePlay,
                    onStepsChange = { channel, steps ->
                        viewModel.setPolyrhythmSteps(channel, steps)
                    },
                    onHitsChange = { channel, hits ->
                        viewModel.setPolyrhythmHits(channel, hits)
                    },
                    onDivisionChange = { channel, division ->
                        // Map float division to nearest TimeDivision
                        val divisions = com.volcagrids.engine.PolyrhythmEngine.Companion.TimeDivision.entries
                        val nearest = divisions.minByOrNull { div: com.volcagrids.engine.PolyrhythmEngine.Companion.TimeDivision ->
                            kotlin.math.abs(div.value - division)
                        }
                        if (nearest != null) {
                            service.polyrhythmEngine.setTimeDivision(channel, nearest)
                        }
                    },
                    onMuteToggle = viewModel::togglePolyrhythmMute,
                    onSoloToggle = viewModel::togglePolyrhythmSolo,
                    onGenerateReich = viewModel::generateReich,
                    onGeneratePrimes = viewModel::generatePrimes,
                    onGenerateFibonacci = viewModel::generateFibonacci,
                    onGenerateKotekan = viewModel::generateKotekan,
                    onDrumsToggle = {
                        viewModel.sequencerMode = SequencerMode.DRUMS
                    }
                )
            }
        } else {
            // Regular Drum/Euclidean Mode UI
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val isCompact = maxWidth < 600.dp
                if (isCompact) {
                    // Phone UI - Swipable Pager
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                        initialPage = 1,
                        pageCount = { 3 }
                    )
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) { page ->
                            when (page) {
                                0 -> DataDeck(
                                    title = "// [CHN_1-3]",
                                    color = RasterSignalA,
                                    viewModel = viewModel,
                                    engineIndex = 0,
                                    channelOffset = 0,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                )
                                1 -> MasterCore(viewModel = viewModel, modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp))
                                2 -> DataDeck(
                                    title = "// [CHN_4-6]",
                                    color = RasterSignalB,
                                    viewModel = viewModel,
                                    engineIndex = 1,
                                    channelOffset = 3,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                )
                            }
                        }
                        
                        // Page Indicator
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(3) { iteration ->
                                val color = if (pagerState.currentPage == iteration) Color.White else Color.DarkGray
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(8.dp)
                                        .background(color, androidx.compose.foundation.shape.CircleShape)
                                )
                            }
                        }
                    }
                } else {
                    // Tablet UI - Side by side Row
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DataDeck(
                            title = "// [CHN_1-3]",
                            color = RasterSignalA,
                            viewModel = viewModel,
                            engineIndex = 0,
                            channelOffset = 0,
                            modifier = Modifier.weight(1f)
                        )

                        MasterCore(viewModel = viewModel, modifier = Modifier.weight(0.5f))

                        DataDeck(
                            title = "// [CHN_4-6]",
                            color = RasterSignalB,
                            viewModel = viewModel,
                            engineIndex = 1,
                            channelOffset = 3,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DataDeck(
    title: String,
    color: Color,
    viewModel: MainViewModel,
    engineIndex: Int,
    channelOffset: Int,
    modifier: Modifier
) {
    Column(modifier = modifier.fillMaxHeight().border(1.dp, RasterGrid).padding(4.dp)) {
        Text(title, style = RasterTypography.labelSmall, color = color, modifier = Modifier.padding(bottom = 4.dp))

        // PAD
        Box(modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, RasterGrid)) {
            GenerativePad(
                modifier = Modifier.fillMaxSize(),
                color = color,
                x = if (engineIndex == 0) viewModel.engineAX else viewModel.engineBX,
                y = if (engineIndex == 0) viewModel.engineAY else viewModel.engineBY,
                viewModel = viewModel,
                engineIndex = engineIndex,
                onPositionChange = { x, y ->
                    if (engineIndex == 0) viewModel.updateEngineA(x, y)
                    else viewModel.updateEngineB(x, y)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // MIXER
        Row(modifier = Modifier.height(180.dp).fillMaxWidth()) {
            val densities = if (engineIndex == 0) viewModel.densitiesA else viewModel.densitiesB
            val partModes = if (engineIndex == 0) viewModel.partModesA else viewModel.partModesB
            val logicModes = if (engineIndex == 0) viewModel.logicModesA else viewModel.logicModesB
            val baseName = if (engineIndex == 0) "T" else "T"
            val baseIdx = if (engineIndex == 0) 1 else 4

            for (i in 0..2) {
                val intensity = FeedbackManager.triggerIntensities[channelOffset + i]
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Part Label + Mode
                    Text("${baseName}${baseIdx + i}", style = RasterTypography.labelSmall, color = color)
                    SystemButton(
                        label = partModes[i].name.take(4),
                        isActive = partModes[i] == com.volcagrids.engine.OutputMode.EUCLIDEAN,
                        onClick = { viewModel.cycleMode(engineIndex, i) },
                        modifier = Modifier.fillMaxWidth(0.8f).height(20.dp)
                    )

                    DataBar(
                        value = densities[i] / 255f,
                        label = "",
                        flashIntensity = intensity,
                        onValueChange = { viewModel.updateDensity(engineIndex, i, (it * 255).toInt()) },
                        color = color,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        onLongPress = {
                            val partIndex = channelOffset + i
                            viewModel.openParameterSequencer(partIndex)
                        }
                    )

                    SystemButton(
                        label = logicModes[i].name.take(3),
                        isActive = logicModes[i] != com.volcagrids.engine.LogicMode.NONE,
                        onClick = { viewModel.cycleLogic(engineIndex, i) },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun MasterCore(viewModel: MainViewModel, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().border(1.dp, RasterGrid).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // SEQUENCER ENGINE MODE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "DRUMS",
                    style = RasterTypography.labelSmall,
                    color = if (viewModel.sequencerMode == SequencerMode.DRUMS) RasterSignalA else Color.Gray,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (viewModel.sequencerMode == SequencerMode.DRUMS) RasterSignalA else Color(0xFF333333)
                        )
                        .clickable {
                            viewModel.sequencerMode = SequencerMode.DRUMS
                            viewModel.polyrhythmPlaying = false
                            viewModel.service?.polyrhythmPlaying = false
                            viewModel.service?.polyrhythmEngine?.reset()
                        }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                )
                Text(
                    text = "EUCL",
                    style = RasterTypography.labelSmall,
                    color = if (viewModel.sequencerMode == SequencerMode.EUCLIDEAN) RasterSignalA else Color.Gray,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (viewModel.sequencerMode == SequencerMode.EUCLIDEAN) RasterSignalA else Color(0xFF333333)
                        )
                        .clickable {
                            viewModel.sequencerMode = SequencerMode.EUCLIDEAN
                            viewModel.polyrhythmPlaying = false
                            viewModel.service?.polyrhythmPlaying = false
                            viewModel.service?.polyrhythmEngine?.reset()
                        }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                )
                Text(
                    text = "POLY",
                    style = RasterTypography.labelSmall,
                    color = if (viewModel.sequencerMode == SequencerMode.POLYRHYTHM) Color(0xFFFFD600) else Color.Gray,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (viewModel.sequencerMode == SequencerMode.POLYRHYTHM) Color(0xFFFFD600) else Color(0xFF333333)
                        )
                        .clickable {
                            viewModel.sequencerMode = SequencerMode.POLYRHYTHM
                            viewModel.polyrhythmPlaying = viewModel.isPlaying
                            viewModel.service?.polyrhythmPlaying = viewModel.isPlaying
                            viewModel.service?.polyrhythmEngine?.reset()
                        }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // LOGIC BRIDGE (Cross-Modulation)
            Text("// LOGIC_BRIDGE", style = RasterTypography.labelSmall, color = RasterWhite)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("---", "AND", "OR", "XOR").forEachIndexed { index, mode ->
                    SystemButton(
                        label = mode,
                        isActive = viewModel.crossLogicMode == index,
                        onClick = { viewModel.crossLogicMode = index },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // SYSTEM UI MENUS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SystemButton("SETT", false, { viewModel.showSettings = true }, Modifier.weight(1f))
                SystemButton("MIDI", false, { viewModel.showMidiConfig = true }, Modifier.weight(1f))
                SystemButton("VLC", false, { viewModel.toggleVolcaDrawer() }, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SystemButton("MOD", false, { viewModel.toggleModMatrix() }, Modifier.weight(1f))
                SystemButton("PERF", false, { viewModel.togglePerformanceLayer() }, Modifier.weight(1f))
                SystemButton("SYS", false, { viewModel.toggleSystemTools() }, Modifier.weight(1f))
            }
        }

        // MACRO SLIDERS
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("PROB", style = RasterTypography.labelSmall, color = Color.Gray)
                DataBar(
                    value = viewModel.globalProbability,
                    label = "",
                    flashIntensity = 0f,
                    onValueChange = { viewModel.globalProbability = it },
                    color = RasterWhite,
                    modifier = Modifier.fillMaxHeight().width(24.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("MORPH", style = RasterTypography.labelSmall, color = RasterSignalA)
                DataBar(
                    value = viewModel.morphFactor,
                    label = "",
                    flashIntensity = 0f,
                    onValueChange = { viewModel.morphFactor = it },
                    color = RasterActive,
                    modifier = Modifier.fillMaxHeight().width(24.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("GLITCH", style = RasterTypography.labelSmall, color = Color.Gray)
                DataBar(
                    value = viewModel.glitchAmount,
                    label = "",
                    flashIntensity = 0f,
                    onValueChange = { viewModel.glitchAmount = it },
                    color = RasterWhite,
                    modifier = Modifier.fillMaxHeight().width(24.dp)
                )
            }
        }

        SystemButton(
            label = if (viewModel.isPlaying) "[>> EXECUTING]" else "[|| HALTED]",
            isActive = viewModel.isPlaying,
            onClick = { viewModel.togglePlay() },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )
    }
}

// Removed legacy SettingsOverlay
