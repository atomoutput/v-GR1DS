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
import com.volcagrids.ui.theme.RasterTypography
import kotlin.random.Random

/**
 * Kit Randomizer Panel - Intelligent randomization for Volca Drum kits
 * Generates new kits by randomizing Pitch, Mod Amount, and EG parameters
 * with genre-based constraints
 */

enum class GenreConstraint {
    TECHNO,
    AMBIENT,
    GLITCH,
    IDM,
    FREE
}

data class RandomizerRange(
    val min: Int = 0,
    val max: Int = 127,
    val default: Int = 64
)

data class KitParameters(
    val pitch1: Int = 64,
    val pitch2: Int = 64,
    val modAmount1: Int = 64,
    val modAmount2: Int = 64,
    val egAttack1: Int = 64,
    val egAttack2: Int = 64,
    val egRelease1: Int = 64,
    val egRelease2: Int = 64,
    val bitReduction: Int = 0,
    val fold: Int = 0,
    val drive: Int = 64
)

@Composable
fun KitRandomizer(
    modifier: Modifier = Modifier,
    currentGenre: GenreConstraint = GenreConstraint.FREE,
    onGenreChange: (GenreConstraint) -> Unit = {},
    onRandomize: (KitParameters) -> Unit = {},
    onPreview: () -> Unit = {},
    accentColor: Color = Color(0xFF00E5FF) // Teal for randomizer
) {
    var pitchRange by remember { mutableStateOf(64) }
    var modRange by remember { mutableStateOf(64) }
    var egRange by remember { mutableStateOf(64) }
    
    Column(
        modifier = modifier
            .background(Color(0xFF121214))
            .border(1.dp, Color(0xFF2A2A2E))
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KIT_RANDOMIZER",
                style = RasterTypography.labelSmall,
                color = accentColor
            )
            
            // Genre selector
            GenreSelector(
                currentGenre = currentGenre,
                onGenreChange = onGenreChange,
                accentColor = accentColor
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Randomize buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SystemButton(
                label = "[RAND:PITCH]",
                isActive = false,
                onClick = {
                    val params = generateRandomParameters(currentGenre, pitchRange, 0, 0)
                    onRandomize(params)
                },
                modifier = Modifier.weight(1f)
            )
            
            SystemButton(
                label = "[RAND:MOD]",
                isActive = false,
                onClick = {
                    val params = generateRandomParameters(currentGenre, 0, modRange, 0)
                    onRandomize(params)
                },
                modifier = Modifier.weight(1f)
            )
            
            SystemButton(
                label = "[RAND:EG]",
                isActive = false,
                onClick = {
                    val params = generateRandomParameters(currentGenre, 0, 0, egRange)
                    onRandomize(params)
                },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SystemButton(
                label = "[RAND:ALL]",
                isActive = false,
                onClick = {
                    val params = generateRandomParameters(currentGenre, pitchRange, modRange, egRange)
                    onRandomize(params)
                },
                modifier = Modifier.weight(1f)
            )
            
            SystemButton(
                label = "[PREVIEW]",
                isActive = false,
                onClick = onPreview,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Range sliders
        Text(
            text = "RANDOMIZATION_RANGES",
            style = RasterTypography.labelSmall,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Pitch range
        RangeSlider(
            label = "PITCH",
            value = pitchRange,
            onValueChange = { pitchRange = it },
            accentColor = accentColor
        )
        
        // Mod range
        RangeSlider(
            label = "MOD",
            value = modRange,
            onValueChange = { modRange = it },
            accentColor = accentColor
        )
        
        // EG range
        RangeSlider(
            label = "EG",
            value = egRange,
            onValueChange = { egRange = it },
            accentColor = accentColor
        )
    }
}

@Composable
private fun GenreSelector(
    currentGenre: GenreConstraint,
    onGenreChange: (GenreConstraint) -> Unit,
    accentColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        GenreConstraint.entries.forEach { genre ->
            val isActive = genre == currentGenre
            Box(
                modifier = Modifier
                    .border(
                        1.dp,
                        if (isActive) accentColor else Color(0xFF333333)
                    )
                    .background(
                        if (isActive) accentColor.copy(alpha = 0.2f) else Color.Transparent
                    )
                    .clickable { onGenreChange(genre) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = genre.name.take(4),
                    style = RasterTypography.labelSmall,
                    color = if (isActive) accentColor else Color.Gray
                )
            }
        }
    }
}

@Composable
private fun RangeSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    accentColor: Color
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = RasterTypography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.width(40.dp)
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, if (isDragging) accentColor else Color(0xFF333333))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            val delta = (dragAmount.x / 3).toInt()
                            onValueChange((value + delta).coerceIn(0, 127))
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val fillWidth = w * (value / 127f)
                
                // Background
                drawRect(
                    color = Color(0xFF1A1A1E),
                    size = Size(w, h)
                )
                
                // Fill
                drawRect(
                    color = accentColor.copy(alpha = 0.5f),
                    size = Size(fillWidth, h)
                )
                
                // Handle
                drawLine(
                    color = if (isDragging) accentColor else Color(0xFF666666),
                    start = Offset(fillWidth, 0f),
                    end = Offset(fillWidth, h),
                    strokeWidth = 2.dp.toPx()
                )
            }
            
            Text(
                text = value.toString(),
                style = RasterTypography.labelSmall,
                color = accentColor,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
            )
        }
    }
}

