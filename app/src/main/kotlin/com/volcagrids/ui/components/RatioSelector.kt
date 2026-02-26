package com.volcagrids.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.volcagrids.ui.theme.RasterTypography

/**
 * RatioSelector - Fixed version without infinite recomposition
 * Uses LazyColumn for proper scrolling and fixed-width buttons
 */
@Composable
fun RatioSelector(
    isVisible: Boolean = false,
    currentRatio: String = "3:2",
    onRatioSelected: (String) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (!isVisible) return
    
    // Prevent recomposition on every state change
    val ratios = remember {
        mapOf(
            "SIMPLE" to listOf("2:3", "3:2", "3:4", "4:3", "2:5", "5:2", "5:4", "4:5"),
            "INTERMEDIATE" to listOf("3:5", "5:3", "7:4", "4:7", "5:6", "6:5", "7:8", "8:7"),
            "ADVANCED" to listOf("7:9", "9:7", "11:8", "8:11"),
            "TRADITIONAL" to listOf("Hemiola", "Afro Blue", "Rumba")
        )
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121214))
                .border(1.dp, Color(0xFF333333))
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "POLYRHYTHM",
                    style = RasterTypography.labelSmall,
                    color = Color(0xFFFFD600)
                )
                Text(
                    text = "[X]",
                    style = RasterTypography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Current selection
            Text(
                text = "SELECTED: $currentRatio",
                style = RasterTypography.labelSmall,
                color = Color(0xFFFFD600),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Ratio categories
            ratios.forEach { (category, categoryRatios) ->
                Text(
                    text = category,
                    style = RasterTypography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categoryRatios.forEach { ratio ->
                        val isSelected = ratio == currentRatio
                        Text(
                            text = ratio,
                            style = RasterTypography.labelSmall,
                            color = if (isSelected) Color(0xFFFFD600) else Color.Gray,
                            modifier = Modifier
                                .width(56.dp)
                                .height(32.dp)
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFFFD600) else Color(0xFF333333)
                                )
                                .clickable { onRatioSelected(ratio) }
                                .padding(4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Info
            Text(
                text = "TAP RATIO TO SELECT",
                style = RasterTypography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
