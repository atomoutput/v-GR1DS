package com.volcagrids.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.volcagrids.ui.theme.RasterTypography

/**
 * Layer Morph Knobs - 6 rotary knobs for crossfading between Layer 1 and Layer 2
 * Each knob sends inverse CC17/CC18 pairs to Volca Drum
 * Range: 0 (Layer 1 full) to 127 (Layer 2 full)
 */
@Composable
fun MorphKnobs(
    modifier: Modifier = Modifier,
    values: List<Int> = listOf(64, 64, 64, 64, 64, 64),
    onValueChange: (Int, Int) -> Unit = { _, _ -> },
    accentColor: Color = Color(0xFF00B0FF) // Ice blue
) {
    Row(
        modifier = modifier
            .background(Color(0xFF121214))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        values.forEachIndexed { index, value ->
            MorphKnob(
                partIndex = index,
                value = value,
                onValueChange = onValueChange,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MorphKnob(
    partIndex: Int,
    value: Int,
    onValueChange: (Int, Int) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .border(1.dp, Color(0xFF2A2A2E))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Part label
        Text(
            text = "P${partIndex + 1}",
            style = RasterTypography.labelSmall,
            color = Color.Gray
        )
        
        // Rotary knob
        Box(
            modifier = Modifier
                .size(48.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            // Vertical drag changes value
                            val delta = (dragAmount.y / 2).toInt()
                            val newValue = (value - delta).coerceIn(0, 127)
                            onValueChange(partIndex, newValue)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val size = this.size
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2 - 4.dp.toPx()
                
                // Background circle
                drawCircle(
                    color = Color(0xFF1A1A1E),
                    radius = radius,
                    center = center
                )
                
                // Value arc (270 degrees total, from -135 to +135)
                val sweepAngle = 270f * (value / 127f)
                val startAngle = -135f
                drawArc(
                    color = accentColor.copy(alpha = 0.5f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Indicator line
                val angle = Math.toRadians((startAngle + sweepAngle).toDouble())
                val indicatorLength = radius - 6.dp.toPx()
                val indicatorEnd = Offset(
                    (center.x + indicatorLength * Math.cos(angle)).toFloat(),
                    (center.y + indicatorLength * Math.sin(angle)).toFloat()
                )
                drawLine(
                    color = if (isDragging) accentColor else Color(0xFFDDDDDD),
                    start = center,
                    end = indicatorEnd,
                    strokeWidth = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                
                // Center dot
                drawCircle(
                    color = accentColor.copy(alpha = 0.3f),
                    radius = 4.dp.toPx(),
                    center = center
                )
            }
        }
        
        // Value indicator
        val layerIndicator = when {
            value < 42 -> "A"
            value > 85 -> "B"
            else -> "◐"
        }
        Text(
            text = layerIndicator,
            style = RasterTypography.labelSmall,
            color = if (value < 42) Color(0xFFFF5722) else if (value > 85) accentColor else Color.Gray,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * Single Morph Knob with label
 */
@Composable
fun MorphKnobSingle(
    modifier: Modifier = Modifier,
    label: String = "MORPH",
    value: Int = 64,
    onValueChange: (Int) -> Unit = {},
    accentColor: Color = Color(0xFF00B0FF)
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .border(1.dp, Color(0xFF2A2A2E))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = RasterTypography.labelSmall,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            val delta = (dragAmount.y / 3).toInt()
                            val newValue = (value - delta).coerceIn(0, 127)
                            onValueChange(newValue)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val size = this.size
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2 - 4.dp.toPx()
                
                // Background
                drawCircle(
                    color = Color(0xFF1A1A1E),
                    radius = radius,
                    center = center
                )
                
                // Tick marks
                for (i in 0..10) {
                    val angle = Math.toRadians(-135 + (270 * i / 10).toDouble())
                    val tickStart = radius - 2.dp.toPx()
                    val tickEnd = radius
                    drawLine(
                        color = Color(0xFF333333),
                        start = Offset(
                            (center.x + tickStart * Math.cos(angle)).toFloat(),
                            (center.y + tickStart * Math.sin(angle)).toFloat()
                        ),
                        end = Offset(
                            (center.x + tickEnd * Math.cos(angle)).toFloat(),
                            (center.y + tickEnd * Math.sin(angle)).toFloat()
                        ),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Value arc
                val sweepAngle = 270f * (value / 127f)
                drawArc(
                    color = accentColor,
                    startAngle = -135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 4.dp.toPx())
                )
                
                // Indicator
                val angle = Math.toRadians((-135 + sweepAngle).toDouble())
                val indicatorLength = radius - 8.dp.toPx()
                drawLine(
                    color = if (isDragging) Color.White else accentColor,
                    start = center,
                    end = Offset(
                        (center.x + indicatorLength * Math.cos(angle)).toFloat(),
                        (center.y + indicatorLength * Math.sin(angle)).toFloat()
                    ),
                    strokeWidth = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Value readout
        Text(
            text = if (value < 64) "L1:${127 - value}" else "L2:$value",
            style = RasterTypography.labelSmall,
            color = accentColor
        )
    }
}
