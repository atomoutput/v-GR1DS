package com.volcagrids.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.volcagrids.engine.PolyrhythmEngine
import com.volcagrids.ui.theme.RasterTypography
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow

@Composable
fun PolyrhythmModeUI(
    modifier: Modifier = Modifier,
    engine: PolyrhythmEngine,
    isPlaying: Boolean = false,
    onPlayToggle: () -> Unit = {},
    onStepsChange: (Int, Int) -> Unit = { _, _ -> },
    onHitsChange: (Int, Int) -> Unit = { _, _ -> },
    onDivisionChange: (Int, Float) -> Unit = { _, _ -> },
    onMuteToggle: (Int) -> Unit = {},
    onSoloToggle: (Int) -> Unit = {},
    onDrumsToggle: () -> Unit = {},
    onGenerateReich: () -> Unit = {},
    onGeneratePrimes: () -> Unit = {},
    onGenerateFibonacci: () -> Unit = {},
    onGenerateKotekan: () -> Unit = {}
) {
    // Force recomposition for animation
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (isPlaying) {
            kotlinx.coroutines.delay(16) // ~60fps smooth animation for phases
            tick = (tick + 1) % 1000
        }
    }
    
    val steps = engine.steps
    val hits = engine.hits
    val timeDivisions = engine.timeDivisions
    val phases = engine.phases
    val isMuted = engine.isMuted
    val isSoloed = engine.isSoloed
    
    val channelNames = listOf("KICK", "SNARE", "LOW", "HH", "CYM", "GLT")
    val channelColors = listOf(
        Color(0xFFFF5722), Color(0xFFFF9800), Color(0xFFFFC107),
        Color(0xFF4CAF50), Color(0xFF00BCD4), Color(0xFF00B0FF)
    )
    
    Column(
        modifier = modifier
            .background(Color(0xE6121214))
            .border(1.dp, Color.White.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        // Header & Master Measure Control
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("POLY_NEXUS", style = RasterTypography.labelSmall, color = Color(0xFFFFD600))
                Spacer(modifier = Modifier.width(16.dp))
                // Master Measure Selector
                Text("MASTER_LOOP: ", style = RasterTypography.labelSmall, color = Color.Gray)
                SystemButton(
                    label = if (engine.masterBeats >= 16) "${engine.masterBeats}_BEATS_(${engine.masterBeats/4}B)" else "${engine.masterBeats}_BEATS",
                    isActive = false,
                    onClick = {
                        engine.masterBeats = when (engine.masterBeats) {
                            1 -> 2; 2 -> 4; 4 -> 8; 8 -> 16; 16 -> 32; 32 -> 64; 64 -> 1; else -> 4
                        }
                    },
                    modifier = Modifier.width(80.dp).height(24.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("[DRUMS]", style = RasterTypography.labelSmall, color = Color(0xFFFF5722),
                    modifier = Modifier.clickable { onDrumsToggle() })
                Text(if (isPlaying) "[||]" else "[>>]", style = RasterTypography.labelSmall,
                    color = if (isPlaying) Color(0xFFFF5722) else Color.Gray,
                    modifier = Modifier.clickable { onPlayToggle() })
            }
        }
        
        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left Side: 6 Channel Sliders
            Column(modifier = Modifier.weight(0.5f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (ch in 0..5) {
                    ChannelControlRow(
                        channel = ch,
                        name = channelNames[ch],
                        color = channelColors[ch],
                        steps = steps.getOrNull(ch) ?: 16,
                        hits = hits.getOrNull(ch) ?: 4,
                        division = timeDivisions.getOrNull(ch) ?: 1.0f,
                        muted = isMuted.getOrNull(ch) ?: false,
                        soloed = isSoloed.getOrNull(ch) ?: false,
                        onStepsChange = { onStepsChange(ch, it) },
                        onHitsChange = { onHitsChange(ch, it) },
                        onDivisionChange = { onDivisionChange(ch, it) },
                        onMute = { onMuteToggle(ch) },
                        onSolo = { onSoloToggle(ch) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Right Side: Concentric Orbit Visualizer
            Box(modifier = Modifier.weight(0.5f).fillMaxHeight().border(1.dp, Color(0xFF333333)), contentAlignment = Alignment.Center) {
                ConcentricOrbitCanvas(
                    steps = steps,
                    hits = hits,
                    phases = phases,
                    isMuted = isMuted,
                    isSoloed = isSoloed,
                    colors = channelColors,
                    tick = tick,
                    modifier = Modifier.fillMaxSize(0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Algorithmic Generators Row
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.Transparent).border(1.dp, Color.White.copy(alpha = 0.1f)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("GENERATOR:", style = RasterTypography.labelSmall, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
            SystemButton("REICH_PHASE", false, { onGenerateReich() }, Modifier.weight(1f).padding(horizontal = 4.dp))
            SystemButton("PRIMES", false, { onGeneratePrimes() }, Modifier.weight(1f).padding(horizontal = 4.dp))
            SystemButton("FIBONACCI", false, { onGenerateFibonacci() }, Modifier.weight(1f).padding(horizontal = 4.dp))
            SystemButton("KOTEKAN", false, { onGenerateKotekan() }, Modifier.weight(1f).padding(horizontal = 4.dp))
        }
    }
}

/**
 * Visualizes the 6 true polyrhythm phases as rotating orbits.
 */
@Composable
fun ConcentricOrbitCanvas(
    steps: IntArray,
    hits: IntArray,
    phases: DoubleArray,
    isMuted: BooleanArray,
    isSoloed: BooleanArray,
    colors: List<Color>,
    tick: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = (minOf(size.width, size.height) / 2) * 0.95f
        val ringSpacing = maxRadius / 6f
        
        val anySoloed = isSoloed.any { it }

        // Draw 12 o'clock zero-phase line
        drawLine(color = Color(0xFF444444), start = center, end = Offset(center.x, center.y - maxRadius), strokeWidth = 2f)

        for (ch in 5 downTo 0) {
            val radius = ringSpacing * (ch + 1)
            val shouldDraw = !isMuted[ch] && (!anySoloed || isSoloed[ch])
            val baseColor = if (shouldDraw) colors[ch] else Color(0xFF222222)
            
            // Draw orbit track
            drawCircle(
                color = baseColor.copy(alpha = 0.2f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            
            if (shouldDraw) {
                val s = steps[ch]
                val h = hits[ch]
                val phase = phases[ch]

                // Draw step markers along the track
                for (i in 0 until s) {
                    val angle = (i * (2 * PI) / s) - (PI / 2) // Start top
                    val pX = center.x + radius * cos(angle).toFloat()
                    val pY = center.y + radius * sin(angle).toFloat()
                    
                    // Highlight active hits uniquely from dormant slices based on Euclidean math
                    val isHitSlice = ((i * h) % s) < h
                    val sliceColor = if (isHitSlice) baseColor else Color(0xFF333333)
                    val sliceRadius = if (isHitSlice) 4.dp.toPx() else 2.dp.toPx()
                    
                    drawCircle(color = sliceColor, radius = sliceRadius, center = Offset(pX, pY))
                }

                // Draw current Phase playhead
                val playheadAngle = (phase * 2 * PI) - (PI / 2)
                val hX = center.x + radius * cos(playheadAngle).toFloat()
                val hY = center.y + radius * sin(playheadAngle).toFloat()
                
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(hX, hY))
                
                // Draw connecting line to center
                drawLine(
                    color = baseColor.copy(alpha = 0.8f),
                    start = center, end = Offset(hX, hY),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun ChannelControlRow(
    channel: Int,
    name: String,
    color: Color,
    steps: Int,
    hits: Int,
    division: Float,
    muted: Boolean,
    soloed: Boolean,
    onStepsChange: (Int) -> Unit,
    onHitsChange: (Int) -> Unit,
    onDivisionChange: (Float) -> Unit,
    onMute: () -> Unit,
    onSolo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFF121214).copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha=0.1f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Name & Mute/Solo
        Column(modifier = Modifier.width(50.dp)) {
            Text("${channel + 1}:$name", style = RasterTypography.labelSmall, color = color,
                fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("m", style = RasterTypography.labelSmall, color = if (muted) Color.Red else Color.Gray,
                    modifier = Modifier.clickable { onMute() })
                Text("s", style = RasterTypography.labelSmall, color = if (soloed) color else Color.Gray,
                    modifier = Modifier.clickable { onSolo() })
            }
        }
        
        // Steps & Hits Column
        Column(modifier = Modifier.weight(0.5f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MiniSlider(
                value = steps,
                maxValue = 32,
                onValueChange = onStepsChange,
                color = color,
                label = "STEPS:"
            )
            MiniSlider(
                value = hits,
                maxValue = steps, // Cannot have more hits than steps
                onValueChange = onHitsChange,
                color = color,
                label = "HITS:"
            )
        }
        
        // Division
        DivisionSlider(
            value = division,
            onValueChange = onDivisionChange,
            color = color,
            modifier = Modifier.weight(0.5f)
        )
    }
}

@Composable
private fun MiniSlider(
    value: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val x = (offset.x / size.width).coerceIn(0f, 1f)
                    val newValue = ((x * (maxValue - 1)).toInt() + 1).coerceIn(1, maxValue)
                    onValueChange(newValue)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val x = (change.position.x / size.width).coerceIn(0f, 1f)
                    val newValue = ((x * (maxValue - 1)).toInt() + 1).coerceIn(1, maxValue)
                    onValueChange(newValue)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(color = Color(0xFF1A1A1E), size = Size(w, h))
            
            val fillWidth = ((value - 1) / (maxValue - 1).toFloat()) * w
            drawRect(color = color.copy(alpha = 0.3f), size = Size(fillWidth, h))
            drawRect(color = color, topLeft = Offset(fillWidth - 2.dp.toPx(), 0f), size = Size(4.dp.toPx(), h))
        }
        Text(text = "$label $value", style = RasterTypography.labelSmall, color = Color.White,
            fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp),
            modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun DivisionSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Ordered musical divisions from slowest to fastest
    val orderedDivisions = listOf(
        0.25f to "¼×  𝅝",
        0.5f  to "½×  𝅗𝅥",
        1.0f  to "1×  𝅘𝅥",
        1.25f to "5/4 𝅘𝅥Q",
        1.5f  to "3/2 𝅘𝅥T",
        1.75f to "7/4 𝅘𝅥S",
        2.0f  to "2×  𝅘𝅥𝅮",
        4.0f  to "4×  𝅘𝅥𝅯"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val x = (offset.x / size.width).coerceIn(0f, 0.99f)
                    val index = (x * orderedDivisions.size).toInt()
                    onValueChange(orderedDivisions[index].first)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val x = (change.position.x / size.width).coerceIn(0f, 0.99f)
                    val index = (x * orderedDivisions.size).toInt()
                    onValueChange(orderedDivisions[index].first)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(color = Color(0xFF1A1A1E), size = Size(w, h))

            // Find current index to draw fill
            val currentIndex = orderedDivisions.indexOfFirst { it.first == value }.coerceAtLeast(0)
            val fillRatio = (currentIndex + 1) / orderedDivisions.size.toFloat()
            val fillWidth = fillRatio * w

            drawRect(color = color.copy(alpha = 0.3f), size = Size(fillWidth, h))
            drawRect(color = color, topLeft = Offset(fillWidth - 2.dp.toPx(), 0f), size = Size(4.dp.toPx(), h))
        }

        val speedText = orderedDivisions.find { it.first == value }?.second ?: "1×"
        Text(text = "SPD: $speedText", style = RasterTypography.labelSmall, color = Color.White,
            modifier = Modifier.align(Alignment.Center))
    }
}
