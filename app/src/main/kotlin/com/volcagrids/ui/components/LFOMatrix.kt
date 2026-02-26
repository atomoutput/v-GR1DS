package com.volcagrids.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.volcagrids.midi.VolcaParameter
import com.volcagrids.ui.theme.RasterTypography
import kotlin.math.PI
import kotlin.math.sin

/**
 * LFO Matrix (Ghost Hands) - Virtual LFOs for parameter modulation
 * 4 LFOs assignable to X/Y/Chaos/CC destinations
 */

enum class LFOWaveform {
    SINE,
    TRIANGLE,
    SAW,
    SQUARE,
    S_AND_H,
    STEP
}

enum class LFODestination {
    NONE,
    ENGINE_A_X,
    ENGINE_A_Y,
    ENGINE_A_RANDOMNESS,
    ENGINE_B_X,
    ENGINE_B_Y,
    ENGINE_B_RANDOMNESS,
    CC_CUSTOM
}

data class LFOConfig(
    val index: Int = 0,
    val waveform: LFOWaveform = LFOWaveform.SINE,
    val rate: Float = 1f, // Hz or beat-synced
    val depth: Float = 0.5f, // 0-1
    val phase: Float = 0f, // 0-1
    val destination: LFODestination = LFODestination.NONE,
    val targetCC: VolcaParameter? = null,
    val enabled: Boolean = false
)

