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
        val AUTO_RESOLVE_KEY = stringPreferencesKey("auto_resolve")
        val AUTO_RESOLVE_NUMBER_KEY = stringPreferencesKey("auto_resolve_number")
        val HOME_CONTENT_KEY = stringPreferencesKey("home_content")
        val EPISODE_CACHE_KEY = stringPreferencesKey("episode_cache")
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

    val autoResolve: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_RESOLVE_KEY]?.toBoolean() ?: false
    }

    val autoResolveNumber: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[AUTO_RESOLVE_NUMBER_KEY]?.toIntOrNull() ?: 0
    }

    val homeContent: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[HOME_CONTENT_KEY] ?: ""
    }

    val episodeCache: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[EPISODE_CACHE_KEY] ?: ""
    }

    suspend fun saveAutoResolve(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[AUTO_RESOLVE_KEY] = enabled.toString() }
    }

    suspend fun saveAutoResolveNumber(number: Int) {
        context.dataStore.edit { prefs -> prefs[AUTO_RESOLVE_NUMBER_KEY] = number.toString() }
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
            val current = parseRecentMangaList(prefs[RECENT_MANGA_KEY] ?: "")
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

    suspend fun updateBookmarkThumb(manga: Manga) {
        context.dataStore.edit { prefs ->
            val current = parseBookmarkList(prefs[BOOKMARK_MANGA_KEY] ?: "")
            val updated = current.map { if (it.id == manga.id) manga else it }
            prefs[BOOKMARK_MANGA_KEY] = serializeBookmarkList(updated)
        }
    }

    suspend fun saveRecentMangaPage(mangaId: Int, episodeId: Int, page: Int) {
        context.dataStore.edit { prefs ->
            val current = parseRecentMangaList(prefs[RECENT_MANGA_KEY] ?: "")
            val updated = current.map {
                if (it.mangaId == mangaId && it.lastEpisodeId == episodeId) it.copy(lastPage = page)
                else it
            }
            prefs[RECENT_MANGA_KEY] = serializeRecentMangaList(updated)
        }
    }

    suspend fun saveRecentMangaList(list: List<RecentManga>) {
        context.dataStore.edit { prefs ->
            prefs[RECENT_MANGA_KEY] = serializeRecentMangaList(list)
        }
    }

    suspend fun saveBookmarkList(list: List<Manga>) {
        context.dataStore.edit { prefs ->
            prefs[BOOKMARK_MANGA_KEY] = serializeBookmarkList(list)
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
            "${it.mangaId}::${it.mangaName}::${it.thumb}::${it.referer}::${it.lastEpisodeId}::${it.lastEpisodeTitle}::${it.lastPage}"
        }
    }

    fun parseRecentMangaList(str: String): List<RecentManga> {
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
                    lastEpisodeTitle = parts[5],
                    lastPage = parts.getOrNull(6)?.toIntOrNull() ?: 0
                )
            } else null
        }
    }

    fun isBookmarked(mangaId: Int, bookmarkStr: String): Boolean {
        return parseBookmarkList(bookmarkStr).any { it.id == mangaId }
    }

    // 홈 콘텐츠 캐시 (updated|popular 구분)
    suspend fun saveHomeContent(content: HomeContent) {
        val updated = serializeBookmarkList(content.updated)
        val popular = serializeBookmarkList(content.popular)
        context.dataStore.edit { prefs -> prefs[HOME_CONTENT_KEY] = "$updated\n$popular" }
    }

    fun parseHomeContent(str: String): HomeContent {
        if (str.isEmpty()) return HomeContent()
        val parts = str.split("\n", limit = 2)
        val updated = parseBookmarkList(parts.getOrNull(0) ?: "")
        val popular = parseBookmarkList(parts.getOrNull(1) ?: "").map { it.copy(isEpisode = true) }
        return HomeContent(updated, popular)
    }

    // 에피소드 목록 캐시 (mangaId별 최근 1개)
    suspend fun saveEpisodeCache(mangaId: Int, episodes: List<Episode>) {
        val serialized = episodes.joinToString("|") { "${it.id}::${it.title}::${it.date}" }
        context.dataStore.edit { prefs -> prefs[EPISODE_CACHE_KEY] = "$mangaId\n$serialized" }
    }

    fun parseEpisodeCache(str: String, mangaId: Int): List<Episode> {
        if (str.isEmpty()) return emptyList()
        val parts = str.split("\n", limit = 2)
        if (parts.getOrNull(0)?.toIntOrNull() != mangaId) return emptyList()
        val episodeStr = parts.getOrNull(1) ?: return emptyList()
        if (episodeStr.isEmpty()) return emptyList()
        return episodeStr.split("|").mapNotNull { item ->
            val p = item.split("::")
            if (p.size >= 3) Episode(p[0].toIntOrNull() ?: return@mapNotNull null, p[1], p[2]) else null
        }
    }
}