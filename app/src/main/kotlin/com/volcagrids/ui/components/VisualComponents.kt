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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.volcagrids.engine.EnvelopeShape

// --- CONSTANTS ---
val ColorVoid = Color(0xFF000000)
val ColorData = Color(0xFFFFFFFF)
val ColorGrid = Color(0xFF333333)

val MonoFont = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    letterSpacing = 1.5.sp
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DataBar(
    value: Float,
    label: String,
    flashIntensity: Float, // 0.0 to 1.0 (Velocity mapped)
    onValueChange: (Float) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,  // Callback for long-press (toggle preview)
    onDoubleClick: (() -> Unit)? = null,  // Callback for double-click (open menu)
    showPreview: Boolean = false,  // Show modulation preview overlay
    previewValue: Float = 0.5f,
    previewShape: EnvelopeShape = EnvelopeShape.SMOOTH
) {
    // ALVA NOTO STROBE LOGIC:
    // When flashing, we invert the screen logic.
    // Background flashes White based on intensity.
    // Foreground (Fill) turns Black to maintain contrast.

    // Interpolate Background: Void(Black) -> Data(White)
    val bgArgb = ColorUtils.blendARGB(ColorVoid.toArgb(), ColorData.toArgb(), flashIntensity)
    val bgColor = Color(bgArgb)

    // Interpolate Foreground: Color -> Void(Black)
    // At high intensity, the bar becomes black against the white flash.
    val barArgb = ColorUtils.blendARGB(color.toArgb(), ColorVoid.toArgb(), flashIntensity)
    val barColor = Color(barArgb)

    // Border flashes white on trigger
    val borderArgb = ColorUtils.blendARGB(ColorGrid.toArgb(), ColorData.toArgb(), flashIntensity)
    val borderColor = Color(borderArgb)

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The Raw Data Column - Box contains Canvas + optional Preview overlay
        // Gesture handling is on the Box wrapper
        Box(
            modifier = Modifier
                .weight(1f)
                .width(28.dp)
                .background(bgColor)
                .border(1.dp, borderColor)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val h = size.height
                        val y = change.position.y.coerceIn(0f, h.toFloat())
                        val newValue = 1f - (y / h.toFloat())
                        onValueChange(newValue.coerceIn(0f, 1f))
                    }
                }
                .combinedClickable(
                    onClick = {},
                    onLongClick = { onLongPress?.invoke() },
                    onDoubleClick = { onDoubleClick?.invoke() }
                )
        ) {
            // Canvas for the bar visualization
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fillHeight = size.height * value
                val thresholdY = size.height - fillHeight

                // Raster-Noton Oscilloscope ticks
                val totalTicks = 40
                for (i in 0..totalTicks) {
                    val y = size.height * (i / totalTicks.toFloat())

                    // Thicker lines for major subdivisions
                    val widthScale = if (i % 4 == 0) 1.0f else 0.5f
                    val lineStart = (size.width - (size.width * widthScale)) / 2
                    val lineEnd = size.width - lineStart

                    // If the Y position is "filled" (below the threshold relative from top), color it bright
                    val isActiveTick = y >= thresholdY
                    val tickColor = if (isActiveTick) {
                        if (flashIntensity > 0.5f) ColorVoid else color
                    } else {
                        color.copy(alpha = 0.2f)
                    }

                    drawLine(
                        color = tickColor,
                        start = Offset(lineStart, y),
                        end = Offset(lineEnd, y),
                        strokeWidth = if (i % 4 == 0) 2f else 1f
                    )
                }
            }

            // Show modulation preview overlay if active
            if (showPreview) {
                ModulationPreview(
                    currentValue = previewValue,
                    envelopeShape = previewShape,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                    color = color
                )
            }
        }  // End Box

        Spacer(modifier = Modifier.height(6.dp))

        // Technical Label
        Text(
            text = label.uppercase(),
            style = MonoFont,
            color = if (flashIntensity > 0.5f) ColorData else Color.Gray
        )
    }
}

@Composable
fun SystemButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .background(if (isActive) ColorData else ColorVoid)
            .border(1.dp, if (isActive) ColorVoid else ColorGrid)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "[$label]",
            style = MonoFont,
            color = if (isActive) ColorVoid else ColorData
        )
    }
}
