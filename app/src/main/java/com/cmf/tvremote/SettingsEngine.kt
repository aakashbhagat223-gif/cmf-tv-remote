package com.cmf.tvremote

import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.settingsDataStore by preferencesDataStore(name = "remote_ui_settings")

enum class WallpaperTheme(val displayName: String) {
    WARM_AURORA("Warm Aurora Gradient"),
    DEEP_SLATE("Deep Slate Minimalist"),
    OLED_BLACK("OLED Pitch Black")
}

data class RemoteUiSettings(
    val fontScale: Float = 1.0f,
    val fontWeightIndex: Int = 2, // 0: Thin, 1: Light, 2: Normal, 3: Medium, 4: Bold
    val squircleRadiusDp: Float = 26f,
    val wallpaperTheme: WallpaperTheme = WallpaperTheme.WARM_AURORA
) {
    val fontWeight: FontWeight
        get() = when (fontWeightIndex) {
            0 -> FontWeight.Thin
            1 -> FontWeight.Light
            2 -> FontWeight.Normal
            3 -> FontWeight.Medium
            4 -> FontWeight.Bold
            else -> FontWeight.Normal
        }

    val cornerRadius: Dp
        get() = squircleRadiusDp.dp
}

class SettingsEngine(private val context: Context) {

    private object PreferencesKeys {
        val FONT_SCALE = floatPreferencesKey("pref_font_scale")
        val FONT_WEIGHT_INDEX = intPreferencesKey("pref_font_weight_idx")
        val SQUIRCLE_RADIUS = floatPreferencesKey("pref_squircle_radius")
        val WALLPAPER_THEME = stringPreferencesKey("pref_wallpaper_theme")
    }

    val settingsFlow: Flow<RemoteUiSettings> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs: Preferences ->
            val scale = prefs[PreferencesKeys.FONT_SCALE] ?: 1.0f
            val weightIdx = prefs[PreferencesKeys.FONT_WEIGHT_INDEX] ?: 2
            val radius = prefs[PreferencesKeys.SQUIRCLE_RADIUS] ?: 26f
            val themeName = prefs[PreferencesKeys.WALLPAPER_THEME] ?: WallpaperTheme.WARM_AURORA.name
            val theme = runCatching { WallpaperTheme.valueOf(themeName) }.getOrDefault(WallpaperTheme.WARM_AURORA)

            RemoteUiSettings(
                fontScale = scale.coerceIn(0.8f, 1.3f),
                fontWeightIndex = weightIdx.coerceIn(0, 4),
                squircleRadiusDp = radius.coerceIn(8f, 34f),
                wallpaperTheme = theme
            )
        }

    suspend fun updateFontScale(scale: Float) {
        context.settingsDataStore.edit { it[PreferencesKeys.FONT_SCALE] = scale }
    }

    suspend fun updateFontWeight(weightIndex: Int) {
        context.settingsDataStore.edit { it[PreferencesKeys.FONT_WEIGHT_INDEX] = weightIndex }
    }

    suspend fun updateSquircleRadius(radiusDp: Float) {
        context.settingsDataStore.edit { it[PreferencesKeys.SQUIRCLE_RADIUS] = radiusDp }
    }

    suspend fun updateWallpaperTheme(theme: WallpaperTheme) {
        context.settingsDataStore.edit { it[PreferencesKeys.WALLPAPER_THEME] = theme.name }
    }
}