/**
 * Generate random parameters based on genre constraints
 */
fun generateRandomParameters(
    genre: GenreConstraint,
    pitchRange: Int,
    modRange: Int,
    egRange: Int
): KitParameters {
    val random = Random
    
    // Genre-based constraints
    val (pitchMin, pitchMax) = when (genre) {
        GenreConstraint.TECHNO -> 40 to 90
        GenreConstraint.AMBIENT -> 20 to 100
        GenreConstraint.GLITCH -> 10 to 120
        GenreConstraint.IDM -> 0 to 127
        GenreConstraint.FREE -> 0 to 127
    }
    
    val (attackMin, attackMax) = when (genre) {
        GenreConstraint.TECHNO -> 0 to 40  // Short attacks
        GenreConstraint.AMBIENT -> 60 to 127  // Long attacks
        GenreConstraint.GLITCH -> 0 to 30  // Very short
        GenreConstraint.IDM -> 0 to 127
        GenreConstraint.FREE -> 0 to 127
    }
    
    val (releaseMin, releaseMax) = when (genre) {
        GenreConstraint.TECHNO -> 20 to 60  // Short decay
        GenreConstraint.AMBIENT -> 80 to 127  // Long decay
        GenreConstraint.GLITCH -> 0 to 40  // Short
        GenreConstraint.IDM -> 0 to 127
        GenreConstraint.FREE -> 0 to 127
    }
    
    // Apply randomization within ranges
    fun randomize(base: Int, range: Int, min: Int, max: Int): Int {
        if (range == 0) return base
        val variation = (random.nextInt(range * 2) - range)
        return ((base + variation).coerceIn(min, max))
    }
    
    return KitParameters(
        pitch1 = randomize(64, pitchRange, pitchMin, pitchMax),
        pitch2 = randomize(64, pitchRange, pitchMin, pitchMax),
        modAmount1 = randomize(64, modRange, 0, 127),
        modAmount2 = randomize(64, modRange, 0, 127),
        egAttack1 = randomize(64, egRange, attackMin, attackMax),
        egAttack2 = randomize(64, egRange, attackMin, attackMax),
        egRelease1 = randomize(64, egRange, releaseMin, releaseMax),
        egRelease2 = randomize(64, egRange, releaseMin, releaseMax),
        bitReduction = when (genre) {
            GenreConstraint.GLITCH, GenreConstraint.IDM -> random.nextInt(80)
            else -> random.nextInt(20)
        },
        fold = when (genre) {
            GenreConstraint.TECHNO -> random.nextInt(60, 100)
            GenreConstraint.GLITCH -> random.nextInt(40, 127)
            else -> random.nextInt(40)
        },
        drive = random.nextInt(40, 100)
    )
}
