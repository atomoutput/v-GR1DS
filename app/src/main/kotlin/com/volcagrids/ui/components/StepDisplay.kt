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
import androidx.compose.ui.unit.dp
import com.volcagrids.ui.theme.RasterTypography

/**
 * Step Display - Shows current step and trigger activity
 * 32-LED horizontal bar with per-part indicators
 */

@Composable
fun StepDisplay(
    modifier: Modifier = Modifier,
    currentStep: Int,
    patternLength: Int = 32,
    triggers: List<Boolean> = listOf(false, false, false, false, false, false),
    accentColor: Color = Color(0xFFFFFFFF)
) {
    Column(
        modifier = modifier
            .background(Color(0xFF121214))
            .padding(4.dp)
    ) {
        // Step bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            val w = size.width
            val h = size.height
            val stepWidth = w / patternLength
            
            // Draw all steps
            for (i in 0 until patternLength) {
                val x = i * stepWidth
                val isCurrent = i == currentStep
                val isBeat = i % 8 == 0
                
                // Background
                drawRect(
                    color = Color(0xFF1A1A1E),
                    topLeft = Offset(x + 1.dp.toPx(), 2.dp.toPx()),
                    size = Size(stepWidth - 2.dp.toPx(), h - 4.dp.toPx())
                )
                
                // Current step highlight
                if (isCurrent) {
                    drawRect(
                        color = accentColor,
                        topLeft = Offset(x + 1.dp.toPx(), 2.dp.toPx()),
                        size = Size(stepWidth - 2.dp.toPx(), h - 4.dp.toPx())
                    )
                } else if (isBeat) {
                    // Beat markers
                    drawRect(
                        color = Color(0xFF333333),
                        topLeft = Offset(x + 1.dp.toPx(), 2.dp.toPx()),
                        size = Size(stepWidth - 2.dp.toPx(), h - 4.dp.toPx())
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Trigger indicators for 6 parts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            triggers.forEachIndexed { index, isActive ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (isActive) Color(0xFFFF5722) else Color(0xFF2A2A2E),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        
        // Step number
        Text(
            text = "STEP:${currentStep + 1}/$patternLength",
            style = RasterTypography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Pattern Length Selector
 */
@Composable
fun PatternLengthSelector(
    modifier: Modifier = Modifier,
    currentLength: Int,
    onLengthChange: (Int) -> Unit,
    accentColor: Color = Color(0xFFFF5722)
) {
    Row(
        modifier = modifier
            .background(Color(0xFF121214))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(8, 16, 32, 64).forEach { length ->
            Text(
                text = "$length",
                style = RasterTypography.labelSmall,
                color = if (length == currentLength) accentColor else Color.Gray,
                modifier = Modifier
                    .border(
                        1.dp,
                        if (length == currentLength) accentColor else Color(0xFF333333)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { onLengthChange(length) }
            )
        }
    }
}
