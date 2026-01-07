package com.imu.verba.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imu.verba.document.model.BlockType
import com.imu.verba.document.model.DocBlock
import com.imu.verba.tts.TtsPlaybackState
import com.imu.verba.ui.components.CodeBlock
import com.imu.verba.ui.components.TableBlock
import com.imu.verba.viewmodel.ReaderUiState
import com.imu.verba.viewmodel.ReaderViewModel

/**
 * Main reader screen for displaying documents with TTS controls.
 */
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                LoadingState()
            }
            uiState.error != null -> {
                ErrorState(message = uiState.error!!)
            }
            uiState.document != null -> {
                DocumentContent(
                    uiState = uiState,
                    onBlockTap = { blockId -> viewModel.playFromBlock(blockId) },
                    onStartHere = { blockId -> viewModel.setStartBlock(blockId) },
                    onEndHere = { blockId -> viewModel.setEndBlock(blockId) }
                )
            }
            else -> {
                EmptyState()
            }
        }

        // Slim TTS controls at bottom
        if (uiState.document != null) {
            TtsControlBar(
                uiState = uiState,
                onPlay = { viewModel.play() },
                onPause = { viewModel.pause() },
                onResume = { viewModel.resume() },
                onStop = { viewModel.stop() },
                onSpeedChange = { rate -> viewModel.setSpeechRate(rate) },
                onVoiceSelect = { voice -> viewModel.setVoice(voice) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Open a document to start reading",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DocumentContent(
    uiState: ReaderUiState,
    onBlockTap: (Int) -> Unit,
    onStartHere: (Int) -> Unit,
    onEndHere: (Int) -> Unit
) {
    val document = uiState.document ?: return
    val listState = rememberLazyListState()

    // Auto-scroll to current block during playback
    LaunchedEffect(uiState.currentBlockId) {
        uiState.currentBlockId?.let { blockId ->
            val index = document.blocks.indexOfFirst { it.id == blockId }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 100.dp // Space for floating controls
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Document title
        item {
            Text(
                text = document.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Document blocks
        items(document.blocks, key = { it.id }) { block ->
            BlockItem(
                block = block,
                isCurrentlyPlaying = uiState.currentBlockId == block.id,
                isStartBlock = uiState.startBlockId == block.id,
                isEndBlock = uiState.endBlockId == block.id,
                onTap = { onBlockTap(block.id) },
                onStartHere = { onStartHere(block.id) },
                onEndHere = { onEndHere(block.id) }
            )
        }
    }
}

@Composable
private fun BlockItem(
    block: DocBlock,
    isCurrentlyPlaying: Boolean,
    isStartBlock: Boolean,
    isEndBlock: Boolean,
    onTap: () -> Unit,
    onStartHere: () -> Unit,
    onEndHere: () -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }

    val backgroundColor = when {
        isCurrentlyPlaying -> MaterialTheme.colorScheme.primaryContainer
        isStartBlock || isEndBlock -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }

    Box {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { offset ->
                            contextMenuOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                            showContextMenu = true
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Range markers
                if (isStartBlock || isEndBlock) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isStartBlock) {
                            RangeMarker(text = "Start", isStart = true)
                        }
                        if (isEndBlock) {
                            RangeMarker(text = "End", isStart = false)
                        }
                    }
                }

                // Render based on block type
                when (block.type) {
                    BlockType.TABLE -> {
                        block.tableData?.let { tableData ->
                            TableBlock(tableData = tableData)
                        }
                    }
                    BlockType.CODE_BLOCK -> {
                        CodeBlock(code = block.text)
                    }
                    BlockType.HEADING -> {
                        Text(
                            text = block.text,
                            style = when (block.headingLevel) {
                                1 -> MaterialTheme.typography.headlineLarge
                                2 -> MaterialTheme.typography.headlineMedium
                                3 -> MaterialTheme.typography.headlineSmall
                                4 -> MaterialTheme.typography.titleLarge
                                5 -> MaterialTheme.typography.titleMedium
                                else -> MaterialTheme.typography.titleSmall
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    BlockType.LIST -> {
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    BlockType.PARAGRAPH -> {
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        // Context menu for range selection
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            offset = contextMenuOffset
        ) {
            DropdownMenuItem(
                text = { Text("Start reading here") },
                onClick = {
                    onStartHere()
                    showContextMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Stop reading here") },
                onClick = {
                    onEndHere()
                    showContextMenu = false
                }
            )
        }
    }
}

@Composable
private fun RangeMarker(text: String, isStart: Boolean) {
    Surface(
        color = if (isStart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isStart) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * Slim bottom TTS control bar - like a media player.
 */
@Composable
private fun TtsControlBar(
    uiState: ReaderUiState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVoiceSelect: (android.speech.tts.Voice) -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Speed button
            SpeedButton(
                currentSpeed = uiState.speechRate,
                onSpeedChange = onSpeedChange
            )

            // Center playback controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Stop button (only when playing or paused)
                if (uiState.playbackState != TtsPlaybackState.IDLE) {
                    IconButton(onClick = onStop) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Main play/pause button
                IconButton(
                    onClick = {
                        when (uiState.playbackState) {
                            TtsPlaybackState.IDLE -> onPlay()
                            TtsPlaybackState.PLAYING -> onPause()
                            TtsPlaybackState.PAUSED -> onResume()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = when (uiState.playbackState) {
                            TtsPlaybackState.PLAYING -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = when (uiState.playbackState) {
                            TtsPlaybackState.PLAYING -> "Pause"
                            else -> "Play"
                        },
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Voice selector
            VoiceButton(
                uiState = uiState,
                onVoiceSelect = onVoiceSelect
            )
        }
    }
}

@Composable
private fun SpeedButton(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

    Box {
        Surface(
            onClick = { expanded = true },
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "${currentSpeed}x",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            speeds.forEach { speed ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${speed}x",
                            fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSpeedChange(speed)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun VoiceButton(
    uiState: ReaderUiState,
    onVoiceSelect: (android.speech.tts.Voice) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Voice",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (uiState.availableVoices.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No voices available") },
                    onClick = { expanded = false }
                )
            } else {
                uiState.availableVoices.take(15).forEach { ttsVoice ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = ttsVoice.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (ttsVoice.voice == uiState.currentVoice)
                                        FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = ttsVoice.languageDisplay,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        },
                        onClick = {
                            onVoiceSelect(ttsVoice.voice)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
