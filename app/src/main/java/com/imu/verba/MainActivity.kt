package com.imu.verba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imu.verba.storage.FileAccessHelper
import com.imu.verba.ui.reader.DisplayMode
import com.imu.verba.ui.reader.ReaderScreen
import com.imu.verba.ui.settings.SettingsScreen
import com.imu.verba.ui.theme.ThemeManager
import com.imu.verba.ui.theme.ThemeMode
import com.imu.verba.ui.theme.VerbaTheme
import com.imu.verba.viewmodel.ReaderViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize theme manager
        ThemeManager.init(applicationContext)
        
        setContent {
            // Observe theme changes
            val themeMode by ThemeManager.themeMode.collectAsState()
            
            VerbaTheme(darkTheme = themeMode == ThemeMode.DARK) {
                VerbaApp(applicationContext = applicationContext)
            }
        }
    }
}

/**
 * Simple navigation state for the app.
 */
enum class Screen {
    READER,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerbaApp(
    applicationContext: android.content.Context,
    viewModel: ReaderViewModel = viewModel(
        factory = ReaderViewModel.Factory(applicationContext)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf(Screen.READER) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.loadDocument(it) }
    }

    when (currentScreen) {
        Screen.READER -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = uiState.document?.name ?: "Verba",
                                maxLines = 1
                            )
                        },
                        actions = {
                            // Mode toggle (Read/Edit) - only show when document is loaded
                            if (uiState.document != null) {
                                IconButton(
                                    onClick = { viewModel.toggleDisplayMode() }
                                ) {
                                    Icon(
                                        imageVector = when (uiState.displayMode) {
                                            DisplayMode.READ -> Icons.Default.Edit
                                            DisplayMode.EDIT -> Icons.Default.MenuBook
                                        },
                                        contentDescription = when (uiState.displayMode) {
                                            DisplayMode.READ -> "Switch to Edit mode"
                                            DisplayMode.EDIT -> "Switch to Read mode"
                                        }
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = {
                                    filePickerLauncher.launch(FileAccessHelper.MimeTypes.MARKDOWN_TYPES)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Open file"
                                )
                            }
                            IconButton(
                                onClick = { currentScreen = Screen.SETTINGS }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                ReaderScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }

        Screen.SETTINGS -> {
            SettingsScreen(
                currentSpeed = uiState.speechRate,
                currentVoice = uiState.currentVoice,
                availableVoices = uiState.availableVoices,
                onSpeedChange = { speed -> viewModel.setSpeechRate(speed) },
                onVoiceChange = { voice -> viewModel.setVoice(voice) },
                onBackClick = { currentScreen = Screen.READER },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
