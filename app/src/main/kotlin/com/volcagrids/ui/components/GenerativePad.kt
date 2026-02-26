package com.volcagrids.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.volcagrids.ui.MainViewModel
import kotlin.random.Random

/**
 * Enhanced GenerativePad with Physics and Gesture Recording support
 */
@Composable
fun GenerativePad(
    modifier: Modifier = Modifier,
    color: Color,
    x: Int,
    y: Int,
    viewModel: MainViewModel? = null,
    engineIndex: Int = 0,
    onPositionChange: (Int, Int) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "noise")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val glitchSeed = (time * 10).toInt()
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(ColorVoid)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        // Start gesture recording if enabled
                        if (viewModel?.isRecordingGesture == true) {
                            viewModel.addGesturePoint(x.toFloat(), y.toFloat())
                        }
                        // Apply physics impulse on tap - forward to service (Fix 2.1)
                        if (viewModel?.physicsEnabled == true) {
                            val impulse = 5f
                            if (engineIndex == 0) {
                                viewModel.service?.physicsA?.applyImpulse(impulse, impulse)
                            } else {
                                viewModel.service?.physicsB?.applyImpulse(impulse, impulse)
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        // Stop gesture recording
                        if (viewModel?.isRecordingGesture == true) {
                            viewModel.stopGestureRecording()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val px = (change.position.x / w * 255).toInt().coerceIn(0, 255)
                        val py = (change.position.y / h * 255).toInt().coerceIn(0, 255)

                        onPositionChange(px, py)

                        // Record gesture point
                        if (viewModel?.isRecordingGesture == true) {
                            viewModel.addGesturePoint(px.toFloat(), py.toFloat())
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Apply small physics impulse on tap - forward to service (Fix 2.1)
                        if (viewModel?.physicsEnabled == true) {
                            val impulse = 3f
                            if (engineIndex == 0) {
                                viewModel.service?.physicsA?.applyImpulse(impulse, impulse)
                            } else {
                                viewModel.service?.physicsB?.applyImpulse(impulse, impulse)
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cursorX = (x / 255f) * w
            val cursorY = (y / 255f) * h

            // 1. Data Grid (Dashed)
            val dashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 4f), 0f)
            val gridStepX = w / 8f
            val gridStepY = h / 8f

            for (i in 0..8) {
                drawLine(
                    color = ColorGrid,
                    start = Offset(i * gridStepX, 0f),
                    end = Offset(i * gridStepX, h),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = ColorGrid,
                    start = Offset(0f, i * gridStepY),
                    end = Offset(w, i * gridStepY),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
            }

            // 2. Active Axis Crosshairs
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(cursorX, 0f),
                end = Offset(cursorX, h),
                strokeWidth = 1f
            )
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(0f, cursorY),
                end = Offset(w, cursorY),
                strokeWidth = 1f
            )

            // 3. Coordinate Node (Empty Square Bracket)
            val nodeSize = 6f
            drawRect(
                color = color,
                topLeft = Offset(cursorX - nodeSize / 2, cursorY - nodeSize / 2),
                size = androidx.compose.ui.geometry.Size(nodeSize, nodeSize),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
            )

            // 4. Raw Telemetry Data at Coordinate
            drawContext.canvas.nativeCanvas.drawText(
                "X:${"%.3f".format(x / 255f)} Y:${"%.3f".format(y / 255f)}",
                cursorX + 8f,
                cursorY - 8f,
                android.graphics.Paint().apply {
                    this.color = android.graphics.Color.WHITE
                    this.textSize = 24f
                    this.typeface = android.graphics.Typeface.MONOSPACE
                }
            )

            // 5. Physics Velocity Vector (if active)
            if (viewModel?.physicsEnabled == true && viewModel.service != null) {
                val velocity = if (engineIndex == 0) {
                    viewModel.service!!.physicsA.vx to viewModel.service!!.physicsA.vy
                } else {
                    viewModel.service!!.physicsB.vx to viewModel.service!!.physicsB.vy
                }
                
                val velScale = 15f
                drawLine(
                    color = Color.White,
                    start = Offset(cursorX, cursorY),
                    end = Offset(cursorX + velocity.first * velScale, cursorY + velocity.second * velScale),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
            }
            
            // 6. Gesture Recording Marker
            if (viewModel?.isRecordingGesture == true) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(w - 20f, 10f),
                    size = androidx.compose.ui.geometry.Size(10f, 10f)
                )
            }
        }

        Text(
            text = "COORD_X:$x // COORD_Y:$y",
            style = com.volcagrids.ui.theme.RasterTypography.labelSmall,
            color = color,
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
        )
    }
}
