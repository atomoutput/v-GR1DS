package com.volcagrids.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.volcagrids.ui.theme.RasterTypography

@Composable
fun RasterPad(
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan,
    x: Int,
    y: Int,
    onPositionChange: (Int, Int) -> Unit
) {
    val shader = remember { GlitchShader() }
    // Generate a static noise map for now, dynamic updates can be expensive in a basic Canvas
    val noiseMap = remember { shader.generateNoiseMap(128, 128, 0.5f, 0.5f, color) }

    Box(
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw Glitch Background
            drawImage(
                image = noiseMap.asImageBitmap(),
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )

            val cursorX = (x / 255f) * size.width
            val cursorY = (y / 255f) * size.height
            
            // Minimalist "Crosshair" Cursor
            drawLine(
                color = color,
                start = Offset(cursorX, 0f),
                end = Offset(cursorX, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = color,
                start = Offset(0f, cursorY),
                end = Offset(size.width, cursorY),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Data Readout
        Text(
            text = "X:$x Y:$y",
            style = RasterTypography.labelSmall,
            color = color,
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
        )
    }
}
