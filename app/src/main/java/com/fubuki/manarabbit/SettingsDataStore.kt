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
        val VIEWER_DIRECTION_KEY = stringPreferencesKey("viewer_direction")
        val RECENT_MANGA_KEY = stringPreferencesKey("recent_manga")
        val RECENT_MANGA_V2_KEY = stringPreferencesKey("recent_manga_v2")
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

    val recentManga: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[RECENT_MANGA_KEY] ?: ""
    }

    val recentMangaV2: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[RECENT_MANGA_V2_KEY] ?: ""
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

    suspend fun saveViewerDirection(value: String) {
        context.dataStore.edit { prefs -> prefs[VIEWER_DIRECTION_KEY] = value }
    }

    suspend fun saveRecentManga(item: MangaItem) {
        context.dataStore.edit { prefs ->
            val current = parseMangaList(prefs[RECENT_MANGA_KEY] ?: "")
            val updated = (listOf(item) + current.filter { it.id != item.id }).take(20)
            prefs[RECENT_MANGA_KEY] = serializeMangaList(updated)
        }
    }

    suspend fun saveRecentMangaV2(item: RecentMangaItem) {
        context.dataStore.edit { prefs ->
            val current = parseRecentMangaList(prefs[RECENT_MANGA_V2_KEY] ?: "")
            val updated = (listOf(item) + current.filter { it.mangaId != item.mangaId }).take(20)
            prefs[RECENT_MANGA_V2_KEY] = serializeRecentMangaList(updated)
        }
    }

    fun serializeMangaList(list: List<MangaItem>): String {
        return list.joinToString("|") { "${it.id},${it.name},${it.thumb},${it.referer}" }
    }

    fun parseMangaList(str: String): List<MangaItem> {
        if (str.isEmpty()) return emptyList()
        return str.split("|").mapNotNull { item ->
            val parts = item.split(",")
            if (parts.size >= 4) {
                MangaItem(
                    id = parts[0].toIntOrNull() ?: return@mapNotNull null,
                    name = parts[1],
                    thumb = parts[2],
                    referer = parts[3]
                )
            } else null
        }
    }

    fun parseCookies(cookieStr: String): Map<String, String> {
        if (cookieStr.isEmpty()) return emptyMap()
        return cookieStr.split(";").mapNotNull { s ->
            val idx = s.indexOf("=")
            if (idx > 0) s.substring(0, idx).trim() to s.substring(idx + 1).trim()
            else null
        }.toMap()
    }

    fun serializeRecentMangaList(list: List<RecentMangaItem>): String {
        return list.joinToString("|") {
            "${it.mangaId}::${it.mangaName}::${it.thumb}::${it.referer}::${it.lastEpisodeId}::${it.lastEpisodeTitle}"
        }
    }

    fun parseRecentMangaList(str: String): List<RecentMangaItem> {
        if (str.isEmpty()) return emptyList()
        return str.split("|").mapNotNull { item ->
            val parts = item.split("::")
            if (parts.size >= 6) {
                RecentMangaItem(
                    mangaId = parts[0].toIntOrNull() ?: return@mapNotNull null,
                    mangaName = parts[1],
                    thumb = parts[2],
                    referer = parts[3],
                    lastEpisodeId = parts[4].toIntOrNull() ?: return@mapNotNull null,
                    lastEpisodeTitle = parts[5]
                )
            } else null
        }
    }
}