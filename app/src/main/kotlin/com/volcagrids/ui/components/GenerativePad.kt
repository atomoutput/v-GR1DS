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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.volcagrids.engine.Resources
import com.volcagrids.ui.MainViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Enhanced GenerativePad with Terrain Visualization
 * Shows topographic elevation contours, activity heatmaps, and terrain response
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
    // Simple animation phase - no trigger feedback to avoid crashes
    val infiniteTransition = rememberInfiniteTransition(label = "terrain")
    
    // Animated terrain pulse
    val terrainPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "terrainPhase"
    )

    var isDragging by remember { mutableStateOf(false) }
    
    // Calculate terrain data at current position
    val terrainData = remember(x, y) {
        calculateTerrainData(x, y)
    }

    Box(
        modifier = modifier
            .background(ColorVoid)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        if (viewModel?.isRecordingGesture == true) {
                            viewModel.addGesturePoint(x.toFloat(), y.toFloat())
                        }
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

                        if (viewModel?.isRecordingGesture == true) {
                            viewModel.addGesturePoint(px.toFloat(), py.toFloat())
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
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

            // === TERRAIN VISUALIZATION LAYERS ===
            
            // 1. Morphing Voronoi Map (Raster-Noton style)
            drawMorphingVoronoi(w, h, x, y, terrainPhase, color)
            
            // 2. Terrain Elevation Contours (based on nodeTable data)
            drawTerrainContours(w, h, x, y, terrainPhase, color)
            
            // 2. Activity Heatmap (static positions, no trigger feedback to avoid crashes)
            drawSimpleActivityHeatmap(w, h, color)
            
            // 3. Terrain Gradient (elevation-based coloring)
            drawTerrainGradient(w, h, x, y, terrainData)

            // 4. Data Grid (Dashed) - subtle background
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f), 0f)
            val gridStepX = w / 8f
            val gridStepY = h / 8f

            for (i in 0..8) {
                drawLine(
                    color = ColorGrid.copy(alpha = 0.3f),
                    start = Offset(i * gridStepX, 0f),
                    end = Offset(i * gridStepX, h),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = ColorGrid.copy(alpha = 0.3f),
                    start = Offset(0f, i * gridStepY),
                    end = Offset(w, i * gridStepY),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
            }

            // 5. Active Axis Crosshairs (brighter when dragging)
            val crosshairAlpha = if (isDragging) 0.8f else 0.5f
            drawLine(
                color = color.copy(alpha = crosshairAlpha),
                start = Offset(cursorX, 0f),
                end = Offset(cursorX, h),
                strokeWidth = if (isDragging) 2f else 1f
            )
            drawLine(
                color = color.copy(alpha = crosshairAlpha),
                start = Offset(0f, cursorY),
                end = Offset(w, cursorY),
                strokeWidth = if (isDragging) 2f else 1f
            )

            // 6. Terrain Node Indicator (shows current elevation)
            drawTerrainNode(cursorX, cursorY, terrainData, color, isDragging)

            // 7. Elevation Ring (pulsing based on terrain height)
            drawElevationRing(cursorX, cursorY, terrainData.elevation, terrainPhase, color)

            // 8. Physics Velocity Vector (if active)
            if (viewModel?.physicsEnabled == true && viewModel.service != null) {
                val velocity = if (engineIndex == 0) {
                    viewModel.service!!.physicsA.vx to viewModel.service!!.physicsA.vy
                } else {
                    viewModel.service!!.physicsB.vx to viewModel.service!!.physicsB.vy
                }

                val velScale = 15f
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(cursorX, cursorY),
                    end = Offset(cursorX + velocity.first * velScale, cursorY + velocity.second * velScale),
                    strokeWidth = 2f,
                    pathEffect = dashEffect
                )
            }

            // 9. Gesture Recording Marker
            if (viewModel?.isRecordingGesture == true) {
                drawRect(
                    color = Color(0xFFFF5722),
                    topLeft = Offset(w - 20f, 10f),
                    size = Size(10f, 10f),
                    style = Stroke(width = 2f)
                )
            }
            
            // 10. Terrain Data Readout
            drawTerrainReadout(cursorX, cursorY, terrainData, w, h)
        }

        Text(
            text = "ELEV:${terrainData.elevation} // GRADE:${terrainData.gradient}°",
            style = com.volcagrids.ui.theme.RasterTypography.labelSmall,
            color = color,
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
        )
    }
}

/**
 * Terrain data structure
 */
