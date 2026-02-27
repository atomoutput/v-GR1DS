package com.volcagrids.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.volcagrids.ui.theme.RasterTypography
import com.volcagrids.ui.theme.RasterGrid
import com.volcagrids.ui.theme.RasterSignalA
import com.volcagrids.ui.theme.RasterDark

/**
 * Plaits Engine Selector
 * Grid of available synthesis engines
 */
data class EngineItem(
    val index: Int,
    val name: String,
    val category: EngineCategory
)

enum class EngineCategory(val displayName: String) {
    SYNTHETIC("Synthetic"),
    DRUM("Drum"),
    PERCUSSIVE("Percussive"),
    SPEECH("Speech")
}

val allEngines = listOf(
    EngineItem(0, "Virtual Analog", EngineCategory.SYNTHETIC),
    EngineItem(1, "FM", EngineCategory.SYNTHETIC),
    EngineItem(2, "Wavetable", EngineCategory.SYNTHETIC),
    EngineItem(3, "Noise", EngineCategory.SYNTHETIC),
    // Additional engines would be added here as they are ported
    EngineItem(4, "Waveshaping", EngineCategory.SYNTHETIC),
    EngineItem(5, "Grain", EngineCategory.SYNTHETIC),
    EngineItem(6, "Additive", EngineCategory.SYNTHETIC),
    EngineItem(7, "Chord", EngineCategory.SYNTHETIC),
    EngineItem(8, "Speech", EngineCategory.SPEECH),
    EngineItem(9, "Swarm", EngineCategory.SYNTHETIC),
    EngineItem(10, "Particle", EngineCategory.SYNTHETIC),
    EngineItem(11, "String", EngineCategory.SYNTHETIC),
    EngineItem(12, "Modal", EngineCategory.SYNTHETIC),
    EngineItem(13, "Bass Drum", EngineCategory.DRUM),
    EngineItem(14, "Snare Drum", EngineCategory.DRUM),
    EngineItem(15, "Hi-Hat", EngineCategory.DRUM)
)

@Composable
fun PlaitsEngineSelector(
    selectedEngine: Int,
    onEngineSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = RasterSignalA
) {
    Column(
        modifier = modifier
            .background(RasterDark)
            .padding(12.dp)
    ) {
        // Header
        Text(
            text = "ENGINE SELECT",
            style = RasterTypography.headlineLarge,
            color = accentColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Engine grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(allEngines) { engine ->
                EngineCell(
                    engine = engine,
                    isSelected = engine.index == selectedEngine,
                    accentColor = accentColor,
                    onSelect = { onEngineSelected(engine.index) }
                )
            }
        }
    }
}

@Composable
private fun EngineCell(
    engine: EngineItem,
    isSelected: Boolean,
    accentColor: Color,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1.2f)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else RasterGrid
            )
            .background(
                color = if (isSelected) accentColor.copy(alpha = 0.1f) else RasterGrid
            )
            .clickable { onSelect() }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = engine.index.toString(),
                style = RasterTypography.headlineLarge,
                color = if (isSelected) accentColor else RasterSignalA
            )
            Text(
                text = engine.name,
                style = RasterTypography.labelSmall,
                color = Color.Gray,
                maxLines = 2
            )
        }
    }
}

/**
 * Compact engine selector for small displays
 */
@Composable
fun PlaitsEngineSelectorCompact(
    selectedEngine: Int,
    onEngineSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = RasterSignalA
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Current engine display
        Row(
            modifier = Modifier
                .border(1.dp, RasterGrid)
                .background(RasterDark)
                .clickable { expanded = true }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ENGINE ",
                style = RasterTypography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = selectedEngine.toString(),
                style = RasterTypography.headlineLarge,
                color = accentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            val currentEngine = allEngines.find { it.index == selectedEngine }
            Text(
                text = currentEngine?.name ?: "Unknown",
                style = RasterTypography.bodyMedium,
                color = RasterSignalA
            )
        }

        // Dropdown when expanded
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { expanded = false }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    items(allEngines) { engine ->
                        EngineCell(
                            engine = engine,
                            isSelected = engine.index == selectedEngine,
                            accentColor = accentColor,
                            onSelect = {
                                onEngineSelected(engine.index)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
