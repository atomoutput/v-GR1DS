package com.volcagrids.ui.components

import android.media.midi.MidiDeviceInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.volcagrids.ui.MainViewModel
import com.volcagrids.ui.theme.RasterTypography
import com.volcagrids.ui.theme.RasterGrid
import com.volcagrids.ui.theme.RasterDark
import com.volcagrids.ui.theme.RasterSignalA
import com.volcagrids.ui.theme.RasterSignalA
import com.volcagrids.ui.theme.RasterSignalB
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun GeomatSlider(
    value: Float, // 0f to 1f
    onValueChange: (Float) -> Unit,
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val x = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(x)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val x = (change.position.x / size.width).coerceIn(0f, 1f)
                    onValueChange(x)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(color = Color(0xFF1A1A1E), size = Size(w, h))
            drawRect(color = color.copy(alpha = 0.3f), size = Size(w * value, h))
            
            // Draw glowing thumb
            drawRect(
                color = color,
                topLeft = Offset((w * value) - 2.dp.toPx(), 0f),
                size = Size(4.dp.toPx(), h)
            )
        }
        Text(text = label, style = RasterTypography.labelSmall, color = Color.White,
            modifier = Modifier.align(Alignment.Center))
    }
}

/**
 * Compact MIDI Config Overlay - Phone Optimized
 */
@Composable
fun MidiConfigOverlayCompact(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.refreshDevices(context) }

    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    
    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { onClose() }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(if (isCompact) 0.95f else 0.5f)
                    .heightIn(max = 600.dp)
                    .background(Color(0xE6121214)) // Glass reflection
                    .border(1.dp, Color.White.copy(alpha = 0.1f))
                    .clickable { }
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MIDI_HARDWARE_CONFIG",
                        style = RasterTypography.labelSmall,
                        color = Color(0xFF00B0FF)
                    )
                    Text(
                        text = "[X]",
                        style = RasterTypography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.clickable { onClose() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Device List
                Text(text = "AVAILABLE_INTERFACES", style = RasterTypography.labelSmall, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1E))
                        .border(1.dp, Color(0xFF2A2A2E))
                        .verticalScroll(rememberScrollState())
                ) {
                    if (viewModel.midiDevices.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "NO DEVICES FOUND", style = RasterTypography.labelSmall, color = Color.DarkGray)
                        }
                    } else {
                        viewModel.midiDevices.forEach { info ->
                            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "UNKNOWN"
                            val isConnected = info.id == viewModel.midiOutputDeviceId
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isConnected) Color(0xFF00B0FF).copy(alpha=0.15f) else Color.Transparent)
                                    .clickable { viewModel.connectDevice(context, info) }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "> $name ${if (isConnected) " [CONNECTED]" else ""}",
                                    style = RasterTypography.labelSmall,
                                    color = if (isConnected) Color(0xFF00B0FF) else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // External Sync
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "EXTERNAL_CLOCK_SYNC", style = RasterTypography.labelSmall, color = Color.Gray)
                    Box(
                        modifier = Modifier
                            .width(60.dp).height(24.dp)
                            .background(if (viewModel.isExtSync) Color(0xFF00B0FF).copy(alpha = 0.2f) else Color(0xFF1A1A1E))
                            .border(1.dp, if (viewModel.isExtSync) Color(0xFF00B0FF) else Color(0xFF2A2A2E))
                            .clickable { viewModel.toggleExtSync(!viewModel.isExtSync) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (viewModel.isExtSync) "ON" else "OFF", style = RasterTypography.labelSmall, color = if (viewModel.isExtSync) Color.White else Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Generic MIDI Triggers setup component not implemented for glassmorphism
                // Info
                Text(
                    text = "TAP_DEVICE_TO_CONNECT",
                    style = RasterTypography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

/**
 * Compact Settings Overlay - Phone Optimized
 */
@Composable
fun SettingsOverlayCompact(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600

    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { onClose() }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(if (isCompact) 0.95f else 0.5f)
                    .heightIn(max = 600.dp)
                    .background(Color(0xE6121214)) // Glass reflection
                    .border(1.dp, Color.White.copy(alpha = 0.1f))
                    .clickable { }
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SETTINGS",
                        style = RasterTypography.labelSmall,
                        color = Color(0xFFFF5722)
                    )
                    Text(
                        text = "[X]",
                        style = RasterTypography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.clickable { onClose() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // BPM Slider
                Text(text = "TEMPO_BPM", style = RasterTypography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                GeomatSlider(
                    value = (viewModel.bpm - 30.0).toFloat() / 270f,
                    onValueChange = { percent ->
                        viewModel.setBPM(30.0 + (percent * 270.0).toInt())
                    },
                    color = Color(0xFFFF5722),
                    label = "${viewModel.bpm.toInt()} BPM"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Swing Slider
                Text(text = "SWING_AMT", style = RasterTypography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                GeomatSlider(
                    value = viewModel.swing / 100f,
                    onValueChange = { percent ->
                        val sv = (percent * 100).toInt()
                        viewModel.swing = sv - (sv % 5) // Snap to 5% increments
                    },
                    color = Color(0xFFFF5722),
                    label = "${viewModel.swing}%"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Clock Resolution
                Text(text = "CLOCK_RESOLUTION", style = RasterTypography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(30.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(0 to "4_PPQN", 1 to "8_PPQN", 2 to "24_PPQN").forEach { (res, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f).fillMaxHeight()
                                .background(if (viewModel.clockResolution == res) Color(0xFFFF5722).copy(alpha = 0.2f) else Color(0xFF1A1A1E))
                                .border(1.dp, if (viewModel.clockResolution == res) Color(0xFFFF5722) else Color(0xFF2A2A2E))
                                .clickable { viewModel.clockResolution = res },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = RasterTypography.labelSmall, color = if (viewModel.clockResolution == res) Color.White else Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Link Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "LINK_MODE (A=B)", style = RasterTypography.labelSmall, color = Color.Gray)
                    Box(
                        modifier = Modifier
                            .width(60.dp).height(24.dp)
                            .background(if (viewModel.linkMode) Color(0xFFFF5722).copy(alpha = 0.2f) else Color(0xFF1A1A1E))
                            .border(1.dp, if (viewModel.linkMode) Color(0xFFFF5722) else Color(0xFF2A2A2E))
                            .clickable { viewModel.toggleLinkMode() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (viewModel.linkMode) "ON" else "OFF", style = RasterTypography.labelSmall, color = if (viewModel.linkMode) Color.White else Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info
                Text(
                    text = "VOLCA_DRUM // SPLIT_CH",
                    style = RasterTypography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