data class TerrainData(
    val elevation: Int,      // 0-255 elevation value
    val gradient: Float,     // Slope angle in degrees
    val aspect: Float,       // Direction of steepest descent
    val curvature: Float     // Convex/concave (-1 to 1)
)

/**
 * Calculate terrain data at given coordinates
 */
private fun calculateTerrainData(x: Int, y: Int): TerrainData {
    val i = (x shr 6).coerceIn(0, 3)
    val j = (y shr 6).coerceIn(0, 3)
    val xi = (x shl 2) and 0xff
    val yi = (y shl 2) and 0xff
    
    // Get elevation from node table (using BD channel as base)
    val drumMap = arrayOf(
        intArrayOf(10, 8, 0, 9, 11),
        intArrayOf(15, 7, 13, 12, 6),
        intArrayOf(18, 14, 4, 5, 3),
        intArrayOf(23, 16, 21, 1, 2),
        intArrayOf(24, 19, 17, 20, 22)
    )
    
    val aMap = Resources.nodeTable[drumMap[i][j]]
    val bMap = Resources.nodeTable[drumMap[i + 1][j]]
    val cMap = Resources.nodeTable[drumMap[i][j + 1]]
    val dMap = Resources.nodeTable[drumMap[i + 1][j + 1]]
    
    val offset = 0.coerceIn(0, 95)
    val a = aMap[offset].toInt()
    val b = bMap[offset].toInt()
    val c = cMap[offset].toInt()
    val d = dMap[offset].toInt()
    
    val elevation = u8Mix(u8Mix(a, b, xi), u8Mix(c, d, xi), yi)
    
    // Calculate gradient (rate of change)
    val dx = b - a
    val dy = c - a
    val gradient = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    
    // Calculate aspect (direction)
    val aspect = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
    
    // Calculate curvature (convexity)
    val center = elevation
    val surrounding = (a + b + c + d) / 4
    val curvature = (center - surrounding) / 127.5f
    
    return TerrainData(
        elevation = elevation,
        gradient = (gradient * 0.5f).coerceIn(0f, 90f),
        aspect = aspect,
        curvature = curvature.coerceIn(-1f, 1f)
    )
}

/**
 * Draw morphing Voronoi diagram in Raster-Noton style
 * Creates a bending, organic cell structure that follows cursor
 */
