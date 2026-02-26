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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.volcagrids.ui.theme.RasterTypography

/**
 * Scene Bank - 8 snapshot slots for saving/recalling states
 */

data class SceneData(
    val index: Int,
    val name: String,
    val engineAX: Int,
    val engineAY: Int,
    val engineBX: Int,
    val engineBY: Int,
    val densitiesA: List<Int>,
    val densitiesB: List<Int>,
    val randomnessA: Int,
    val randomnessB: Int,
    val isSaved: Boolean = false
)

@Composable
fun SceneBank(
    modifier: Modifier = Modifier,
    scenes: List<SceneData> = emptyList(),
    currentSceneA: Int = -1,
    currentSceneB: Int = -1,
    onSaveScene: (Int) -> Unit = {},
    onLoadSceneA: (Int) -> Unit = {},
    onLoadSceneB: (Int) -> Unit = {},
    onDeleteScene: (Int) -> Unit = {},
    accentColor: Color = Color(0xFFFFD600) // Gold for scenes
) {
    Column(
        modifier = modifier
            .background(Color(0xFF121214))
            .padding(8.dp)
    ) {
        Text(
            text = "SCENE_BANK",
            style = RasterTypography.labelSmall,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 8 scene slots in 2 rows of 4
        (0..7).chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { index ->
                    val scene = scenes.find { it.index == index }
                    SceneSlot(
                        scene = scene ?: SceneData(index, "", 127, 127, 127, 127, listOf(127, 127, 127), listOf(127, 127, 127), 0, 0),
                        isSceneA = index == currentSceneA,
                        isSceneB = index == currentSceneB,
                        onSave = { onSaveScene(index) },
                        onLoadA = { onLoadSceneA(index) },
                        onLoadB = { onLoadSceneB(index) },
                        onDelete = { onDeleteScene(index) },
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        // Instructions
        Text(
            text = "LONG_PRESS_SAVE // TAP_LOAD_A // SHIFT+TAP_LOAD_B",
            style = RasterTypography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun SceneSlot(
    scene: SceneData,
    isSceneA: Boolean,
    isSceneB: Boolean,
    onSave: () -> Unit,
    onLoadA: () -> Unit,
    onLoadB: () -> Unit,
    onDelete: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var showActions by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .border(
                1.dp,
                when {
                    isSceneA && isSceneB -> Color.Magenta
                    isSceneA -> Color(0xFFFF5722) // Amber for A
                    isSceneB -> Color(0xFF00B0FF) // Blue for B
                    scene.isSaved -> accentColor
                    else -> Color(0xFF2A2A2E)
                }
            )
            .background(
                when {
                    isSceneA && isSceneB -> Color.Magenta.copy(alpha = 0.2f)
                    isSceneA -> Color(0xFFFF5722).copy(alpha = 0.2f)
                    isSceneB -> Color(0xFF00B0FF).copy(alpha = 0.2f)
                    scene.isSaved -> accentColor.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            )
            .clickable { onLoadA() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scene number
            Text(
                text = "S${scene.index + 1}",
                style = RasterTypography.labelSmall,
                color = if (scene.isSaved) accentColor else Color.Gray,
                fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
            
            // Scene name or empty indicator
            Text(
                text = if (scene.name.isNotEmpty()) scene.name.take(6) else if (scene.isSaved) "---" else "empty",
                style = RasterTypography.labelSmall,
                color = if (scene.isSaved) Color.White else Color.Gray,
                fontSize = androidx.compose.ui.unit.TextUnit(7f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
            
            // Load indicators
            if (isSceneA) {
                Text(
                    text = "[A]",
                    style = RasterTypography.labelSmall,
                    color = Color(0xFFFF5722),
                    fontSize = androidx.compose.ui.unit.TextUnit(6f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            }
            if (isSceneB) {
                Text(
                    text = "[B]",
                    style = RasterTypography.labelSmall,
                    color = Color(0xFF00B0FF),
                    fontSize = androidx.compose.ui.unit.TextUnit(6f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            }
        }
        
        // Action overlay (shown on long press or hover)
        if (showActions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { }
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "[SAVE]",
                        style = RasterTypography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.clickable { onSave() }
                    )
                    Text(
                        text = "[LOAD_B]",
                        style = RasterTypography.labelSmall,
                        color = Color(0xFF00B0FF),
                        modifier = Modifier.clickable { onLoadB() }
                    )
                    Text(
                        text = "[DEL]",
                        style = RasterTypography.labelSmall,
                        color = Color(0xFFFF5757),
                        modifier = Modifier.clickable { onDelete() }
                    )
                }
            }
        }
    }
}

/**
 * Morph Fader - Crossfader between Scene A and Scene B
 */
@Composable
fun MorphFader(
    modifier: Modifier = Modifier,
    morphFactor: Float,
    sceneAName: String = "A",
    sceneBName: String = "B",
    onMorphChange: (Float) -> Unit = {},
    accentColor: Color = Color(0xFFFFFFFF)
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Scene labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SCENE_A:$sceneAName",
                style = RasterTypography.labelSmall,
                color = Color(0xFFFF5722)
            )
            Text(
                text = "SCENE_B:$sceneBName",
                style = RasterTypography.labelSmall,
                color = Color(0xFF00B0FF)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Fader track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .border(1.dp, if (isDragging) accentColor else Color(0xFF333333))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDrag = { change, _ ->
                            val x = change.position.x
                            val width = size.width
                            val newValue = (x / width).coerceIn(0f, 1f)
                            onMorphChange(newValue)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // Gradient background showing morph interpolation
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF5722).copy(alpha = 0.3f),
                            Color(0xFF00B0FF).copy(alpha = 0.3f)
                        )
                    ),
                    size = Size(w, h)
                )
                
                // Center indicator
                val centerX = w * morphFactor
                drawLine(
                    color = accentColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, h),
                    strokeWidth = 2.dp.toPx()
                )
                
                // Handle
                drawCircle(
                    color = if (isDragging) accentColor else Color.White,
                    radius = 12.dp.toPx(),
                    center = Offset(centerX, h / 2)
                )
                
                // Handle inner
                drawCircle(
                    color = Color.Black,
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, h / 2)
                )
            }
            
            // Percentage display
            Text(
                text = "${(morphFactor * 100).toInt()}%",
                style = RasterTypography.labelSmall,
                color = accentColor,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        
        // Morph label
        Text(
            text = "MORPH:A◄►B",
            style = RasterTypography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
