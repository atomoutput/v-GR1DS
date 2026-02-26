package com.volcagrids.ui.components

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.volcagrids.ui.MainViewModel
import com.volcagrids.ui.theme.RasterTypography

/**
 * AdvancedOverlays.kt
 * Container Dialogs to inject the 7 orphaned advanced mathematical/generative UI features.
 */

@Composable
fun ModMatrixOverlay(
    viewModel: MainViewModel,
    isVisible: Boolean,
    onClose: () -> Unit
) {
    if (!isVisible) return
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
                    .fillMaxWidth(if (isCompact) 0.95f else 0.7f)
                    .fillMaxHeight(0.8f) // Scrollable modal
                    .background(Color(0xE6121214)) // Glassmorphism reflection
                    .border(1.dp, Color.White.copy(alpha = 0.1f))
                    .clickable { /* Block touches */ }
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MODULATION_MATRIX",
                        style = RasterTypography.titleMedium,
                        color = Color(0xFFFFD600) // Gold
                    )
                    Text(
                        text = "[X]",
                        style = RasterTypography.titleMedium,
                        color = Color.Gray,
                        modifier = Modifier.clickable { onClose() }
                    )
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LFOMatrix(
                        modifier = Modifier.fillMaxWidth(),
                        lfos = viewModel.lfoConfigs,
                        onLFOChange = { lfo -> viewModel.updateLFO(lfo) }
                    )
                    
                    EuclideanSH(
                        modifier = Modifier.fillMaxWidth(),
                        // Will bind later if more configuration is needed for EuclideanSH
                    )

                    CCMatrix(
                        modifier = Modifier.fillMaxWidth(),
                        slots = viewModel.modulationSlots,
                        onSlotChange = { slot -> viewModel.updateModulationSlot(slot) }
                    )
                }
            }
        }
    }
}

@Composable
fun PerformanceOverlay(
    viewModel: MainViewModel,
    isVisible: Boolean,
    onClose: () -> Unit
) {
    if (!isVisible) return
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
                    .fillMaxWidth(if (isCompact) 0.95f else 0.7f)
                    .fillMaxHeight(0.8f)
                    .background(Color(0xE6121214)) // Glassmorphism reflection
                    .border(1.dp, Color.White.copy(alpha = 0.1f))
                    .clickable { }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "PERFORMANCE_CONTROLS", style = RasterTypography.titleMedium, color = Color(0xFF00E5FF))
                    Text(text = "[X]", style = RasterTypography.titleMedium, color = Color.Gray, modifier = Modifier.clickable { onClose() })
                }

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PhysicsOverlay(
                        modifier = Modifier.fillMaxWidth().height(250.dp)
                    )
                    
                    GestureRecorder(
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        currentGesture = null
                    )
                }
            }
        }
    }
}

@Composable
fun SystemToolsOverlay(
    viewModel: MainViewModel,
    isVisible: Boolean,
    onClose: () -> Unit
) {
    if (!isVisible) return
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
                    .fillMaxWidth(if (isCompact) 0.95f else 0.7f)
                    .fillMaxHeight(0.8f)
                    .background(Color(0xE6121214)) // Glassmorphism reflection
                    .border(1.dp, Color.White.copy(alpha = 0.1f))
                    .clickable { }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "SYSTEM_TOOLS", style = RasterTypography.titleMedium, color = Color(0xFFFF5722))
                    Text(text = "[X]", style = RasterTypography.titleMedium, color = Color.Gray, modifier = Modifier.clickable { onClose() })
                }

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    KitRandomizer(
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    )
                    
                    SceneBank(
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    )
                }
            }
        }
    }
}
