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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.volcagrids.engine.EnvelopeShape
import com.volcagrids.midi.VolcaParameter
import com.volcagrids.ui.theme.RasterTypography

/**
 * Parameter Sequencer Overlay - Compact, Phone-Optimized
 * All controls visible without scrolling
 */
@Composable
fun ParameterSequencerOverlay(
    isVisible: Boolean = false,
    currentPart: Int = 0,
    currentX: Int = 127,
    currentY: Int = 127,
    currentValue: Int = 64,
    currentRange: Int = 127,
    currentOffset: Int = 0,
    currentShape: EnvelopeShape = EnvelopeShape.SMOOTH,
    currentCC: Int = 51,
    isLinked: Boolean = true,
    engineAX: Int = 0,
    engineAY: Int = 0,
    engineBX: Int = 0,
    engineBY: Int = 0,
    onDismiss: () -> Unit = {},
    onPartChange: (Int) -> Unit = {},
    onCCChange: (Int) -> Unit = {},
    onShapeChange: (EnvelopeShape) -> Unit = {},
    onRangeChange: (Int) -> Unit = {},
    onOffsetChange: (Int) -> Unit = {},
    onPositionChange: (Int, Int) -> Unit = { _, _ -> },
    onLinkToggle: () -> Unit = {},
    onCopyFromA: () -> Unit = {},
    onCopyFromB: () -> Unit = {}
) {
    if (!isVisible) return

    // Local state for position (updates during drag)
    var dragX by remember { mutableStateOf(currentX) }
    var dragY by remember { mutableStateOf(currentY) }
    
    // Update local state when parent changes
    LaunchedEffect(currentX, currentY) {
        dragX = currentX
        dragY = currentY
    }

    val isCompact = LocalConfiguration.current.screenWidthDp < 600

    // Full-screen overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(if (isCompact) 0.98f else 0.5f)
                .heightIn(max = 600.dp)
                .background(Color(0xFF121214))
                .border(1.dp, Color(0xFF333333))
                .clickable { }
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PARAM // P${currentPart + 1}",
                    style = RasterTypography.labelSmall,
                    color = Color(0xFFFF5722)
                )

                Text(
                    text = "[X]",
                    style = RasterTypography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Top row: XY Map + Value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // XY Map
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFF2A2A2E))
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val x = (change.position.x / size.width * 255).toInt().coerceIn(0, 255)
                                val y = (change.position.y / size.height * 255).toInt().coerceIn(0, 255)
                                dragX = x
                                dragY = y
                                onPositionChange(x, y)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw grid
                        for (i in 0..8) {
                            drawLine(
                                color = Color(0xFF2A2A2E),
                                start = Offset((i / 8f) * w, 0f),
                                end = Offset((i / 8f) * w, h),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color(0xFF2A2A2E),
                                start = Offset(0f, (i / 8f) * h),
                                end = Offset(w, (i / 8f) * h),
                                strokeWidth = 1f
                            )
                        }

                        // --- Topographical Engine Data ---
                        val nodeSize = 8f
                        val dashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)

                        // Engine A (RasterSignalA)
                        val aX = (engineAX / 255f) * w
                        val aY = (engineAY / 255f) * h
                        drawRect(
                            color = Color(0xFFEBEBEB).copy(alpha = 0.6f),
                            topLeft = Offset(aX - nodeSize / 2, aY - nodeSize / 2),
                            size = Size(nodeSize, nodeSize),
                            style = Stroke(width = 1f)
                        )
                        drawLine(
                            color = Color(0xFFEBEBEB).copy(alpha = 0.3f),
                            start = Offset(aX, 0f), end = Offset(aX, h),
                            strokeWidth = 1f, pathEffect = dashEffect
                        )
                        drawLine(
                            color = Color(0xFFEBEBEB).copy(alpha = 0.3f),
                            start = Offset(0f, aY), end = Offset(w, aY),
                            strokeWidth = 1f, pathEffect = dashEffect
                        )

                        // Engine B (RasterSignalB)
                        val bX = (engineBX / 255f) * w
                        val bY = (engineBY / 255f) * h
                        drawRect(
                            color = Color(0xFF757575).copy(alpha = 0.6f),
                            topLeft = Offset(bX - nodeSize / 2, bY - nodeSize / 2),
                            size = Size(nodeSize, nodeSize),
                            style = Stroke(width = 1f)
                        )
                        drawLine(
                            color = Color(0xFF757575).copy(alpha = 0.3f),
                            start = Offset(bX, 0f), end = Offset(bX, h),
                            strokeWidth = 1f, pathEffect = dashEffect
                        )
                        drawLine(
                            color = Color(0xFF757575).copy(alpha = 0.3f),
                            start = Offset(0f, bY), end = Offset(w, bY),
                            strokeWidth = 1f, pathEffect = dashEffect
                        )

                        // --- Active Parameter Cursor ---
                        val cursorX = (dragX / 255f) * w
                        val cursorY = (dragY / 255f) * h

                        drawLine(
                            color = Color(0xFFEBEBEB),
                            start = Offset(cursorX, 0f),
                            end = Offset(cursorX, h),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0xFFEBEBEB),
                            start = Offset(0f, cursorY),
                            end = Offset(w, cursorY),
                            strokeWidth = 1f
                        )

                        drawRect(
                            color = Color(0xFFEBEBEB),
                            topLeft = Offset(cursorX - nodeSize / 2, cursorY - nodeSize / 2),
                            size = Size(nodeSize, nodeSize),
                            style = Stroke(width = 1f)
                        )
                    }

                    // Link indicator
                    if (isLinked) {
                        Text(
                            text = "LINKED",
                            style = RasterTypography.labelSmall,
                            color = Color(0xFF00B0FF),
                            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                        )
                    }

                    // Value readout
                    Text(
                        text = "$dragX,$dragY",
                        style = RasterTypography.labelSmall,
                        color = Color(0xFFFF5722),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                    )
                }

                // Value display
                Column(
                    modifier = Modifier
                        .width(70.dp)
                        .fillMaxHeight()
                        .border(1.dp, Color(0xFF2A2A2E)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "VALUE", style = RasterTypography.labelSmall, color = Color.Gray)
                    Text(
                        text = "$currentValue",
                        style = RasterTypography.labelSmall,
                        color = Color(0xFFFF5722),
                        fontSize = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "RANGE", style = RasterTypography.labelSmall, color = Color.Gray)
                    Text(
                        text = "$currentRange",
                        style = RasterTypography.labelSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "OFFSET", style = RasterTypography.labelSmall, color = Color.Gray)
                    Text(
                        text = "$currentOffset",
                        style = RasterTypography.labelSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Parameter selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "CC:${currentCC}", style = RasterTypography.labelSmall, color = Color(0xFFFF5722))
                
                // Link toggle
                Text(
                    text = if (isLinked) "[LINK:ON]" else "[LINK:OFF]",
                    style = RasterTypography.labelSmall,
                    color = if (isLinked) Color(0xFF00B0FF) else Color.Gray,
                    modifier = Modifier.clickable { onLinkToggle() }
                )
            }

            // CC Parameter selector - compact grid
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Show 6 CC options per row
                val ccOptions = listOf(
                    51 to "DRV", 49 to "BIT", 50 to "FLD",  // Row 1
                    117 to "DEC", 118 to "BDY", 119 to "TUN",  // Row 2
                    26 to "PIT", 29 to "MOD", 10 to "PAN",  // Row 3
                    20 to "ATK", 23 to "REL", 17 to "L1"  // Row 4
                )
                
                ccOptions.forEach { (cc, label) ->
                    val isSelected = cc == currentCC
                    Text(
                        text = label,
                        style = RasterTypography.labelSmall,
                        color = if (isSelected) Color(0xFFFF5722) else Color.Gray,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFFF5722) else Color(0xFF333333)
                            )
                            .clickable { onCCChange(cc) }
                            .padding(vertical = 6.dp, horizontal = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Shape selector - compact
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                EnvelopeShape.entries.forEach { shape ->
                    val isSelected = shape == currentShape
                    Text(
                        text = shape.name.take(3),
                        style = RasterTypography.labelSmall,
                        color = if (isSelected) Color(0xFFFF5722) else Color.Gray,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFFF5722) else Color(0xFF333333)
                            )
                            .clickable { onShapeChange(shape) }
                            .padding(vertical = 6.dp, horizontal = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Range slider
            CompactSlider(
                label = "RANGE",
                value = currentRange,
                onValueChange = onRangeChange
            )

            // Offset slider
            CompactSlider(
                label = "OFFSET",
                value = currentOffset,
                onValueChange = onOffsetChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: Part selector + Copy buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Part selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    (0..5).forEach { part ->
                        val isSelected = part == currentPart
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFFF5722) else Color(0xFF333333)
                                )
                                .background(
                                    if (isSelected) Color(0xFFFF5722).copy(alpha = 0.2f) else Color.Transparent
                                )
                                .clickable { onPartChange(part) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${part + 1}",
                                style = RasterTypography.labelSmall,
                                color = if (isSelected) Color(0xFFFF5722) else Color.Gray
                            )
                        }
                    }
                }

                // Copy buttons
                Text(
                    text = "[A]",
                    style = RasterTypography.labelSmall,
                    color = Color(0xFFFF5722),
                    modifier = Modifier
                        .clickable { onCopyFromA() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = "[B]",
                    style = RasterTypography.labelSmall,
                    color = Color(0xFF00B0FF),
                    modifier = Modifier
                        .clickable { onCopyFromB() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = RasterTypography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.width(45.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .border(1.dp, if (isDragging) Color(0xFFFF5722) else Color(0xFF333333))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDrag = { change, _ ->
                            val x = change.position.x / size.width
                            onValueChange((x * 127).toInt().coerceIn(0, 127))
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val fillWidth = (value / 127f) * w

                drawRect(
                    color = Color(0xFF1A1A1E),
                    size = Size(w, h)
                )

                drawRect(
                    color = Color(0xFFFF5722).copy(alpha = 0.5f),
                    size = Size(fillWidth, h)
                )

                drawLine(
                    color = if (isDragging) Color(0xFFFF5722) else Color(0xFF666666),
                    start = Offset(fillWidth, 0f),
                    end = Offset(fillWidth, h),
                    strokeWidth = 2f
                )
            }

            Text(
                text = "$value",
                style = RasterTypography.labelSmall,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
            )
        }
    }
}
