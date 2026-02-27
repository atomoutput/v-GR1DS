package com.volcagrids.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.volcagrids.plaits.PlaitsTrack
import com.volcagrids.ui.MainViewModel
import com.volcagrids.ui.theme.RasterBlack
import com.volcagrids.ui.theme.RasterDark
import com.volcagrids.ui.theme.RasterSignalA
import com.volcagrids.ui.theme.RasterTypography
import kotlinx.coroutines.launch

/**
 * Plaits Full-Screen Overlay Editor
 * Tabs: Engine | Parameters | Modulation | Track
 */
@Composable
fun PlaitsOverlay(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RasterBlack)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        // Swipe to change pages
                        if (dragAmount.x > 50f && pagerState.currentPage > 0) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        } else if (dragAmount.x < -50f && pagerState.currentPage < 3) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLAITS SYNTHESIZER",
                        style = RasterTypography.headlineLarge,
                        color = RasterSignalA
                    )

                    // Close button
                    Box(
                        modifier = Modifier
                            .border(1.dp, RasterSignalA)
                            .clickable { onClose() }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "✕",
                            style = RasterTypography.bodyMedium,
                            color = RasterSignalA
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab bar
                PlaitsTabBar(
                    currentPage = pagerState.currentPage,
                    onTabSelected = {
                        scope.launch {
                            pagerState.animateScrollToPage(it)
                        }
                    },
                    isCompact = isCompact
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> PlaitsEnginePage(viewModel)
                        1 -> PlaitsParametersPage(viewModel)
                        2 -> PlaitsModulationPage(viewModel)
                        3 -> PlaitsTrackPage(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaitsTabBar(
    currentPage: Int,
    onTabSelected: (Int) -> Unit,
    isCompact: Boolean
) {
    val tabs = listOf("ENGINE", "PARAMS", "MOD", "TRACK")
    val scope = rememberCoroutineScope()

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = currentPage == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) RasterSignalA else Color.DarkGray
                    )
                    .background(
                        color = if (isSelected) RasterSignalA.copy(alpha = 0.1f) else RasterDark
                    )
                    .clickable { 
                        scope.launch {
                            onTabSelected(index)
                        }
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    style = if (isCompact) RasterTypography.labelSmall else RasterTypography.bodyMedium,
                    color = if (isSelected) RasterSignalA else Color.Gray
                )
            }
        }
    }
}

@Composable
private fun PlaitsEnginePage(viewModel: MainViewModel) {
    val currentTrack = viewModel.getCurrentPlaitsTrack()
    val currentEngine = currentTrack?.getEngine() ?: 0
    val scope = rememberCoroutineScope()

    Column {
        // Track selector
        Text(
            text = "TRACK: ${currentTrack?.name ?: "Unknown"}",
            style = RasterTypography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Engine selector
        PlaitsEngineSelector(
            selectedEngine = currentEngine,
            onEngineSelected = { engine ->
                scope.launch {
                    viewModel.setPlaitsEngine(viewModel.currentPlaitsTrackIndex, engine)
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlaitsParametersPage(viewModel: MainViewModel) {
    val currentTrack = viewModel.getCurrentPlaitsTrack()
    val patch = currentTrack?.patch

    var harmonics by remember { mutableStateOf(patch?.harmonics ?: 0.5f) }
    var timbre by remember { mutableStateOf(patch?.timbre ?: 0.5f) }
    var morph by remember { mutableStateOf(patch?.morph ?: 0.5f) }
    var fmAmount by remember { mutableStateOf(patch?.frequencyModulationAmount ?: 0.0f) }
    var timbreModAmount by remember { mutableStateOf(patch?.timbreModulationAmount ?: 0.0f) }
    var morphModAmount by remember { mutableStateOf(patch?.morphModulationAmount ?: 0.0f) }
    var decay by remember { mutableStateOf(patch?.decay ?: 0.5f) }

    PlaitsParameterKnobs(
        harmonics = harmonics,
        timbre = timbre,
        morph = morph,
        fmAmount = fmAmount,
        timbreModAmount = timbreModAmount,
        morphModAmount = morphModAmount,
        decay = decay,
        onHarmonicsChange = {
            harmonics = it
            viewModel.setPlaitsParameters(viewModel.currentPlaitsTrackIndex, harmonics = it)
        },
        onTimbreChange = {
            timbre = it
            viewModel.setPlaitsParameters(viewModel.currentPlaitsTrackIndex, timbre = it)
        },
        onMorphChange = {
            morph = it
            viewModel.setPlaitsParameters(viewModel.currentPlaitsTrackIndex, morph = it)
        },
        onFmAmountChange = {
            fmAmount = it
            viewModel.setPlaitsParameters(viewModel.currentPlaitsTrackIndex, decay = it)
        },
        onTimbreModAmountChange = { timbreModAmount = it },
        onMorphModAmountChange = { morphModAmount = it },
        onDecayChange = {
            decay = it
            viewModel.setPlaitsParameters(viewModel.currentPlaitsTrackIndex, decay = it)
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun PlaitsModulationPage(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RasterDark)
            .padding(16.dp)
    ) {
        Text(
            text = "MODULATION MATRIX",
            style = RasterTypography.headlineLarge,
            color = RasterSignalA,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Coming soon: LFO and envelope modulation routing",
            style = RasterTypography.bodyMedium,
            color = Color.Gray
        )

        // Placeholder for modulation matrix
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, RasterSignalA.copy(alpha = 0.3f))
        ) {
            Text(
                text = "MOD MATRIX\n\nSources: LFO 1-4, Envelopes, Physics\nDestinations: All parameters\nAmounts: -100% to +100%",
                style = RasterTypography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun PlaitsTrackPage(viewModel: MainViewModel) {
    val currentTrack = viewModel.getCurrentPlaitsTrack()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RasterDark)
            .padding(16.dp)
    ) {
        Text(
            text = "TRACK SETTINGS",
            style = RasterTypography.headlineLarge,
            color = RasterSignalA,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Track selector
        Text(
            text = "SELECT TRACK",
            style = RasterTypography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            viewModel.plaitsTracks.forEachIndexed { index, track ->
                val isSelected = index == viewModel.currentPlaitsTrackIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) RasterSignalA else Color.DarkGray
                        )
                        .background(
                            color = if (isSelected) RasterSignalA.copy(alpha = 0.1f) else RasterBlack
                        )
                        .clickable { viewModel.setCurrentPlaitsTrack(index) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = RasterTypography.headlineLarge,
                        color = if (isSelected) RasterSignalA else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Track info
        if (currentTrack != null) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "NAME",
                        style = RasterTypography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = currentTrack.name,
                        style = RasterTypography.bodyMedium,
                        color = RasterSignalA
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ENGINE",
                        style = RasterTypography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = currentTrack.getEngineName(),
                        style = RasterTypography.bodyMedium,
                        color = RasterSignalA
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ENABLED",
                        style = RasterTypography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = if (currentTrack.enabled) "YES" else "NO",
                        style = RasterTypography.bodyMedium,
                        color = if (currentTrack.enabled) Color(0xFF00FF00) else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trigger Routing
        currentTrack?.let { track ->
            PlaitsTriggerRouter(
                track = track,
                onTriggerSourcesChange = { sources ->
                    // Trigger sources updated in track directly
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Enable/Disable toggle
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RasterSignalA)
                .clickable {
                    currentTrack?.enabled = !(currentTrack?.enabled ?: false)
                }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (currentTrack?.enabled == true) "DISABLE TRACK" else "ENABLE TRACK",
                style = RasterTypography.bodyMedium,
                color = RasterSignalA
            )
        }
    }
}
