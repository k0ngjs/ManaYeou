package com.fubuki.manarabbit

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val BASE_URL_KEY = stringPreferencesKey("base_url")
        val CF_COOKIES_KEY = stringPreferencesKey("cf_cookies")
        val THEME_KEY = stringPreferencesKey("theme")
        val VIEWER_MODE_KEY = stringPreferencesKey("viewer_mode")
        val VIEWER_DOUBLE_KEY = stringPreferencesKey("viewer_double")
        val VIEWER_DOUBLE_FIRST_KEY = stringPreferencesKey("viewer_double_first")
        val VIEWER_DIRECTION_KEY = stringPreferencesKey("viewer_direction") // ltr, rtl
    }

    val baseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[BASE_URL_KEY] ?: ""
    }

    val cfCookies: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[CF_COOKIES_KEY] ?: ""
    }

    val theme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_KEY] ?: "system"
    }

    val viewerMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[VIEWER_MODE_KEY] ?: "scroll"
    }

    val viewerDouble: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[VIEWER_DOUBLE_KEY]?.toBoolean() ?: false
    }

    val viewerDoubleFirst: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[VIEWER_DOUBLE_FIRST_KEY] ?: "single"
    }

    val viewerDirection: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[VIEWER_DIRECTION_KEY] ?: "ltr"
    }

    suspend fun saveViewerDirection(value: String) {
        context.dataStore.edit { prefs -> prefs[VIEWER_DIRECTION_KEY] = value }
    }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { prefs -> prefs[BASE_URL_KEY] = url }
    }

    suspend fun saveCfCookies(cookies: Map<String, String>) {
        val cookieStr = cookies.entries.joinToString(";") { "${it.key}=${it.value}" }
        context.dataStore.edit { prefs -> prefs[CF_COOKIES_KEY] = cookieStr }
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { prefs -> prefs[THEME_KEY] = theme }
    }

    suspend fun saveViewerMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[VIEWER_MODE_KEY] = mode }
    }

    suspend fun saveViewerDouble(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[VIEWER_DOUBLE_KEY] = value.toString() }
    }

    suspend fun saveViewerDoubleFirst(value: String) {
        context.dataStore.edit { prefs -> prefs[VIEWER_DOUBLE_FIRST_KEY] = value }
    }

    fun parseCookies(cookieStr: String): Map<String, String> {
        if (cookieStr.isEmpty()) return emptyMap()
        return cookieStr.split(";").mapNotNull { s ->
            val idx = s.indexOf("=")
            if (idx > 0) s.substring(0, idx).trim() to s.substring(idx + 1).trim()
            else null
        }.toMap()
    }
}