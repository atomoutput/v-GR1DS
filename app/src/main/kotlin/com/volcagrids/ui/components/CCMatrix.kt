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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.volcagrids.midi.VolcaParameter
import com.volcagrids.ui.theme.RasterTypography

/**
 * CC Assignment Matrix - Grid for routing topographic levels to any CC parameter
 * 6 parts × 4 modulation slots each
 * Each slot can assign a source (level) to a destination (CC)
 */

data class ModulationSlot(
    val partIndex: Int,
    val slotIndex: Int,
    val source: ModSource = ModSource.NONE,
    val destination: VolcaParameter? = null,
    val amount: Int = 64,
    val enabled: Boolean = false
)

enum class ModSource {
    NONE,
    LEVEL_BD,
    LEVEL_SD,
    LEVEL_HH,
    DENSITY_BD,
    DENSITY_SD,
    DENSITY_HH,
    X_AXIS,
    Y_AXIS,
    RANDOM
}

@Composable
fun CCMatrix(
    modifier: Modifier = Modifier,
    slots: List<ModulationSlot> = emptyList(),
    onSlotChange: (ModulationSlot) -> Unit = {},
    accentColor: Color = Color(0xFFFFD600) // Gold for modulation
) {
    Column(
        modifier = modifier
            .background(Color.Transparent)
            .padding(8.dp)
    ) {
        Text(
            text = "MODULATION_MATRIX",
            style = RasterTypography.labelSmall,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "SRC",
                style = RasterTypography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "►",
                style = RasterTypography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.weight(0.5f)
            )
            Text(
                text = "DEST",
                style = RasterTypography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "AMT",
                style = RasterTypography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Slots for each part
        (0..5).forEach { partIndex ->
            Text(
                text = "PART_${partIndex + 1}",
                style = RasterTypography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            (0..3).forEach { slotIndex ->
                val slot = slots.find { it.partIndex == partIndex && it.slotIndex == slotIndex }
                    ?: ModulationSlot(partIndex, slotIndex)
                
                ModulationSlotRow(
                    slot = slot,
                    onSlotChange = onSlotChange,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(2.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ModulationSlotRow(
    slot: ModulationSlot,
    onSlotChange: (ModulationSlot) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .border(
                1.dp,
                if (slot.enabled) accentColor else Color(0xFF2A2A2E)
            )
            .background(
                if (slot.enabled) accentColor.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Source selector
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    // Cycle through sources
                    val nextSource = when (slot.source) {
                        ModSource.NONE -> ModSource.LEVEL_BD
                        ModSource.LEVEL_BD -> ModSource.LEVEL_SD
                        ModSource.LEVEL_SD -> ModSource.LEVEL_HH
                        ModSource.LEVEL_HH -> ModSource.DENSITY_BD
                        ModSource.DENSITY_BD -> ModSource.DENSITY_SD
                        ModSource.DENSITY_SD -> ModSource.DENSITY_HH
                        ModSource.DENSITY_HH -> ModSource.X_AXIS
                        ModSource.X_AXIS -> ModSource.Y_AXIS
                        ModSource.Y_AXIS -> ModSource.RANDOM
                        ModSource.RANDOM -> ModSource.NONE
                    }
                    onSlotChange(slot.copy(source = nextSource, enabled = nextSource != ModSource.NONE))
                }
        ) {
            Text(
                text = slot.source.name.take(4),
                style = RasterTypography.labelSmall,
                color = if (slot.enabled) accentColor else Color.Gray
            )
        }
        
        // Arrow
        Text(
            text = "►",
            style = RasterTypography.labelSmall,
            color = if (slot.enabled) accentColor else Color.Gray,
            modifier = Modifier.weight(0.5f)
        )
        
        // Destination selector
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    // Cycle through common destinations
                    val currentDest = slot.destination
                    val nextDest = when (currentDest) {
                        null -> VolcaParameter.DRIVE
                        VolcaParameter.DRIVE -> VolcaParameter.BIT_REDUCTION
                        VolcaParameter.BIT_REDUCTION -> VolcaParameter.FOLD
                        VolcaParameter.FOLD -> VolcaParameter.DRY_GAIN
                        VolcaParameter.DRY_GAIN -> VolcaParameter.SEND
                        VolcaParameter.SEND -> VolcaParameter.WAVEGUIDE_DECAY
                        VolcaParameter.WAVEGUIDE_DECAY -> VolcaParameter.WAVEGUIDE_BODY
                        VolcaParameter.WAVEGUIDE_BODY -> VolcaParameter.EG_ATTACK_1
                        VolcaParameter.EG_ATTACK_1 -> VolcaParameter.EG_RELEASE_1
                        VolcaParameter.EG_RELEASE_1 -> VolcaParameter.PITCH_1
                        VolcaParameter.PITCH_1 -> VolcaParameter.MOD_AMOUNT_1
                        VolcaParameter.MOD_AMOUNT_1 -> null
                        else -> VolcaParameter.DRIVE
                    }
                    onSlotChange(slot.copy(destination = nextDest, enabled = nextDest != null))
                }
        ) {
            val destText = slot.destination?.name?.take(6) ?: "---"
            Text(
                text = destText,
                style = RasterTypography.labelSmall,
                color = if (slot.destination != null) accentColor else Color.Gray
            )
        }
        
        // Amount slider
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Mini vertical fader
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        // Toggle amount
                        val newAmount = if (slot.amount < 64) 127 else 64
                        onSlotChange(slot.copy(amount = newAmount))
                    }
            ) {
                val w = size.width
                val h = size.height
                val fillHeight = h * (slot.amount / 127f)
                
                // Background
                drawRect(
                    color = Color(0xFF1A1A1E),
                    size = Size(w, h)
                )
                
                // Fill
                drawRect(
                    color = accentColor.copy(alpha = 0.5f),
                    topLeft = Offset(0f, h - fillHeight),
                    size = Size(w, fillHeight)
                )
                
                // Handle
                val handleY = h - fillHeight
                drawLine(
                    color = accentColor,
                    start = Offset(0f, handleY),
                    end = Offset(w, handleY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

/**
 * Compact CC assignment for single parameter
 */
@Composable
fun CCAssignmentCompact(
    modifier: Modifier = Modifier,
    label: String,
    source: ModSource,
    destination: VolcaParameter?,
    amount: Int,
    onSourceChange: (ModSource) -> Unit = {},
    onDestinationChange: (VolcaParameter?) -> Unit = {},
    onAmountChange: (Int) -> Unit = {},
    accentColor: Color = Color(0xFFFFD600)
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .border(1.dp, if (destination != null) accentColor else Color(0xFF2A2A2E))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label
        Text(
            text = label,
            style = RasterTypography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.width(40.dp)
        )
        
        // Source
        Box(
            modifier = Modifier
                .clickable {
                    val nextSource = when (source) {
                        ModSource.NONE -> ModSource.LEVEL_BD
                        ModSource.LEVEL_BD -> ModSource.LEVEL_SD
                        ModSource.LEVEL_SD -> ModSource.LEVEL_HH
                        ModSource.LEVEL_HH -> ModSource.NONE
                        else -> ModSource.LEVEL_BD
                    }
                    onSourceChange(nextSource)
                }
        ) {
            Text(
                text = source.name.take(4),
                style = RasterTypography.labelSmall,
                color = if (source != ModSource.NONE) accentColor else Color.Gray
            )
        }
        
        // Arrow
        Text("►", style = RasterTypography.labelSmall, color = Color.Gray)
        
        // Destination
        Box(
            modifier = Modifier
                .clickable {
                    val nextDest = when (destination) {
                        null -> VolcaParameter.DRIVE
                        VolcaParameter.DRIVE -> VolcaParameter.BIT_REDUCTION
                        VolcaParameter.BIT_REDUCTION -> VolcaParameter.FOLD
                        else -> null
                    }
                    onDestinationChange(nextDest)
                }
        ) {
            Text(
                text = destination?.cc?.toString() ?: "---",
                style = RasterTypography.labelSmall,
                color = if (destination != null) accentColor else Color.Gray,
                modifier = Modifier.width(30.dp)
            )
        }
        
        // Amount
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .border(1.dp, accentColor)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            val delta = (dragAmount.x / 2).toInt()
                            onAmountChange((amount + delta).coerceIn(0, 127))
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fillWidth = size.width * (amount / 127f)
                drawRect(
                    color = accentColor,
                    size = Size(fillWidth, size.height)
                )
            }
            
            Text(
                text = amount.toString(),
                style = RasterTypography.labelSmall,
                color = accentColor,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
