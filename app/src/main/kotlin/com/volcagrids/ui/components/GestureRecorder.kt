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
import com.volcagrids.ui.theme.RasterTypography

/**
 * Gesture Recorder - Record and loop XY pad movements
 * "Ghost Finger" feature that retraces recorded paths
 */

data class GesturePoint(
    val x: Float,
    val y: Float,
    val timestamp: Long
)

data class RecordedGesture(
    val points: List<GesturePoint>,
    val duration: Long,
    val name: String = ""
)

@Composable
fun GestureRecorder(
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
    isPlaying: Boolean = false,
    currentGesture: RecordedGesture?,
    recordedGestures: List<RecordedGesture> = emptyList(),
    selectedGestureIndex: Int = -1,
    onRecordToggle: () -> Unit = {},
    onPlayToggle: () -> Unit = {},
    onStop: () -> Unit = {},
    onSelectGesture: (Int) -> Unit = {},
    onClearGesture: () -> Unit = {},
    accentColor: Color = Color(0xFFFF5722) // Amber for gesture
) {
    Column(
        modifier = modifier
            .background(Color(0xFF121214))
            .border(1.dp, Color(0xFF2A2A2E))
            .padding(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GESTURE_RECORDER",
                style = RasterTypography.labelSmall,
                color = accentColor
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Record button
                Text(
                    text = if (isRecording) "[REC●]" else "[REC]",
                    style = RasterTypography.labelSmall,
                    color = if (isRecording) Color.Red else Color.Gray,
                    modifier = Modifier.clickable { onRecordToggle() }
                )
                
                // Play button
                Text(
                    text = if (isPlaying) "[PLAY●]" else "[PLAY]",
                    style = RasterTypography.labelSmall,
                    color = if (isPlaying) accentColor else Color.Gray,
                    modifier = Modifier.clickable { onPlayToggle() }
                )
                
                // Stop button
                Text(
                    text = "[STOP]",
                    style = RasterTypography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.clickable { onStop() }
                )
            }
        }
        
        // Gesture visualization
        if (currentGesture != null && currentGesture.points.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFF1A1A1E))
                    .border(1.dp, Color(0xFF333333))
            ) {
                val w = size.width
                val h = size.height
                
                if (currentGesture.points.size > 1) {
                    val path = androidx.compose.ui.graphics.Path()
                    
                    // Normalize and draw gesture path
                    val minX = currentGesture.points.minOf { it.x }
                    val maxX = currentGesture.points.maxOf { it.x }
                    val minY = currentGesture.points.minOf { it.y }
                    val maxY = currentGesture.points.maxOf { it.y }
                    
                    val rangeX = maxX - minX
                    val rangeY = maxY - minY
                    
                    path.moveTo(
                        ((currentGesture.points[0].x - minX) / rangeX * (w - 16) + 8).coerceIn(0f, w),
                        ((currentGesture.points[0].y - minY) / rangeY * (h - 16) + 8).coerceIn(0f, h)
                    )
                    
                    for (i in 1 until currentGesture.points.size) {
                        val point = currentGesture.points[i]
                        val nx = ((point.x - minX) / rangeX * (w - 16) + 8).coerceIn(0f, w)
                        val ny = ((point.y - minY) / rangeY * (h - 16) + 8).coerceIn(0f, h)
                        path.lineTo(nx, ny)
                    }
                    
                    // Draw path
                    drawPath(
                        path = path,
                        color = accentColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                    
                    // Draw points
                    currentGesture.points.forEachIndexed { index, point ->
                        val nx = ((point.x - minX) / rangeX * (w - 16) + 8).coerceIn(0f, w)
                        val ny = ((point.y - minY) / rangeY * (h - 16) + 8).coerceIn(0f, h)
                        drawCircle(
                            color = if (index == currentGesture.points.size - 1) Color.White else accentColor,
                            radius = if (index == currentGesture.points.size - 1) 4.dp.toPx() else 2.dp.toPx(),
                            center = Offset(nx, ny)
                        )
                    }
                }
            }
            
            // Duration display
            Text(
                text = "${currentGesture.duration}ms",
                style = RasterTypography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            // Duration and point count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DURATION:${currentGesture.duration}ms",
                    style = RasterTypography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = "POINTS:${currentGesture.points.size}",
                    style = RasterTypography.labelSmall,
                    color = Color.Gray
                )
            }
        } else {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFF1A1A1E))
                    .border(1.dp, Color(0xFF333333)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[NO_GESTURE_RECORDED]",
                    style = RasterTypography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        
        // Saved gestures list
        if (recordedGestures.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "SAVED_GESTURES:",
                style = RasterTypography.labelSmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            recordedGestures.forEachIndexed { index, gesture ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectGesture(index) }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${index + 1}. ${gesture.name.ifEmpty { "GESTURE_${index + 1}" }}",
                        style = RasterTypography.labelSmall,
                        color = if (index == selectedGestureIndex) accentColor else Color.White
                    )
                    Text(
                        text = "${gesture.duration}ms",
                        style = RasterTypography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Clear button
            Text(
                text = "[CLEAR_ALL]",
                style = RasterTypography.labelSmall,
                color = Color(0xFFFF5757),
                modifier = Modifier.clickable { onClearGesture() }
            )
        }
        
        // Instructions
        Text(
            text = "DRAG_TO_RECORD // PLAY_LOOPS",
            style = RasterTypography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Ghost Finger Visualization - Shows recorded path being retraced
 */
@Composable
fun GhostFingerOverlay(
    modifier: Modifier = Modifier,
    gesture: RecordedGesture?,
    playbackProgress: Float, // 0.0 to 1.0
    padBounds: Size,
    accentColor: Color = Color(0xFFFF5722)
) {
    if (gesture == null || gesture.points.isEmpty()) return
    
    Canvas(modifier = modifier) {
        val w = padBounds.width
        val h = padBounds.height
        
        // Normalize gesture to pad bounds
        val minX = gesture.points.minOf { it.x }
        val maxX = gesture.points.maxOf { it.x }
        val minY = gesture.points.minOf { it.y }
        val maxY = gesture.points.maxOf { it.y }
        
        val rangeX = maxX - minX
        val rangeY = maxY - minY
        
        // Calculate current playback position
        val currentIndex = (playbackProgress * (gesture.points.size - 1)).toInt()
        
        // Draw completed path (faded)
        val path = androidx.compose.ui.graphics.Path()
        
        if (gesture.points.isNotEmpty()) {
            val firstPoint = gesture.points[0]
            val nx = ((firstPoint.x - minX) / rangeX * w).coerceIn(0f, w)
            val ny = ((firstPoint.y - minY) / rangeY * h).coerceIn(0f, h)
            path.moveTo(nx, ny)
            
            for (i in 1..currentIndex.coerceAtLeast(0)) {
                val point = gesture.points[i]
                val nx2 = ((point.x - minX) / rangeX * w).coerceIn(0f, w)
                val ny2 = ((point.y - minY) / rangeY * h).coerceIn(0f, h)
                path.lineTo(nx2, ny2)
            }
            
            // Draw completed portion
            drawPath(
                path = path,
                color = accentColor.copy(alpha = 0.3f),
                style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            
            // Draw remaining path (dotted)
            val remainingPath = androidx.compose.ui.graphics.Path()
            if (currentIndex < gesture.points.size - 1) {
                val currentPoint = gesture.points[currentIndex]
                val cx = ((currentPoint.x - minX) / rangeX * w).coerceIn(0f, w)
                val cy = ((currentPoint.y - minY) / rangeY * h).coerceIn(0f, h)
                remainingPath.moveTo(cx, cy)
                
                for (i in (currentIndex + 1) until gesture.points.size) {
                    val point = gesture.points[i]
                    val nx = ((point.x - minX) / rangeX * w).coerceIn(0f, w)
                    val ny = ((point.y - minY) / rangeY * h).coerceIn(0f, h)
                    remainingPath.lineTo(nx, ny)
                }
                
                drawPath(
                    path = remainingPath,
                    color = accentColor.copy(alpha = 0.2f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                )
            }
            
            // Draw current position (bright dot)
            if (currentIndex < gesture.points.size) {
                val currentPoint = gesture.points[currentIndex]
                val cx = ((currentPoint.x - minX) / rangeX * w).coerceIn(0f, w)
                val cy = ((currentPoint.y - minY) / rangeY * h).coerceIn(0f, h)
                
                // Glow
                drawCircle(
                    color = accentColor.copy(alpha = 0.3f),
                    radius = 12.dp.toPx(),
                    center = Offset(cx, cy)
                )
                
                // Core
                drawCircle(
                    color = accentColor,
                    radius = 6.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
