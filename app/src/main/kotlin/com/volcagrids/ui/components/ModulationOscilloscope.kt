package com.volcagrids.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import com.volcagrids.engine.EnvelopeShape
import com.volcagrids.ui.theme.RasterTypography

/**
 * Modulation Oscilloscope - Real-time visualization of parameter modulation
 * Shows the actual waveform being applied to the parameter
 */
@Composable
fun ModulationOscilloscope(
    currentValue: Float,           // Current normalized value (0.0 - 1.0)
    targetValue: Float,            // Target normalized value (0.0 - 1.0)
    stepPosition: Float,           // Current step position (0.0 - 1.0)
    envelopeShape: EnvelopeShape,
    parameterName: String,
    partIndex: Int,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00FF00)
) {
    // Oscilloscope trace history
    val maxHistory = 128
    val traceHistory = remember { mutableStateListOf<Float>() }
    
    // Update trace
    LaunchedEffect(currentValue) {
        traceHistory.add(currentValue)
        if (traceHistory.size > maxHistory) {
            traceHistory.removeAt(0)
        }
    }
    
    // Infinite transition for grid animation
    val infiniteTransition = rememberInfiniteTransition(label = "scope")
    val gridPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gridPhase"
    )

    Box(
        modifier = modifier
            .background(Color(0xFF0A0A0A))
            .border(1.dp, Color(0xFF333333))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // 1. Draw Grid (Oscilloscope-style)
            val gridColor = Color(0xFF1A1A1A)
            val majorGridColor = Color(0xFF2A2A2A)
            
            // Vertical divisions (time/steps)
            for (i in 0..16) {
                val x = (i / 16f) * w
                val isMajor = i % 4 == 0
                drawLine(
                    color = if (isMajor) majorGridColor else gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = if (isMajor) 1.5f else 0.5f
                )
            }
            
            // Horizontal divisions (value)
            for (i in 0..8) {
                val y = (i / 8f) * h
                val isMajor = i % 2 == 0
                drawLine(
                    color = if (isMajor) majorGridColor else gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (isMajor) 1.5f else 0.5f
                )
            }
            
            // 2. Draw Trace History (Oscilloscope waveform)
            if (traceHistory.size > 1) {
                val tracePath = androidx.compose.ui.graphics.Path()
                val segmentWidth = w / maxHistory
                
                tracePath.moveTo(0f, h - (traceHistory[0] * h))
                
                for (i in 1 until traceHistory.size) {
                    val x = i * segmentWidth
                    val y = h - (traceHistory[i] * h)
                    tracePath.lineTo(x, y)
                }
                
                // Glow effect
                drawPath(
                    path = tracePath,
                    color = accentColor.copy(alpha = 0.3f),
                    style = Stroke(width = 8f)
                )
                
                // Main trace
                drawPath(
                    path = tracePath,
                    color = accentColor,
                    style = Stroke(width = 2f)
                )
            }
            
            // 3. Draw Current Value Indicator (bright dot)
            val currentX = w - 10f
            val currentY = h - (currentValue * h)
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = Offset(currentX, currentY)
            )
            
            // 4. Draw Target Value Line (dashed)
            val targetY = h - (targetValue * h)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), gridPhase * 10)
            drawLine(
                color = accentColor.copy(alpha = 0.5f),
                start = Offset(0f, targetY),
                end = Offset(w, targetY),
                strokeWidth = 1f,
                pathEffect = dashEffect
            )
            
            // 5. Draw Step Position Marker (vertical line on right side)
            val stepX = (stepPosition * w * 0.3f) + (w * 0.7f)  // Right 30% of screen
            drawLine(
                color = Color(0xFFFF5722),
                start = Offset(stepX, 0f),
                end = Offset(stepX, h),
                strokeWidth = 2f
            )
            
            // 6. Draw Envelope Shape Indicator
            drawShapeIndicator(envelopeShape, Offset(15f, 15f), 20f)
        }
        
        // Parameter Label
        Text(
            text = "MOD // $parameterName",
            style = RasterTypography.labelSmall,
            color = accentColor,
            modifier = Modifier.align(Alignment.TopStart)
        )
        
        // Value Readout
        Text(
            text = "${(currentValue * 127).toInt()}",
            style = RasterTypography.labelSmall,
            color = Color.White,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

/**
 * Draw envelope shape indicator
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShapeIndicator(
    shape: EnvelopeShape,
    position: Offset,
    size: Float
) {
    val color = when (shape) {
        EnvelopeShape.SMOOTH -> Color(0xFF00FF00)
        EnvelopeShape.EXPONENTIAL -> Color(0xFFFF9800)
        EnvelopeShape.STEPPED -> Color(0xFF00BCD4)
        EnvelopeShape.RANDOM -> Color(0xFFE91E63)
    }
    
    when (shape) {
        EnvelopeShape.SMOOTH -> {
            // Sine wave icon
            drawLine(
                color = color,
                start = Offset(position.x, position.y + size/2),
                end = Offset(position.x + size, position.y + size/2),
                strokeWidth = 1.5f
            )
        }
        EnvelopeShape.EXPONENTIAL -> {
            // Curve icon
            drawLine(
                color = color,
                start = Offset(position.x, position.y + size),
                end = Offset(position.x + size, position.y),
                strokeWidth = 1.5f
            )
        }
        EnvelopeShape.STEPPED -> {
            // Step icon
            drawLine(
                color = color,
                start = Offset(position.x, position.y + size),
                end = Offset(position.x + size/2, position.y + size),
                strokeWidth = 1.5f
            )
            drawLine(
                color = color,
                start = Offset(position.x + size/2, position.y + size),
                end = Offset(position.x + size/2, position.y),
                strokeWidth = 1.5f
            )
            drawLine(
                color = color,
                start = Offset(position.x + size/2, position.y),
                end = Offset(position.x + size, position.y),
                strokeWidth = 1.5f
            )
        }
        EnvelopeShape.RANDOM -> {
            // Random icon (zigzag)
            drawLine(
                color = color,
                start = Offset(position.x, position.y + size/2),
                end = Offset(position.x + size/3, position.y),
                strokeWidth = 1.5f
            )
            drawLine(
                color = color,
                start = Offset(position.x + size/3, position.y),
                end = Offset(position.x + 2*size/3, position.y + size),
                strokeWidth = 1.5f
            )
            drawLine(
                color = color,
                start = Offset(position.x + 2*size/3, position.y + size),
                end = Offset(position.x + size, position.y + size/2),
                strokeWidth = 1.5f
            )
        }
    }
}

/**
 * Compact modulation preview for inline display
 */
@Composable
fun ModulationPreview(
    currentValue: Float,
    envelopeShape: EnvelopeShape,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00FF00)
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(Color(0xFF0A0A0A))
            .border(1.dp, Color(0xFF333333))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // Draw mini waveform
            val points = 32
            for (i in 0 until points) {
                val x = (i / points.toFloat()) * w
                val phase = i / points.toFloat()
                
                // Simulate waveform based on shape
                val y = when (envelopeShape) {
                    EnvelopeShape.SMOOTH -> {
                        h - (currentValue * h * 0.8f) - (h * 0.1f * kotlin.math.sin(phase * Math.PI * 4).toFloat())
                    }
                    EnvelopeShape.EXPONENTIAL -> {
                        h - (currentValue * h * Math.pow(phase.toDouble(), 3.0).toFloat() * 0.9f) - (h * 0.05f)
                    }
                    EnvelopeShape.STEPPED -> {
                        h - (currentValue * h * 0.9f) - (h * 0.05f)
                    }
                    EnvelopeShape.RANDOM -> {
                        h - (currentValue * h * 0.7f) - (h * 0.2f * (kotlin.random.Random.nextFloat() - 0.5f))
                    }
                }
                
                drawCircle(
                    color = color,
                    radius = 1.5f,
                    center = Offset(x, y)
                )
            }
        }
    }
}