private fun DrawScope.drawMorphingVoronoi(
    w: Float,
    h: Float,
    x: Int,
    y: Int,
    phase: Float,
    color: Color
) {
    // Generate seed points based on terrain position
    // Seeds morph and bend based on cursor position
    val numSeeds = 12
    val seeds = mutableListOf<VoronoiSeed>()
    
    // Create a grid of seeds that morph with cursor position
    for (i in 0 until numSeeds) {
        val baseAngle = (i / numSeeds.toFloat()) * 2 * Math.PI.toFloat()
        val baseRadius = (w * 0.3f) + (i % 3) * (w * 0.05f)
        
        // Morph seeds based on cursor position
        val cursorInfluenceX = (x / 255f - 0.5f) * w * 0.3f
        val cursorInfluenceY = (y / 255f - 0.5f) * h * 0.3f
        
        // Add phase-based animation
        val phaseOffset = sin(phase + i * 0.5f) * 20f
        
        val seedX = w / 2 + baseRadius * cos(baseAngle) + cursorInfluenceX + phaseOffset
        val seedY = h / 2 + baseRadius * sin(baseAngle) + cursorInfluenceY + phaseOffset
        
        seeds.add(VoronoiSeed(seedX, seedY, i % 3))
    }
    
    // Add cursor as a special seed point
    seeds.add(VoronoiSeed((x / 255f) * w, (y / 255f) * h, 3))
    
    // Draw Voronoi cells using distance field approach
    val cellSize = 25f  // Larger cells for better performance
    for (py in 0 until (h / cellSize).toInt()) {
        for (px in 0 until (w / cellSize).toInt()) {
            val pointX = px * cellSize + cellSize / 2
            val pointY = py * cellSize + cellSize / 2
            
            // Find closest seed
            var minDist = Float.MAX_VALUE
            var closestSeed = -1
            
            for ((i, seed) in seeds.withIndex()) {
                val dx = pointX - seed.x
                val dy = pointY - seed.y
                val dist = dx * dx + dy * dy
                
                // Add cursor influence - bends cells toward cursor
                val cursorDx = pointX - (x / 255f) * w
                val cursorDy = pointY - (y / 255f) * h
                val cursorDist = sqrt(cursorDx * cursorDx + cursorDy * cursorDy)
                val bendFactor = 1f - (cursorDist / (w * 0.5f)).coerceIn(0f, 1f)
                val modifiedDist = dist * (1f - bendFactor * 0.3f)
                
                if (modifiedDist < minDist) {
                    minDist = modifiedDist
                    closestSeed = i
                }
            }
            
            // Draw cell border if this is an edge pixel
            if (closestSeed >= 0) {
                // Check neighboring cells for boundaries
                val neighbors = listOf(
                    px + 1 to py,
                    px to py + 1,
                    px - 1 to py,
                    px to py - 1
                )
                
                var isEdge = false
                for ((nx, ny) in neighbors) {
                    if (nx in 0 until (w / cellSize).toInt() && 
                        ny in 0 until (h / cellSize).toInt()) {
                        val neighborPointX = nx * cellSize + cellSize / 2
                        val neighborPointY = ny * cellSize + cellSize / 2
                        
                        var neighborMinDist = Float.MAX_VALUE
                        var neighborClosest = -1
                        
                        for ((i, seed) in seeds.withIndex()) {
                            val dx = neighborPointX - seed.x
                            val dy = neighborPointY - seed.y
                            val dist = dx * dx + dy * dy
                            
                            val cursorDx = neighborPointX - (x / 255f) * w
                            val cursorDy = neighborPointY - (y / 255f) * h
                            val cursorDist = sqrt(cursorDx * cursorDx + cursorDy * cursorDy)
                            val bendFactor = 1f - (cursorDist / (w * 0.5f)).coerceIn(0f, 1f)
                            val modifiedDist = dist * (1f - bendFactor * 0.3f)
                            
                            if (modifiedDist < neighborMinDist) {
                                neighborMinDist = modifiedDist
                                neighborClosest = i
                            }
                        }
                        
                        if (neighborClosest != closestSeed) {
                            isEdge = true
                            break
                        }
                    }
                }
                
                if (isEdge) {
                    val seed = seeds.getOrNull(closestSeed)
                    val seedType = seed?.type ?: 0
                    
                    // Base alpha by seed type
                    val alpha = when (seedType) {
                        3 -> 0.8f  // Cursor cell - brightest
                        0 -> 0.4f
                        1 -> 0.3f
                        2 -> 0.2f
                        else -> 0.3f
                    }
                    
                    drawRect(
                        color = color.copy(alpha = alpha),
                        topLeft = Offset(pointX - cellSize / 2, pointY - cellSize / 2),
                        size = Size(cellSize, cellSize),
                        style = Stroke(width = 1f)
                    )
                }
            }
        }
    }
    
    // Draw seed points as technical markers
    for ((i, seed) in seeds.withIndex()) {
        val markerSize = if (seed.type == 3) 8f else 4f
        val markerAlpha = if (seed.type == 3) 1.0f else 0.5f
        
        // Draw crosshair marker
        drawLine(
            color = color.copy(alpha = markerAlpha),
            start = Offset(seed.x - markerSize, seed.y),
            end = Offset(seed.x + markerSize, seed.y),
            strokeWidth = 1f
        )
        drawLine(
            color = color.copy(alpha = markerAlpha),
            start = Offset(seed.x, seed.y - markerSize),
            end = Offset(seed.x, seed.y + markerSize),
            strokeWidth = 1f
        )
        
        // Draw seed number (technical label)
        if (seed.type < 3) {
            drawContext.canvas.nativeCanvas.drawText(
                "${i + 1}",
                seed.x + 6f,
                seed.y + 4f,
                android.graphics.Paint().apply {
                    this.color = android.graphics.Color.WHITE
                    this.textSize = 14f
                    this.typeface = android.graphics.Typeface.MONOSPACE
                    this.alpha = 150
                }
            )
        }
    }
}

/**
 * Voronoi seed point with morphing behavior
 */
private data class VoronoiSeed(
    val x: Float,
    val y: Float,
    val type: Int  // 0-2: regular seeds, 3: cursor
)

/**
 * Draw terrain elevation contours
 */
private fun DrawScope.drawTerrainContours(
    w: Float,
    h: Float,
    x: Int,
    y: Int,
    phase: Float,
    color: Color
) {
    val numContours = 8
    for (level in 0 until numContours) {
        val targetElevation = (level * 32) % 255
        val distance = kotlin.math.abs(targetElevation - ((x + y) / 2))
        val alpha = (1f - distance / 128f).coerceIn(0f, 0.3f)
        
        if (alpha > 0.05f) {
            val offset = (phase * 20f) % w
            drawLine(
                color = color.copy(alpha = alpha * 0.5f),
                start = Offset(offset, 0f),
                end = Offset(offset + w, h),
                strokeWidth = 1f
            )
        }
    }
}

