package com.fubuki.manarabbit.data

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
        val BOOKMARK_MANGA_KEY = stringPreferencesKey("bookmark_manga")
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

    val bookmarkManga: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[BOOKMARK_MANGA_KEY] ?: ""
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

    suspend fun saveRecentManga(item: RecentManga) {
        context.dataStore.edit { prefs ->
            val current = parseMangaList(prefs[RECENT_MANGA_KEY] ?: "")
            val updated = (listOf(item) + current.filter { it.mangaId != item.mangaId }).take(20)
            prefs[RECENT_MANGA_KEY] = serializeRecentMangaList(updated)
        }
    }

    suspend fun toggleBookmark(manga: Manga) {
        context.dataStore.edit { prefs ->
            val current = parseBookmarkList(prefs[BOOKMARK_MANGA_KEY] ?: "")
            val updated = if (current.any { it.id == manga.id }) {
                current.filter { it.id != manga.id }
            } else {
                listOf(manga) + current
            }
            prefs[BOOKMARK_MANGA_KEY] = serializeBookmarkList(updated)
        }
    }

    fun serializeBookmarkList(list: List<Manga>): String {
        return list.joinToString("|") { "${it.id}::${it.name}::${it.thumb}::${it.referer}" }
    }

    fun parseBookmarkList(str: String): List<Manga> {
        if (str.isEmpty()) return emptyList()
        return str.split("|").mapNotNull { item ->
            val parts = item.split("::")
            if (parts.size >= 4) {
                Manga(
                    id = parts[0].toIntOrNull() ?: return@mapNotNull null,
                    name = parts[1],
                    thumb = parts[2],
                    referer = parts[3]
                )
            } else null
        }
    }

    fun serializeRecentMangaList(list: List<RecentManga>): String {
        return list.joinToString("|") {
            "${it.mangaId}::${it.mangaName}::${it.thumb}::${it.referer}::${it.lastEpisodeId}::${it.lastEpisodeTitle}"
        }
    }

    fun parseMangaList(str: String): List<RecentManga> {
        if (str.isEmpty()) return emptyList()
        return str.split("|").mapNotNull { item ->
            val parts = item.split("::")
            if (parts.size >= 6) {
                RecentManga(
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

    fun isBookmarked(mangaId: Int, bookmarkStr: String): Boolean {
        return parseBookmarkList(bookmarkStr).any { it.id == mangaId }
    }
}