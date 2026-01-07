package com.imu.verba.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Theme mode options.
 * Light and Dark only - System follow removed for simplicity.
 */
enum class ThemeMode {
    LIGHT,
    DARK
}

/**
 * Manages the app's theme preference.
 * Persists to SharedPreferences.
 */
object ThemeManager {
    private const val PREFS_NAME = "verba_prefs"
    private const val KEY_THEME = "theme_mode"
    
    private val _themeMode = MutableStateFlow(ThemeMode.LIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    
    private var prefs: SharedPreferences? = null
    
    /**
     * Initialize the ThemeManager with a context.
     * Call this in Application.onCreate() or MainActivity.
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTheme = prefs?.getString(KEY_THEME, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
        _themeMode.value = ThemeMode.valueOf(savedTheme)
    }
    
    /**
     * Set the theme mode and persist it.
     */
    fun setTheme(mode: ThemeMode) {
        _themeMode.value = mode
        prefs?.edit()?.putString(KEY_THEME, mode.name)?.apply()
    }
    
    /**
     * Toggle between Light and Dark.
     */
    fun toggle() {
        val newMode = when (_themeMode.value) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
        }
        setTheme(newMode)
    }
    
    /**
     * Get current theme as boolean for Compose.
     */
    fun isDarkTheme(): Boolean = _themeMode.value == ThemeMode.DARK
    
    /**
     * Get display name for current theme.
     */
    fun getThemeDisplayName(): String = when (_themeMode.value) {
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
}
