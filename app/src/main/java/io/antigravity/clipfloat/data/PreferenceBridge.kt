package io.antigravity.clipfloat.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import io.antigravity.clipfloat.design.theme.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore by preferencesDataStore(name = "clipfloat_prefs")

class PreferenceBridge(private val context: Context) {
    private object Keys {
        val SLOT_COUNT = intPreferencesKey("slot_count")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val HUD_OPACITY = floatPreferencesKey("hud_opacity")
        val AUTO_TRIM = booleanPreferencesKey("auto_trim")
        val STRIP_TRACKERS = booleanPreferencesKey("strip_trackers")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
    }

    val configStream: Flow<EngineConfig> = context.dataStore.data
        .catch { ex ->
            if (ex is IOException) emit(emptyPreferences()) else throw ex
        }
        .map { prefs ->
            EngineConfig(
                slotCount = prefs[Keys.SLOT_COUNT] ?: 4,
                theme = runCatching { 
                    ThemePreset.valueOf(prefs[Keys.THEME_PRESET] ?: ThemePreset.CYBER_CHARCOAL.name) 
                }.getOrDefault(ThemePreset.CYBER_CHARCOAL),
                hudOpacity = prefs[Keys.HUD_OPACITY] ?: 0.88f,
                autoTrim = prefs[Keys.AUTO_TRIM] ?: true,
                stripTrackers = prefs[Keys.STRIP_TRACKERS] ?: true,
                hapticsEnabled = prefs[Keys.HAPTICS] ?: true
            )
        }

    suspend fun setSlotCount(count: Int) {
        context.dataStore.edit { it[Keys.SLOT_COUNT] = count.coerceIn(2, 8) }
    }

    suspend fun setTheme(theme: ThemePreset) {
        context.dataStore.edit { it[Keys.THEME_PRESET] = theme.name }
    }

    suspend fun setOpacity(opacity: Float) {
        context.dataStore.edit { it[Keys.HUD_OPACITY] = opacity.coerceIn(0.40f, 1.0f) }
    }

    suspend fun setAutoTrim(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_TRIM] = enabled }
    }

    suspend fun setStripTrackers(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STRIP_TRACKERS] = enabled }
    }
}
