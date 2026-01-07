package com.imu.verba.ui.settings

import android.speech.tts.Voice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.imu.verba.tts.TtsVoice
import kotlinx.coroutines.launch
import com.imu.verba.ui.theme.ThemeManager
import com.imu.verba.ui.theme.ThemeMode
import com.imu.verba.update.ReleaseInfo
import com.imu.verba.update.UpdateManager

/**
 * Obsidian-style settings screen with grouped sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSpeed: Float,
    currentVoice: Voice?,
    availableVoices: List<TtsVoice>,
    onSpeedChange: (Float) -> Unit,
    onVoiceChange: (Voice) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showVoiceSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Text-to-Speech section
            item {
                SettingsSectionHeader(title = "Text-to-Speech")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Voice",
                    subtitle = currentVoice?.name?.let { formatVoiceName(it) } ?: "System default",
                    onClick = { showVoiceSheet = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Speed,
                    title = "Speech Speed",
                    subtitle = "${currentSpeed}x",
                    onClick = { showSpeedSheet = true }
                )
            }

            item { SettingsDivider() }

            // Appearance section
            item {
                SettingsSectionHeader(title = "Appearance")
            }

            item {
                val currentTheme by ThemeManager.themeMode.collectAsState()
                
                ThemeToggleItem(
                    isDarkMode = currentTheme == ThemeMode.DARK,
                    onToggle = { ThemeManager.toggle() }
                )
            }

            item { SettingsDivider() }

            // About section
            item {
                SettingsSectionHeader(title = "About")
            }

            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Verba",
                    subtitle = "Version ${UpdateManager.getCurrentVersion()}",
                    showChevron = false,
                    onClick = { }
                )
            }
            
            item {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                var isChecking by remember { mutableStateOf(false) }
                var updateResult by remember { mutableStateOf<ReleaseInfo?>(null) }
                var showUpdateDialog by remember { mutableStateOf(false) }
                var updateError by remember { mutableStateOf<String?>(null) }
                
                UpdateCheckItem(
                    isChecking = isChecking,
                    onCheckClick = {
                        coroutineScope.launch {
                            isChecking = true
                            updateError = null
                            val result = UpdateManager.checkForUpdates()
                            isChecking = false
                            result.fold(
                                onSuccess = { release ->
                                    updateResult = release
                                    if (release != null) {
                                        showUpdateDialog = true
                                    } else {
                                        updateError = "You're up to date!"
                                    }
                                },
                                onFailure = { e ->
                                    updateError = "Failed to check: ${e.message}"
                                }
                            )
                        }
                    },
                    statusMessage = updateError
                )
                
                if (showUpdateDialog && updateResult != null) {
                    UpdateAvailableDialog(
                        releaseInfo = updateResult!!,
                        onDismiss = { showUpdateDialog = false },
                        onDownload = {
                            UpdateManager.downloadApk(context, updateResult!!)
                            showUpdateDialog = false
                        },
                        onViewRelease = {
                            UpdateManager.openReleasePage(context, updateResult!!)
                            showUpdateDialog = false
                        }
                    )
                }
            }
        }
    }

    // Voice selection bottom sheet
    if (showVoiceSheet) {
        VoiceSelectionSheet(
            voices = availableVoices,
            currentVoice = currentVoice,
            onVoiceSelect = { voice ->
                onVoiceChange(voice)
                showVoiceSheet = false
            },
            onDismiss = { showVoiceSheet = false }
        )
    }

    // Speed selection bottom sheet
    if (showSpeedSheet) {
        SpeedSelectionSheet(
            currentSpeed = currentSpeed,
            onSpeedSelect = { speed ->
                onSpeedChange(speed)
                showSpeedSheet = false
            },
            onDismiss = { showSpeedSheet = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun UpdateCheckItem(
    isChecking: Boolean,
    onCheckClick: () -> Unit,
    statusMessage: String?
) {
    Surface(
        onClick = { if (!isChecking) onCheckClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Check for Updates",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = statusMessage ?: "Tap to check for new versions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    releaseInfo: ReleaseInfo,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onViewRelease: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Available") },
        text = {
            Column {
                Text(
                    text = "Version ${releaseInfo.tagName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = releaseInfo.body.take(300) + if (releaseInfo.body.length > 300) "..." else "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text("Download")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
                TextButton(onClick = onViewRelease) {
                    Text("View Release")
                }
            }
        }
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun ThemeToggleItem(
    isDarkMode: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dark Mode",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (isDarkMode) "On" else "Off",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = isDarkMode,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceSelectionSheet(
    voices: List<TtsVoice>,
    currentVoice: Voice?,
    onVoiceSelect: (Voice) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Text(
                text = "Select Voice",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (voices.isEmpty()) {
                Text(
                    text = "No voices available. Check Android Settings → Text-to-Speech to download voices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn {
                    items(voices) { ttsVoice ->
                        val isSelected = ttsVoice.voice == currentVoice
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVoiceSelect(ttsVoice.voice) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ttsVoice.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = ttsVoice.languageDisplay,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSelectionSheet(
    currentSpeed: Float,
    onSpeedSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var sliderValue by remember { mutableFloatStateOf(currentSpeed) }
    
    val presetSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Speech Speed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Current speed display
            Text(
                text = "${String.format("%.2f", sliderValue)}x",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Slider
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onSpeedSelect(sliderValue) },
                valueRange = 0.5f..3.0f,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0.5x", style = MaterialTheme.typography.labelSmall)
                Text("3.0x", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preset speeds
            Text(
                text = "Presets",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetSpeeds.take(5).forEach { speed ->
                    SpeedPresetChip(
                        speed = speed,
                        isSelected = speed == sliderValue,
                        onClick = {
                            sliderValue = speed
                            onSpeedSelect(speed)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetSpeeds.drop(5).forEach { speed ->
                    SpeedPresetChip(
                        speed = speed,
                        isSelected = speed == sliderValue,
                        onClick = {
                            sliderValue = speed
                            onSpeedSelect(speed)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if less than 5 items
                repeat(5 - presetSpeeds.drop(5).size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SpeedPresetChip(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
               else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = "${speed}x",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatVoiceName(voiceName: String): String {
    return voiceName
        .substringAfterLast("-")
        .substringBefore("#")
        .replaceFirstChar { it.uppercase() }
}