/**
 * Draw simple static activity heatmap (no trigger feedback to avoid crashes)
 */
private fun DrawScope.drawSimpleActivityHeatmap(
    w: Float,
    h: Float,
    color: Color
) {
    // Static positions for BD, SD, HH
    val positions = listOf(
        Offset(w * 0.2f, h * 0.3f),  // BD
        Offset(w * 0.5f, h * 0.5f),  // SD
        Offset(w * 0.8f, h * 0.3f)   // HH
    )
    
    val labels = listOf("BD", "SD", "HH")
    
    for (i in 0..2) {
        val pos = positions[i]
        // Subtle static indicator
        drawCircle(
            color = color.copy(alpha = 0.15f),
            radius = 20f,
            center = pos,
            style = Stroke(width = 1f)
        )
        
        // Label
        drawContext.canvas.nativeCanvas.drawText(
            labels[i],
            pos.x - 10f,
            pos.y + 5f,
            android.graphics.Paint().apply {
                this.color = android.graphics.Color.WHITE
                this.textSize = 14f
                this.typeface = android.graphics.Typeface.MONOSPACE
                this.alpha = 100
            }
        )
    }
}

/**
 * Draw terrain gradient visualization
 */
private fun DrawScope.drawTerrainGradient(
    w: Float,
    h: Float,
    x: Int,
    y: Int,
    terrainData: TerrainData
) {
    // Subtle gradient based on elevation
    val elevationFactor = terrainData.elevation / 255f
    
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1a1a2e).copy(alpha = 0.3f),
                Color(0xFF16213e).copy(alpha = 0.3f * elevationFactor)
            )
        )
    )
}

/**
 * Draw terrain node indicator
 */
private fun DrawScope.drawTerrainNode(
    cursorX: Float,
    cursorY: Float,
    terrainData: TerrainData,
    color: Color,
    isDragging: Boolean
) {
    val nodeSize = if (isDragging) 10f else 6f
    
    // Outer glow based on elevation
    val glowAlpha = terrainData.elevation / 510f
    drawCircle(
        color = color.copy(alpha = glowAlpha),
        radius = nodeSize * 2,
        center = Offset(cursorX, cursorY),
        style = Stroke(width = 1f)
    )
    
    // Inner node
    drawRect(
        color = color,
        topLeft = Offset(cursorX - nodeSize / 2, cursorY - nodeSize / 2),
        size = Size(nodeSize, nodeSize),
        style = Stroke(width = if (isDragging) 2f else 1f)
    )
    
    // Elevation indicator (filled based on height)
    val fillHeight = nodeSize * (terrainData.elevation / 255f)
    drawRect(
        color = color.copy(alpha = 0.5f),
        topLeft = Offset(cursorX - nodeSize / 2 + 1f, cursorY + nodeSize / 2 - fillHeight),
        size = Size(nodeSize - 2, fillHeight)
    )
}

/**
 * Draw pulsing elevation ring
 */
private fun DrawScope.drawElevationRing(
    cursorX: Float,
    cursorY: Float,
    elevation: Int,
    phase: Float,
    color: Color
) {
    val baseRadius = 20f + (elevation / 255f) * 30f
    val pulseRadius = baseRadius + sin(phase) * 5f
    
    drawCircle(
        color = color.copy(alpha = 0.2f),
        radius = pulseRadius,
        center = Offset(cursorX, cursorY),
        style = Stroke(width = 1f)
    )
}

/**
 * Draw terrain data readout
 */
private fun DrawScope.drawTerrainReadout(
    cursorX: Float,
    cursorY: Float,
    terrainData: TerrainData,
    w: Float,
    h: Float
) {
    val paint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.WHITE
        this.textSize = 20f
        this.typeface = android.graphics.Typeface.MONOSPACE
        this.alpha = 180
    }
    
    drawContext.canvas.nativeCanvas.drawText(
        "ELEV:${terrainData.elevation}",
        cursorX + 10f,
        cursorY + 20f,
        paint
    )
    
    paint.alpha = 150
    drawContext.canvas.nativeCanvas.drawText(
        "GRAD:${terrainData.gradient.toInt()}°",
        cursorX + 10f,
        cursorY + 40f,
        paint
    )
}

/**
 * Bilinear interpolation helper
 */
private fun u8Mix(a: Int, b: Int, mix: Int): Int {
    return (a * (255 - mix) + b * mix) shr 8
}
