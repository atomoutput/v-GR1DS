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
import com.volcagrids.ui.theme.RasterDark
import com.volcagrids.ui.theme.RasterGrid

/**
 * Plaits Parameter Knobs
 * 7 rotary knobs for core synthesis parameters
 */
@Composable
fun PlaitsParameterKnobs(
    harmonics: Float,
    timbre: Float,
    morph: Float,
    fmAmount: Float,
    timbreModAmount: Float,
    morphModAmount: Float,
    decay: Float,
    onHarmonicsChange: (Float) -> Unit = {},
    onTimbreChange: (Float) -> Unit = {},
    onMorphChange: (Float) -> Unit = {},
    onFmAmountChange: (Float) -> Unit = {},
    onTimbreModAmountChange: (Float) -> Unit = {},
    onMorphModAmountChange: (Float) -> Unit = {},
    onDecayChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00B0FF)
) {
    Column(
        modifier = modifier
            .background(RasterDark)
            .padding(12.dp)
    ) {
        // Top row: Core parameters
        Text(
            text = "PARAMETERS",
            style = RasterTypography.headlineLarge,
            color = accentColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PlaitsKnob(
                label = "HARMONICS",
                value = harmonics,
                onValueChange = onHarmonicsChange,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
            PlaitsKnob(
                label = "TIMBRE",
                value = timbre,
                onValueChange = onTimbreChange,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
            PlaitsKnob(
                label = "MORPH",
                value = morph,
                onValueChange = onMorphChange,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Middle row: Modulation amounts
        Text(
            text = "MODULATION",
            style = RasterTypography.headlineLarge,
            color = accentColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PlaitsKnob(
                label = "FM AMT",
                value = fmAmount,
                onValueChange = onFmAmountChange,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
            PlaitsKnob(
                label = "TIMBRE MOD",
                value = timbreModAmount,
                onValueChange = onTimbreModAmountChange,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
            PlaitsKnob(
                label = "MORPH MOD",
                value = morphModAmount,
                onValueChange = onMorphModAmountChange,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom row: Envelope
        Text(
            text = "ENVELOPE",
            style = RasterTypography.headlineLarge,
            color = accentColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PlaitsKnob(
                label = "DECAY",
                value = decay,
                onValueChange = onDecayChange,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PlaitsKnob(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .border(1.dp, RasterGrid)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Label
        Text(
            text = label,
            style = RasterTypography.labelSmall,
            color = Color.Gray,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Knob
        Box(
            modifier = Modifier
                .size(56.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            val delta = (dragAmount.y / 100f)
                            val newValue = (value - delta).coerceIn(0f, 1f)
                            onValueChange(newValue)
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

                // Tick marks
                for (i in 0..10) {
                    val angleDeg = -135f + (270f * i / 10)
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val tickStart = radius - 2.dp.toPx()
                    val tickEnd = radius
                    drawLine(
                        color = Color(0xFF333333),
                        start = Offset(
                            (center.x + tickStart * Math.cos(angleRad)).toFloat(),
                            (center.y + tickStart * Math.sin(angleRad)).toFloat()
                        ),
                        end = Offset(
                            (center.x + tickEnd * Math.cos(angleRad)).toFloat(),
                            (center.y + tickEnd * Math.sin(angleRad)).toFloat()
                        ),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Value arc (270 degrees, from -135 to +135)
                val sweepAngle = 270f * value
                val startAngle = -135f
                drawArc(
                    color = accentColor.copy(alpha = 0.6f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Indicator line
                val angleRad = Math.toRadians((-135 + sweepAngle).toDouble())
                val indicatorLength = radius - 8.dp.toPx()
                drawLine(
                    color = if (isDragging) accentColor else Color(0xFFDDDDDD),
                    start = center,
                    end = Offset(
                        (center.x + indicatorLength * Math.cos(angleRad)).toFloat(),
                        (center.y + indicatorLength * Math.sin(angleRad)).toFloat()
                    ),
                    strokeWidth = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                // Center dot
                drawCircle(
                    color = accentColor.copy(alpha = 0.3f),
                    radius = 3.dp.toPx(),
                    center = center
                )
            }
        }

        // Value readout
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${(value * 100).toInt()}%",
            style = RasterTypography.labelSmall,
            color = accentColor
        )
    }
}

/**
 * Single large knob for focused parameter control
 */
@Composable
fun PlaitsSingleKnob(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00B0FF),
    showValue: Boolean = true
) {
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .border(1.dp, RasterGrid)
            .background(RasterDark)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = RasterTypography.headlineLarge,
            color = accentColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            val delta = (dragAmount.y / 150f)
                            val newValue = (value - delta).coerceIn(0f, 1f)
                            onValueChange(newValue)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val size = this.size
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2 - 6.dp.toPx()

                // Background
                drawCircle(
                    color = Color(0xFF1A1A1E),
                    radius = radius,
                    center = center
                )

                // Value arc
                val sweepAngle = 270f * value
                drawArc(
                    color = accentColor,
                    startAngle = -135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 5.dp.toPx())
                )

                // Indicator
                val angleRad = Math.toRadians((-135 + sweepAngle).toDouble())
                val indicatorLength = radius - 12.dp.toPx()
                drawLine(
                    color = if (isDragging) Color.White else accentColor,
                    start = center,
                    end = Offset(
                        (center.x + indicatorLength * Math.cos(angleRad)).toFloat(),
                        (center.y + indicatorLength * Math.sin(angleRad)).toFloat()
                    ),
                    strokeWidth = 4.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }

        if (showValue) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(value * 100).toInt()}%",
                style = RasterTypography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
