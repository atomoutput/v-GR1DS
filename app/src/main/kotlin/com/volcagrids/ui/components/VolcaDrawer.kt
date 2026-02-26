package com.volcagrids.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.volcagrids.ui.theme.RasterTypography

/**
 * Compact Volca Drawer - Phone Optimized
 */
@Composable
fun VolcaDrawer(
    modifier: Modifier = Modifier,
    isVisible: Boolean = false,
    onDismiss: () -> Unit = {},
    waveguideDecay: Int = 64,
    waveguideBody: Int = 64,
    waveguideTune: Int = 64,
    onWaveguideChange: (Int, Int, Int) -> Unit = { _, _, _ -> },
    morphValues: List<Int> = listOf(64, 64, 64, 64, 64, 64),
    onMorphChange: (Int, Int) -> Unit = { _, _ -> },
    accentColor: Color = Color(0xFFFF5722)
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(if (isCompact) 1f else 0.6f)
                    .background(Color(0xFF121214))
                    .border(1.dp, Color(0xFF333333))
                    .clickable { }
                    .padding(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "VOLCA",
                        style = RasterTypography.labelSmall,
                        color = accentColor
                    )
                    Text(
                        text = "[X]",
                        style = RasterTypography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Waveguide - Compact
                Text(text = "WAVEGUIDE", style = RasterTypography.labelSmall, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(4.dp))

                WaveguidePadCompact(
                    decay = waveguideDecay,
                    body = waveguideBody,
                    onValueChange = { d, b -> onWaveguideChange(d, b, waveguideTune) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Morph Knobs - Single row
                Text(text = "MORPH", style = RasterTypography.labelSmall, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    morphValues.forEachIndexed { index, value ->
                        MiniMorphKnob(
                            partIndex = index,
                            value = value,
                            onValueChange = onMorphChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Info
                Text(
                    text = "DRAG TO ADJUST",
                    style = RasterTypography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

/**
 * Compact Volca Controls for inline use
 */
@Composable
fun VolcaControlsCompact(
    modifier: Modifier = Modifier,
    waveguideDecay: Int = 64,
    waveguideBody: Int = 64,
    onWaveguideChange: (Int, Int) -> Unit = { _, _ -> },
    morphValues: List<Int> = listOf(64, 64, 64, 64, 64, 64),
    onMorphChange: (Int, Int) -> Unit = { _, _ -> }
) {
    Column(
        modifier = modifier
            .background(Color(0xFF121214))
            .padding(8.dp)
    ) {
        // Compact waveguide
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "WAVEGUIDE",
                style = RasterTypography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            
            WaveguidePadCompact(
                decay = waveguideDecay,
                body = waveguideBody,
                onValueChange = onWaveguideChange,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Compact morph
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "MORPH",
                style = RasterTypography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            
            morphValues.forEachIndexed { index, value ->
                MiniMorphKnob(
                    partIndex = index,
                    value = value,
                    onValueChange = onMorphChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniMorphKnob(
    partIndex: Int,
    value: Int,
    onValueChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, Color(0xFF2A2A2E))
            .padding(2.dp)
            .pointerInput(Unit) {
                var accumulatedDrag = 0f
                detectDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount.y
                        val delta = (-accumulatedDrag / 3).toInt()
                        val newValue = (value + delta).coerceIn(0, 127)
                        onValueChange(partIndex, newValue)
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${partIndex + 1}",
            style = RasterTypography.labelSmall,
            color = Color.White,
            fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Vertical slider track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFF1A1A1E))
        ) {
            // Fill level from bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(((value / 127f) * 50).dp)
                    .background(Color(0xFF00B0FF))
            )
            
            // Top cap
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFF00B0FF))
            )
            
            // Value text overlay
            Text(
                text = "$value",
                style = RasterTypography.labelSmall,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
