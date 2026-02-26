package com.volcagrids.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun TopographyPad(
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan,
    x: Int,
    y: Int,
    onPositionChange: (Int, Int) -> Unit
) {
    val visualizer = remember { MapVisualizer() }
    val heatmap = remember { visualizer.generateHeatmap(128, 128, color.toArgb()) }

    Canvas(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val px = change.position.x.coerceIn(0f, w)
                    val py = change.position.y.coerceIn(0f, h)
                    
                    val normalizedX = (px / w * 255).toInt()
                    val normalizedY = (py / h * 255).toInt()
                    onPositionChange(normalizedX, normalizedY)
                }
            }
    ) {
        // Draw Heatmap background
        drawImage(
            image = heatmap.asImageBitmap(),
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )

        val cursorX = (x / 255f) * size.width
        val cursorY = (y / 255f) * size.height
        
        // Render the Cursor
        drawCircle(
            color = color,
            radius = 12.dp.toPx(),
            center = Offset(cursorX, cursorY)
        )
        
        // Render a faint glowing halo
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = 24.dp.toPx(),
            center = Offset(cursorX, cursorY)
        )
        
        // Render Crosshairs
        drawLine(
            color = color.copy(alpha = 0.2f),
            start = Offset(cursorX, 0f),
            end = Offset(cursorX, size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = color.copy(alpha = 0.2f),
            start = Offset(0f, cursorY),
            end = Offset(size.width, cursorY),
            strokeWidth = 1.dp.toPx()
        )
    }
}
