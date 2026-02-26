package com.volcagrids.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color Palette
val ColorGunmetal = Color(0xFF121214)
val ColorPanel = Color(0xFF1E1E22)
val ColorTeal = Color(0xFF00E5FF)
val ColorViolet = Color(0xFFD500F9)
val ColorGold = Color(0xFFFFD600)
val ColorDarkGray = Color(0xFF2A2A2E)

@Composable
fun VerticalFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fader Track
        Box(
            modifier = Modifier
                .weight(1f)
                .width(40.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val h = size.height
                        // Invert Y because 0 is top in screen coords, but we want 0 at bottom
                        val y = change.position.y.coerceIn(0f, h.toFloat())
                        val newValue = 1f - (y / h)
                        onValueChange(newValue.coerceIn(0f, 1f))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackWidth = 4.dp.toPx()
                val trackX = center.x - (trackWidth / 2)
                
                // Draw Track Groove
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(trackX, 0f),
                    size = Size(trackWidth, size.height),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                
                // Draw Fill Level
                val fillHeight = size.height * value
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.5f))
                    ),
                    topLeft = Offset(trackX, size.height - fillHeight),
                    size = Size(trackWidth, fillHeight),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )

                // Draw Handle (Cap)
                val handleHeight = 24.dp.toPx()
                val handleWidth = 32.dp.toPx()
                val handleY = (size.height * (1f - value)) - (handleHeight / 2)
                
                drawRoundRect(
                    color = Color(0xFFDDDDDD),
                    topLeft = Offset(center.x - (handleWidth / 2), handleY),
                    size = Size(handleWidth, handleHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                
                // Handle Accent Line
                drawLine(
                    color = color,
                    start = Offset(center.x - (handleWidth / 2), handleY + (handleHeight / 2)),
                    end = Offset(center.x + (handleWidth / 2), handleY + (handleHeight / 2)),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TechButton(
    label: String,
    isActive: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) color.copy(alpha = 0.2f) else ColorDarkGray)
            .border(1.dp, if (isActive) color else Color.Gray, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) color else Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun PanelContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(ColorPanel, RoundedCornerShape(8.dp))
            .border(1.dp, ColorDarkGray, RoundedCornerShape(8.dp))
            .padding(12.dp),
        content = content
    )
}
