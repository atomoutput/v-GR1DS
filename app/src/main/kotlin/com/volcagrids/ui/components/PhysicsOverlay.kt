package com.volcagrids.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
 * Physics Overlay - Visual feedback and controls for physics engine
 * Shows trajectory preview, velocity vectors, and physics parameters
 */
@Composable
fun PhysicsOverlay(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = false,
    x: Float = 127f,
    y: Float = 127f,
    vx: Float = 0f,
    vy: Float = 0f,
    friction: Float = 0.98f,
    elasticity: Float = 0.9f,
    onToggle: () -> Unit = {},
    onFrictionChange: (Float) -> Unit = {},
    onElasticityChange: (Float) -> Unit = {},
    accentColor: Color = Color(0xFFFFD600) // Gold for physics
) {
    Column(
        modifier = modifier
            .background(Color(0xFF121214))
            .border(1.dp, if (isEnabled) accentColor else Color(0xFF2A2A2E))
            .padding(8.dp)
    ) {
        // Header with toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PHYSICS_ENGINE",
                style = RasterTypography.labelSmall,
                color = if (isEnabled) accentColor else Color.Gray
            )
            
            Text(
                text = if (isEnabled) "[ON]" else "[OFF]",
                style = RasterTypography.labelSmall,
                color = if (isEnabled) accentColor else Color.Gray,
                modifier = Modifier.clickable { onToggle() }
            )
        }
        
        if (isEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Velocity visualization
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFF1A1A1E))
            ) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2, h / 2)
                
                // Draw velocity vector
                val vectorScale = 30f
                val endX = center.x + vx * vectorScale
                val endY = center.y + vy * vectorScale
                
                drawLine(
                    color = accentColor,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                
                // Arrow head
                val arrowSize = 6.dp.toPx()
                val angle = Math.atan2((endY - center.y).toDouble(), (endX - center.x).toDouble())
                drawLine(
                    color = accentColor,
                    start = Offset(endX, endY),
                    end = Offset(
                        (endX - arrowSize * Math.cos(angle - Math.PI / 6)).toFloat(),
                        (endY - arrowSize * Math.sin(angle - Math.PI / 6)).toFloat()
                    ),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = accentColor,
                    start = Offset(endX, endY),
                    end = Offset(
                        (endX - arrowSize * Math.cos(angle + Math.PI / 6)).toFloat(),
                        (endY - arrowSize * Math.sin(angle + Math.PI / 6)).toFloat()
                    ),
                    strokeWidth = 2.dp.toPx()
                )
                
                // Center point
                drawCircle(
                    color = accentColor.copy(alpha = 0.3f),
                    radius = 4.dp.toPx(),
                    center = center
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Velocity readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "VX:${String.format("%+.2f", vx)}",
                    style = RasterTypography.labelSmall,
                    color = accentColor
                )
                Text(
                    text = "VY:${String.format("%+.2f", vy)}",
                    style = RasterTypography.labelSmall,
                    color = accentColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Friction control
            MiniSlider(
                label = "FRICTION",
                value = friction,
                onValueChange = onFrictionChange,
                accentColor = accentColor
            )
            
            // Elasticity control
            MiniSlider(
                label = "ELASTIC",
                value = elasticity,
                onValueChange = onElasticityChange,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun MiniSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = RasterTypography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.width(50.dp)
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, if (isDragging) accentColor else Color(0xFF333333))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            val delta = dragAmount.x / size.width
                            onValueChange((value + delta).coerceIn(0f, 1f))
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val fillWidth = w * value
                
                drawRect(
                    color = Color(0xFF1A1A1E),
                    size = Size(w, h)
                )
                
                drawRect(
                    color = accentColor.copy(alpha = 0.5f),
                    size = Size(fillWidth, h)
                )
                
                drawLine(
                    color = if (isDragging) accentColor else Color(0xFF666666),
                    start = Offset(fillWidth, 0f),
                    end = Offset(fillWidth, h),
                    strokeWidth = 2.dp.toPx()
                )
            }
            
            Text(
                text = String.format("%.2f", value),
                style = RasterTypography.labelSmall,
                color = accentColor,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
            )
        }
    }
}

/**
 * Trajectory Preview - Shows predicted path for physics cursor
 */
@Composable
fun TrajectoryPreview(
    modifier: Modifier = Modifier,
    x: Float,
    y: Float,
    vx: Float,
    vy: Float,
    friction: Float,
    elasticity: Float,
    bounds: Size,
    accentColor: Color = Color(0xFFFFD600)
) {
    Canvas(modifier = modifier) {
        val path = androidx.compose.ui.graphics.Path()
        
        var simX = x
        var simY = y
        var simVx = vx
        var simVy = vy
        
        path.moveTo(simX, simY)
        
        // Simulate 30 steps ahead
        repeat(30) {
            simX += simVx
            simY += simVy
            simVx *= friction
            simVy *= friction
            
            // Bounce
            if (simX < 0 || simX > bounds.width) {
                simVx = -simVx * elasticity
                simX = simX.coerceIn(0f, bounds.width)
            }
            if (simY < 0 || simY > bounds.height) {
                simVy = -simVy * elasticity
                simY = simY.coerceIn(0f, bounds.height)
            }
            
            path.lineTo(simX, simY)
        }
        
        // Draw dashed trajectory line
        drawPath(
            path = path,
            color = accentColor.copy(alpha = 0.5f),
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
        )
    }
}