@Composable
fun LFOMatrix(
    modifier: Modifier = Modifier,
    lfos: List<LFOConfig> = listOf(
        LFOConfig(0),
        LFOConfig(1),
        LFOConfig(2),
        LFOConfig(3)
    ),
    onLFOChange: (LFOConfig) -> Unit = {},
    accentColor: Color = Color(0xFFFFD600) // Gold for LFO
) {
    Column(
        modifier = modifier
            .background(Color.Transparent)
            .padding(8.dp)
    ) {
        Text(
            text = "LFO_MATRIX",
            style = RasterTypography.labelSmall,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        lfos.forEach { lfo ->
            LFOSlot(
                lfo = lfo,
                onLFOChange = onLFOChange,
                accentColor = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun LFOSlot(
    lfo: LFOConfig,
    onLFOChange: (LFOConfig) -> Unit,
    accentColor: Color
) {
    var isDraggingDepth by remember { mutableStateOf(false) }
    var isDraggingRate by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(
                1.dp,
                if (lfo.enabled) accentColor else Color(0xFF2A2A2E)
            )
            .background(
                if (lfo.enabled) accentColor.copy(alpha = 0.05f) else Color.Transparent
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LFO number and enable toggle
        Text(
            text = "LFO${lfo.index + 1}",
            style = RasterTypography.labelSmall,
            color = if (lfo.enabled) accentColor else Color.Gray,
            modifier = Modifier
                .width(40.dp)
                .clickable {
                    onLFOChange(lfo.copy(enabled = !lfo.enabled))
                }
        )
        
        // Waveform selector
        Box(
            modifier = Modifier
                .clickable {
                    val nextWaveform = when (lfo.waveform) {
                        LFOWaveform.SINE -> LFOWaveform.TRIANGLE
                        LFOWaveform.TRIANGLE -> LFOWaveform.SAW
                        LFOWaveform.SAW -> LFOWaveform.SQUARE
                        LFOWaveform.SQUARE -> LFOWaveform.S_AND_H
                        LFOWaveform.S_AND_H -> LFOWaveform.STEP
                        LFOWaveform.STEP -> LFOWaveform.SINE
                    }
                    onLFOChange(lfo.copy(waveform = nextWaveform))
                }
        ) {
            Canvas(
                modifier = Modifier
                    .size(24.dp)
            ) {
                drawWaveform(
                    waveform = lfo.waveform,
                    color = if (lfo.enabled) accentColor else Color.Gray,
                    size = size
                )
            }
        }
        
        // Rate control
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDraggingRate = true },
                        onDragEnd = { isDraggingRate = false },
                        onDrag = { change, dragAmount ->
                            val delta = -dragAmount.x / 50f
                            onLFOChange(lfo.copy(rate = (lfo.rate + delta).coerceIn(0.1f, 20f)))
                        }
                    )
                }
        ) {
            Text("RATE", style = RasterTypography.labelSmall, color = Color.Gray, fontSize = androidx.compose.ui.unit.TextUnit(6f, androidx.compose.ui.unit.TextUnitType.Sp))
            Text(
                text = "${String.format("%.1f", lfo.rate)}Hz",
                style = RasterTypography.labelSmall,
                color = if (isDraggingRate) accentColor else Color.White,
                fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }
        
        // Depth control
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDraggingDepth = true },
                        onDragEnd = { isDraggingDepth = false },
                        onDrag = { change, dragAmount ->
                            val delta = -dragAmount.x / 100f
                            onLFOChange(lfo.copy(depth = (lfo.depth + delta).coerceIn(0f, 1f)))
                        }
                    )
                }
        ) {
            Text("DEPTH", style = RasterTypography.labelSmall, color = Color.Gray, fontSize = androidx.compose.ui.unit.TextUnit(6f, androidx.compose.ui.unit.TextUnitType.Sp))
            Text(
                text = "${(lfo.depth * 100).toInt()}%",
                style = RasterTypography.labelSmall,
                color = if (isDraggingDepth) accentColor else Color.White,
                fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }
        
        // Destination selector
        Box(
            modifier = Modifier
                .weight(1.5f)
                .clickable {
                    val nextDest = when (lfo.destination) {
                        LFODestination.NONE -> LFODestination.ENGINE_A_X
                        LFODestination.ENGINE_A_X -> LFODestination.ENGINE_A_Y
                        LFODestination.ENGINE_A_Y -> LFODestination.ENGINE_A_RANDOMNESS
                        LFODestination.ENGINE_A_RANDOMNESS -> LFODestination.ENGINE_B_X
                        LFODestination.ENGINE_B_X -> LFODestination.ENGINE_B_Y
                        LFODestination.ENGINE_B_Y -> LFODestination.ENGINE_B_RANDOMNESS
                        LFODestination.ENGINE_B_RANDOMNESS -> LFODestination.CC_CUSTOM
                        LFODestination.CC_CUSTOM -> LFODestination.NONE
                    }
                    onLFOChange(lfo.copy(destination = nextDest, enabled = nextDest != LFODestination.NONE))
                }
        ) {
            Text(
                text = lfo.destination.name.take(8),
                style = RasterTypography.labelSmall,
                color = if (lfo.enabled) accentColor else Color.Gray,
                fontSize = androidx.compose.ui.unit.TextUnit(7f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }
    }
}

@Composable
private fun LFOWaveformPreview(
    modifier: Modifier = Modifier,
    waveform: LFOWaveform,
    rate: Float,
    phase: Float,
    accentColor: Color
) {
    Canvas(
        modifier = modifier
            .background(Color.Transparent)
    ) {
        val w = size.width
        val h = size.height
        val centerY = h / 2
        val amplitude = h / 2 - 4.dp.toPx()
        
        val path = androidx.compose.ui.graphics.Path()
        
        // Draw waveform for current phase position
        val samples = 100
        for (i in 0..samples) {
            val x = (i.toFloat() / samples) * w
            val t = (i.toFloat() / samples) * 2 * PI * rate + phase * 2 * PI
            
            val y = when (waveform) {
                LFOWaveform.SINE -> centerY - amplitude * sin(t).toFloat()
                LFOWaveform.TRIANGLE -> centerY - amplitude * (2 * kotlin.math.abs((t / (2 * PI)) % 2 - 1) - 1).toFloat()
                LFOWaveform.SAW -> centerY - amplitude * (2 * ((t / (2 * PI)) % 2) - 1).toFloat()
                LFOWaveform.SQUARE -> centerY - amplitude * (if (sin(t) > 0) 1 else -1).toFloat()
                LFOWaveform.S_AND_H -> {
                    // Random sample and hold
                    val sampleIndex = (t / (2 * PI) * 4).toInt()
                    val value = (sampleIndex * 12345 % 1000) / 1000f * 2 - 1
                    centerY - amplitude * value
                }
                LFOWaveform.STEP -> {
                    // Stepped waveform
                    val stepIndex = (t / (2 * PI) * 8).toInt()
                    val value = (stepIndex % 8) / 4f - 1
                    centerY - amplitude * value
                }
            }
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun drawWaveform(
    waveform: LFOWaveform,
    color: Color,
    size: Size
) {
    val w = size.width
    val h = size.height
    val centerY = h / 2
    val amplitude = h / 2 - 2f // Fixed pixel value instead of dp.toPx()
    
    val path = androidx.compose.ui.graphics.Path()
    val samples = 20
    
    for (i in 0..samples) {
        val x = (i.toFloat() / samples) * w
        val t = (i.toFloat() / samples) * 2 * PI
        
        val y = when (waveform) {
            LFOWaveform.SINE -> centerY - amplitude * sin(t).toFloat()
            LFOWaveform.TRIANGLE -> centerY - amplitude * (2 * kotlin.math.abs((t / (2 * PI)) % 2 - 1) - 1).toFloat()
            LFOWaveform.SAW -> centerY - amplitude * (2 * ((t / (2 * PI)) % 2) - 1).toFloat()
            LFOWaveform.SQUARE -> centerY - amplitude * (if (sin(t) > 0) 1 else -1).toFloat()
            LFOWaveform.S_AND_H -> centerY - amplitude * (if (i < samples / 2) 0.5f else -0.5f)
            LFOWaveform.STEP -> centerY - amplitude * ((i % 5) / 2f - 1).toFloat()
        }
        
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    
    // In a real Canvas context, we would draw the path here
    // This is a simplified version for the small icon
}

/**
 * LFO Value Indicator - Shows current LFO output value
 */
@Composable
fun LFOValueIndicator(
    modifier: Modifier = Modifier,
    value: Float, // -1 to 1
    label: String,
    accentColor: Color
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .width(40.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDrag = { _, _ -> }
                )
            }
    ) {
        Text(
            text = label,
            style = RasterTypography.labelSmall,
            color = Color.Gray,
            fontSize = androidx.compose.ui.unit.TextUnit(7f, androidx.compose.ui.unit.TextUnitType.Sp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .border(1.dp, if (isDragging) accentColor else Color(0xFF333333))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // Center line
                drawLine(
                    color = Color(0xFF333333),
                    start = Offset(0f, h / 2),
                    end = Offset(w, h / 2),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Value indicator
                val y = h / 2 - (value * h / 2)
                drawCircle(
                    color = if (isDragging) accentColor else Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(w / 2, y)
                )
            }
        }
        
        Text(
            text = String.format("%+.2f", value),
            style = RasterTypography.labelSmall,
            color = accentColor,
            fontSize = androidx.compose.ui.unit.TextUnit(7f, androidx.compose.ui.unit.TextUnitType.Sp)
        )
    }
}
