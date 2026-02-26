package com.volcagrids.ui.components

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.volcagrids.midi.VolcaParameter
import com.volcagrids.ui.theme.RasterTypography

/**
 * Euclidean S&H (Sample & Hold) - Rhythmic random parameter jumps
 * Uses Euclidean patterns to trigger random value changes
 */

data class EuclideanSHConfig(
    val steps: Int = 16,
    val hits: Int = 5,
    val targetCC: VolcaParameter = VolcaParameter.DRIVE,
    val minValue: Int = 0,
    val maxValue: Int = 127,
    val channel: Int = 0,
    val enabled: Boolean = false
)

@Composable
fun EuclideanSH(
    modifier: Modifier = Modifier,
    config: EuclideanSHConfig = EuclideanSHConfig(),
    currentStep: Int = 0,
    currentValue: Int = 64,
    onConfigChange: (EuclideanSHConfig) -> Unit = {},
    accentColor: Color = Color(0xFF00E5FF) // Teal for Euclidean
) {
    Column(
        modifier = modifier
            .background(Color(0xFF121214))
            .border(1.dp, if (config.enabled) accentColor else Color(0xFF2A2A2E))
            .padding(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EUCLIDEAN_S&H",
                style = RasterTypography.labelSmall,
                color = if (config.enabled) accentColor else Color.Gray
            )
            
            Text(
                text = if (config.enabled) "[ON]" else "[OFF]",
                style = RasterTypography.labelSmall,
                color = if (config.enabled) accentColor else Color.Gray,
                modifier = Modifier.clickable {
                    onConfigChange(config.copy(enabled = !config.enabled))
                }
            )
        }
        
        if (config.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Euclidean pattern visualization
            EuclideanPatternDisplay(
                steps = config.steps,
                hits = config.hits,
                currentStep = currentStep,
                accentColor = accentColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Steps control
                Column {
                    Text("STEPS", style = RasterTypography.labelSmall, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "-",
                            style = RasterTypography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.clickable {
                                onConfigChange(config.copy(steps = (config.steps - 1).coerceAtLeast(1)))
                            }
                        )
                        Text(
                            text = "${config.steps}",
                            style = RasterTypography.labelSmall,
                            color = accentColor
                        )
                        Text(
                            text = "+",
                            style = RasterTypography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.clickable {
                                onConfigChange(config.copy(steps = (config.steps + 1).coerceAtMost(32)))
                            }
                        )
                    }
                }
                
                // Hits control
                Column {
                    Text("HITS", style = RasterTypography.labelSmall, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "-",
                            style = RasterTypography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.clickable {
                                onConfigChange(config.copy(hits = (config.hits - 1).coerceAtLeast(0)))
                            }
                        )
                        Text(
                            text = "${config.hits}",
                            style = RasterTypography.labelSmall,
                            color = accentColor
                        )
                        Text(
                            text = "+",
                            style = RasterTypography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.clickable {
                                onConfigChange(config.copy(hits = (config.hits + 1).coerceAtMost(config.steps)))
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Range controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Min value
                Column(modifier = Modifier.weight(1f)) {
                    Text("MIN", style = RasterTypography.labelSmall, color = Color.Gray)
                    Text(
                        text = "${config.minValue}",
                        style = RasterTypography.labelSmall,
                        color = accentColor
                    )
                }
                
                // Max value
                Column(modifier = Modifier.weight(1f)) {
                    Text("MAX", style = RasterTypography.labelSmall, color = Color.Gray)
                    Text(
                        text = "${config.maxValue}",
                        style = RasterTypography.labelSmall,
                        color = accentColor
                    )
                }
                
                // Current value
                Column(modifier = Modifier.weight(1f)) {
                    Text("VALUE", style = RasterTypography.labelSmall, color = Color.Gray)
                    Text(
                        text = "${currentValue}",
                        style = RasterTypography.labelSmall,
                        color = accentColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Target CC display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TARGET:", style = RasterTypography.labelSmall, color = Color.Gray)
                Text(
                    text = "CC:${config.targetCC.cc}",
                    style = RasterTypography.labelSmall,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun EuclideanPatternDisplay(
    steps: Int,
    hits: Int,
    currentStep: Int,
    accentColor: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF1A1A1E))
    ) {
        val w = size.width
        val h = size.height
        val stepWidth = w / steps
        
        // Calculate Euclidean pattern
        val pattern = calculateEuclidean(steps, hits)
        
        // Draw steps
        for (i in 0 until steps) {
            val x = i * stepWidth
            val isHit = pattern.getOrNull(i) == true
            val isCurrent = i == currentStep
            
            // Background
            drawRect(
                color = Color(0xFF2A2A2E),
                topLeft = Offset(x + 1.dp.toPx(), 2.dp.toPx()),
                size = Size(stepWidth - 2.dp.toPx(), h - 4.dp.toPx())
            )
            
            // Hit indicator
            if (isHit) {
                drawRect(
                    color = if (isCurrent) Color.White else accentColor,
                    topLeft = Offset(x + 1.dp.toPx(), 2.dp.toPx()),
                    size = Size(stepWidth - 2.dp.toPx(), h - 4.dp.toPx())
                )
            }
            
            // Current step highlight
            if (isCurrent) {
                drawRect(
                    color = Color.White.copy(alpha = 0.5f),
                    topLeft = Offset(x, 0f),
                    size = Size(stepWidth, h)
                )
            }
        }
    }
}

/**
 * Calculate Euclidean rhythm pattern
 * Returns list of booleans where true = hit
 */
private fun calculateEuclidean(steps: Int, hits: Int): List<Boolean> {
    if (hits == 0) return List(steps) { false }
    if (hits >= steps) return List(steps) { true }
    
    val pattern = MutableList(steps) { false }
    var positions = mutableListOf(0)
    var remaining = hits - 1
    
    // Simple Euclidean distribution
    val interval = steps.toFloat() / hits
    for (i in 1 until hits) {
        positions.add((i * interval).toInt())
    }
    
    positions.forEach { if (it < steps) pattern[it] = true }
    
    return pattern
}

/**
 * Compact Euclidean S&H for inline use
 */
@Composable
fun EuclideanSHCompact(
    modifier: Modifier = Modifier,
    steps: Int = 16,
    hits: Int = 5,
    currentStep: Int = 0,
    enabled: Boolean = false,
    onToggle: () -> Unit = {},
    accentColor: Color = Color(0xFF00E5FF)
) {
    Row(
        modifier = modifier
            .background(Color(0xFF121214))
            .border(1.dp, if (enabled) accentColor else Color(0xFF2A2A2E))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "EUC",
            style = RasterTypography.labelSmall,
            color = if (enabled) accentColor else Color.Gray,
            modifier = Modifier.clickable { onToggle() }
        )
        
        Text(
            text = "${hits}/${steps}",
            style = RasterTypography.labelSmall,
            color = accentColor
        )
        
        // Mini pattern display
        Canvas(
            modifier = Modifier
                .width(60.dp)
                .height(16.dp)
        ) {
            val w = size.width
            val h = size.height
            val stepWidth = w / steps
            val pattern = calculateEuclidean(steps, hits)
            
            for (i in 0 until steps.coerceAtMost(16)) {
                val x = i * stepWidth
                val isHit = pattern.getOrNull(i) == true
                val isCurrent = i == currentStep
                
                drawRect(
                    color = if (isHit) accentColor else Color(0xFF2A2A2E),
                    topLeft = Offset(x + 1.dp.toPx(), 0f),
                    size = Size(stepWidth - 1.dp.toPx(), h)
                )
                
                if (isCurrent) {
                    drawLine(
                        color = Color.White,
                        start = Offset(x + stepWidth / 2, 0f),
                        end = Offset(x + stepWidth / 2, h),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}
