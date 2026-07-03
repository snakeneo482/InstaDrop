package com.instadrop.app.data.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How the app picks light vs. dark colours. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Tiny persisted-settings store backed by [android.content.SharedPreferences],
 * exposed reactively as [StateFlow]s so Compose recomposes when a setting changes.
 * No DI framework or DataStore dependency needed for a handful of flags.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("instadrop_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM),
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, false))
    /** Use the device wallpaper palette (Material You) on Android 12+ instead of the brand colours. */
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _backendUrl = MutableStateFlow(prefs.getString(KEY_BACKEND, "") ?: "")
    /** Base URL of the self-hosted Cobalt instance used for non-Instagram sites (YouTube, TikTok, …). */
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    private val _apiKey = MutableStateFlow(prefs.getString(KEY_APIKEY, "") ?: "")
    /** Optional API key if your Cobalt instance requires one. */
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME, mode.name) }
        _themeMode.value = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DYNAMIC, enabled) }
        _dynamicColor.value = enabled
    }

    fun setBackendUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit { putString(KEY_BACKEND, trimmed) }
        _backendUrl.value = trimmed
    }

    fun setApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit { putString(KEY_APIKEY, trimmed) }
        _apiKey.value = trimmed
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_BACKEND = "backend_url"
        const val KEY_APIKEY = "api_key"
    }
}
