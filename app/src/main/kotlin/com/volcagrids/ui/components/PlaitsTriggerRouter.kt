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
import com.volcagrids.plaits.PlaitsTrack
import com.volcagrids.plaits.TriggerSource
import com.volcagrids.ui.theme.RasterDark
import com.volcagrids.ui.theme.RasterSignalA
import com.volcagrids.ui.theme.RasterTypography

/**
 * Plaits Trigger Router
 * Configure which Grids parts trigger each Plaits track
 */
@Composable
fun PlaitsTriggerRouter(
    track: PlaitsTrack,
    onTriggerSourcesChange: (List<TriggerSource>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(RasterDark)
            .padding(12.dp)
    ) {
        Text(
            text = "TRIGGER ROUTING: ${track.name}",
            style = RasterTypography.headlineLarge,
            color = RasterSignalA,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Engine A triggers
        Text(
            text = "ENGINE A",
            style = RasterTypography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TriggerSourceButton(
                label = "P1",
                isActive = track.triggerSources.contains(TriggerSource.ENGINE_A_PART1),
                onClick = {
                    if (track.triggerSources.contains(TriggerSource.ENGINE_A_PART1)) {
                        track.removeTriggerSource(TriggerSource.ENGINE_A_PART1)
                    } else {
                        track.addTriggerSource(TriggerSource.ENGINE_A_PART1)
                    }
                    onTriggerSourcesChange(track.triggerSources)
                }
            )
            TriggerSourceButton(
                label = "P2",
                isActive = track.triggerSources.contains(TriggerSource.ENGINE_A_PART2),
                onClick = {
                    if (track.triggerSources.contains(TriggerSource.ENGINE_A_PART2)) {
                        track.removeTriggerSource(TriggerSource.ENGINE_A_PART2)
                    } else {
                        track.addTriggerSource(TriggerSource.ENGINE_A_PART2)
                    }
                    onTriggerSourcesChange(track.triggerSources)
                }
            )
            TriggerSourceButton(
                label = "P3",
                isActive = track.triggerSources.contains(TriggerSource.ENGINE_A_PART3),
                onClick = {
                    if (track.triggerSources.contains(TriggerSource.ENGINE_A_PART3)) {
                        track.removeTriggerSource(TriggerSource.ENGINE_A_PART3)
                    } else {
                        track.addTriggerSource(TriggerSource.ENGINE_A_PART3)
                    }
                    onTriggerSourcesChange(track.triggerSources)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Engine B triggers
        Text(
            text = "ENGINE B",
            style = RasterTypography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TriggerSourceButton(
                label = "P1",
                isActive = track.triggerSources.contains(TriggerSource.ENGINE_B_PART1),
                onClick = {
                    if (track.triggerSources.contains(TriggerSource.ENGINE_B_PART1)) {
                        track.removeTriggerSource(TriggerSource.ENGINE_B_PART1)
                    } else {
                        track.addTriggerSource(TriggerSource.ENGINE_B_PART1)
                    }
                    onTriggerSourcesChange(track.triggerSources)
                }
            )
            TriggerSourceButton(
                label = "P2",
                isActive = track.triggerSources.contains(TriggerSource.ENGINE_B_PART2),
                onClick = {
                    if (track.triggerSources.contains(TriggerSource.ENGINE_B_PART2)) {
                        track.removeTriggerSource(TriggerSource.ENGINE_B_PART2)
                    } else {
                        track.addTriggerSource(TriggerSource.ENGINE_B_PART2)
                    }
                    onTriggerSourcesChange(track.triggerSources)
                }
            )
            TriggerSourceButton(
                label = "P3",
                isActive = track.triggerSources.contains(TriggerSource.ENGINE_B_PART3),
                onClick = {
                    if (track.triggerSources.contains(TriggerSource.ENGINE_B_PART3)) {
                        track.removeTriggerSource(TriggerSource.ENGINE_B_PART3)
                    } else {
                        track.addTriggerSource(TriggerSource.ENGINE_B_PART3)
                    }
                    onTriggerSourcesChange(track.triggerSources)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Polyrhythm triggers
        Text(
            text = "POLYRHYTHM",
            style = RasterTypography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 1..6) {
                val source = when (i) {
                    1 -> TriggerSource.POLYRHYTHM_1
                    2 -> TriggerSource.POLYRHYTHM_2
                    3 -> TriggerSource.POLYRHYTHM_3
                    4 -> TriggerSource.POLYRHYTHM_4
                    5 -> TriggerSource.POLYRHYTHM_5
                    6 -> TriggerSource.POLYRHYTHM_6
                    else -> null
                }
                source?.let {
                    TriggerSourceButton(
                        label = "$i",
                        isActive = track.triggerSources.contains(it),
                        onClick = {
                            if (track.triggerSources.contains(it)) {
                                track.removeTriggerSource(it)
                            } else {
                                track.addTriggerSource(it)
                            }
                            onTriggerSourcesChange(track.triggerSources)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick presets
        Text(
            text = "PRESETS",
            style = RasterTypography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PresetButton(
                label = "CLEAR",
                onClick = {
                    track.clearTriggerSources()
                    onTriggerSourcesChange(emptyList())
                }
            )
            PresetButton(
                label = "A1+B1",
                onClick = {
                    track.clearTriggerSources()
                    track.addTriggerSource(TriggerSource.ENGINE_A_PART1)
                    track.addTriggerSource(TriggerSource.ENGINE_B_PART1)
                    onTriggerSourcesChange(track.triggerSources)
                }
            )
            PresetButton(
                label = "ALL A",
                onClick = {
                    track.clearTriggerSources()
                    track.addTriggerSource(TriggerSource.ENGINE_A_PART1)
                    track.addTriggerSource(TriggerSource.ENGINE_A_PART2)
                    track.addTriggerSource(TriggerSource.ENGINE_A_PART3)
                    onTriggerSourcesChange(track.triggerSources)
                }
            )
            PresetButton(
                label = "ALL B",
                onClick = {
                    track.clearTriggerSources()
                    track.addTriggerSource(TriggerSource.ENGINE_B_PART1)
                    track.addTriggerSource(TriggerSource.ENGINE_B_PART2)
                    track.addTriggerSource(TriggerSource.ENGINE_B_PART3)
                    onTriggerSourcesChange(track.triggerSources)
                }
            )
        }
    }
}

@Composable
private fun TriggerSourceButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (isActive) RasterSignalA else Color.DarkGray
            )
            .background(
                color = if (isActive) RasterSignalA.copy(alpha = 0.2f) else RasterDark
            )
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = RasterTypography.bodyMedium,
            color = if (isActive) RasterSignalA else Color.Gray
        )
    }
}

@Composable
private fun PresetButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.DarkGray)
            .background(RasterDark)
            .clickable { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = RasterTypography.labelSmall,
            color = Color.Gray
        )
    }
}
