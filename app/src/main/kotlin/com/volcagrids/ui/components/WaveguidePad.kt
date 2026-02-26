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
 * Waveguide Resonator Pad - Complete Implementation
 * X-axis: Decay (CC117) 0-127
 * Y-axis: Body (CC118) 0-127
 * Optional: Tune (CC119) via modifier
 */
@Composable
fun WaveguidePad(
    modifier: Modifier = Modifier,
    decay: Int = 64,
    body: Int = 64,
    tune: Int = 64,
    onValueChange: (Int, Int, Int) -> Unit = { _, _, _ -> },
    showGrid: Boolean = true,
    accentColor: Color = Color(0xFFFF5722) // Amber for waveguide
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .background(Color(0xFF1A1A1E))
            .border(1.dp, Color(0xFF333333))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDrag = { change, _ ->
                        val x = (change.position.x / size.width * 127).toInt().coerceIn(0, 127)
                        val y = (1f - change.position.y / size.height) * 127f // Invert Y
                        val tuneValue = tune // Could add vertical zones for tune
                        onValueChange(x, y.toInt(), tuneValue)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // Draw grid
            if (showGrid) {
                val gridSize = 16
                for (i in 0..gridSize) {
                    val x = (i.toFloat() / gridSize) * w
                    val y = (i.toFloat() / gridSize) * h
                    drawLine(
                        color = Color(0xFF2A2A2E),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0xFF2A2A2E),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }
            }
            
            // Draw center point marker
            drawCircle(
                color = accentColor.copy(alpha = 0.3f),
                radius = 8.dp.toPx(),
                center = Offset(w / 2, h / 2)
            )
            
            // Draw current position cursor
            val cursorX = (decay / 127f) * w
            val cursorY = (1f - body / 127f) * h
            
            // Crosshair
            drawLine(
                color = accentColor,
                start = Offset(cursorX, 0f),
                end = Offset(cursorX, h),
                strokeWidth = 1f
            )
            drawLine(
                color = accentColor,
                start = Offset(0f, cursorY),
                end = Offset(w, cursorY),
                strokeWidth = 1f
            )
            
            // Cursor circle with glow
            drawCircle(
                color = accentColor.copy(alpha = 0.3f),
                radius = 20.dp.toPx(),
                center = Offset(cursorX, cursorY)
            )
            drawCircle(
                color = accentColor,
                radius = 6.dp.toPx(),
                center = Offset(cursorX, cursorY)
            )
            
            // Draw axis labels on canvas edges
            drawRect(
                color = accentColor.copy(alpha = 0.5f),
                topLeft = Offset(0f, h - 4f),
                size = Size(cursorX, 4f)
            )
            drawRect(
                color = accentColor.copy(alpha = 0.5f),
                topLeft = Offset(w - 4f, cursorY),
                size = Size(4f, h - cursorY)
            )
        }
        
        // Top-left: Parameter readout
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = "DECAY:$decay",
                style = RasterTypography.labelSmall,
                color = accentColor
            )
            Text(
                text = "BODY:$body",
                style = RasterTypography.labelSmall,
                color = accentColor
            )
            Text(
                text = "TUNE:$tune",
                style = RasterTypography.labelSmall,
                color = accentColor.copy(alpha = 0.7f)
            )
        }
        
        // Bottom-right: Mode indicator
        Text(
            text = "[WAVEGUIDE]",
            style = RasterTypography.labelSmall,
            color = if (isDragging) accentColor else Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        )
    }
}

/**
 * Compact Waveguide Pad for limited space
 */
@Composable
fun WaveguidePadCompact(
    modifier: Modifier = Modifier,
    decay: Int = 64,
    body: Int = 64,
    onValueChange: (Int, Int) -> Unit = { _, _ -> }
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .background(Color(0xFF1A1A1E))
            .border(1.dp, Color(0xFF333333))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDrag = { change, _ ->
                        val x = (change.position.x / size.width * 127).toInt().coerceIn(0, 127)
                        val y = (1f - change.position.y / size.height) * 127f
                        onValueChange(x, y.toInt())
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // Minimal grid
            drawLine(
                color = Color(0xFF2A2A2E),
                start = Offset(w / 2, 0f),
                end = Offset(w / 2, h),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0xFF2A2A2E),
                start = Offset(0f, h / 2),
                end = Offset(w, h / 2),
                strokeWidth = 1f
            )
            
            // Cursor
            val cursorX = (decay / 127f) * w
            val cursorY = (1f - body / 127f) * h
            
            drawCircle(
                color = Color(0xFFFF5722),
                radius = 4.dp.toPx(),
                center = Offset(cursorX, cursorY)
            )
        }
        
        Text(
            text = "D:$decay B:$body",
            style = RasterTypography.labelSmall,
            color = Color(0xFFFF5722),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
        )
    }
}
