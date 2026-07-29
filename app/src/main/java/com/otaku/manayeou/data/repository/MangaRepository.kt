package com.otaku.manayeou.data.repository

import android.content.Context
import android.util.Log
import com.otaku.manayeou.data.local.AppDatabase
import com.otaku.manayeou.data.local.SettingsRepository
import com.otaku.manayeou.data.local.UrlMode
import com.otaku.manayeou.data.local.entity.BookmarkEntity
import com.otaku.manayeou.data.local.entity.ReadHistoryEntity
import com.otaku.manayeou.data.model.Chapter
import com.otaku.manayeou.data.model.MangaPage
import com.otaku.manayeou.data.model.Series
import com.otaku.manayeou.data.remote.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock

class MangaRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val bookmarkDao = db.bookmarkDao()
    private val historyDao = db.readHistoryDao()
    private val settingsRepo = SettingsRepository(context)

    companion object {
        // 텔레그램 채널에서 알아낸 kmana 도메인은 세션 동안 거의 바뀌지 않으므로
        // 화면마다 새로 생성되는 MangaRepository 인스턴스들이 공유하도록 companion에 캐싱
        @Volatile private var cachedAutoBaseUrl: String? = null
        private val baseUrlMutex = kotlinx.coroutines.sync.Mutex()
    }

    // ── 원격 ──────────────────────────────────────────

    private suspend fun resolveBaseUrl(): String {
        if (settingsRepo.getUrlMode() == UrlMode.MANUAL) {
            val manual = settingsRepo.getManualBaseUrl().trim()
            if (manual.isNotBlank()) return manual.trimEnd('/')
        }
        cachedAutoBaseUrl?.let { return it }
        return baseUrlMutex.withLock {
            cachedAutoBaseUrl?.let { return@withLock it }
            val urls = fetchChannelUrls()
            Log.d("MangaRepo", "Telegram URLs: $urls")
            // 텔레그램에서 가져온 kmana 도메인 중 첫 번째 사용; 없으면 기본값
            val resolved = urls.firstOrNull()?.trimEnd('/') ?: "https://kmana10.net"
            cachedAutoBaseUrl = resolved
            resolved
        }
    }

    suspend fun fetchLatest(): List<Series> {
        val base = resolveBaseUrl()
        return scrapeLatest(base).also { Log.d("MangaRepo", "fetchLatest: ${it.size} items") }
    }

    suspend fun fetchPopular(): List<Series> {
        val base = resolveBaseUrl()
        return scrapePopular(base).also { Log.d("MangaRepo", "fetchPopular: ${it.size} items") }
    }

    suspend fun fetchSeries(url: String): Series? = scrapeSeries(url)

    suspend fun fetchChapters(seriesUrl: String): List<Chapter> = scrapeChapters(seriesUrl)

    suspend fun fetchPages(chapterUrl: String): List<MangaPage> = scrapePages(chapterUrl)

    suspend fun search(keyword: String): List<Series> {
        val base = resolveBaseUrl()
        return searchSeries(base, keyword)
    }

    // ── 북마크 ─────────────────────────────────────────

    fun getBookmarks(): Flow<List<Series>> =
        bookmarkDao.getAll().map { list ->
            list.map { e ->
                Series(
                    id = e.seriesId,
                    title = e.title,
                    author = e.author,
                    coverUrl = e.coverUrl,
                    sourceUrl = e.sourceUrl
                )
            }
        }

    fun isBookmarked(seriesId: String): Flow<Boolean> =
        bookmarkDao.isBookmarked(seriesId)

    suspend fun addBookmark(series: Series) {
        bookmarkDao.insert(
            BookmarkEntity(
                seriesId = series.id,
                title = series.title,
                author = series.author,
                coverUrl = series.coverUrl,
                sourceUrl = series.sourceUrl
            )
        )
    }

    suspend fun removeBookmark(seriesId: String) {
        bookmarkDao.delete(seriesId)
    }

    // ── 읽기 기록 ───────────────────────────────────────

    fun getRecentHistory(limit: Int = 20): Flow<List<Series>> =
        historyDao.getRecent(limit).map { list ->
            list.distinctBy { it.seriesId }.map { e ->
                Series(
                    id = e.seriesId,
                    title = e.seriesTitle,
                    coverUrl = e.coverUrl,
                    // 예전 기록엔 seriesUrl이 없을 수 있어(마이그레이션 이전 데이터),
                    // 그 경우에만 KmanaScraper와 동일한 "/episode/{id}/1/1" 패턴으로 복원
                    sourceUrl = e.seriesUrl.ifBlank { "${resolveBaseUrl()}/episode/${e.seriesId}/1/1" },
                    latestChapter = e.chapterTitle
                )
            }
        }

    fun getLastReadChapterUrl(seriesId: String): Flow<String?> =
        historyDao.getLatestForSeries(seriesId).map { it?.chapterUrl }

    suspend fun removeHistory(seriesId: String) {
        historyDao.deleteBySeriesId(seriesId)
    }

    suspend fun recordHistory(
        chapterUrl: String,
        chapterTitle: String,
        seriesId: String,
        seriesUrl: String,
        seriesTitle: String,
        coverUrl: String,
        lastPage: Int,
        totalPages: Int
    ) {
        historyDao.insert(
            ReadHistoryEntity(
                chapterUrl = chapterUrl,
                seriesId = seriesId,
                seriesUrl = seriesUrl,
                seriesTitle = seriesTitle,
                coverUrl = coverUrl,
                chapterTitle = chapterTitle,
                lastPage = lastPage,
                totalPages = totalPages
            )
        )
    }

    suspend fun clearAllData() {
        bookmarkDao.clearAll()
        historyDao.clearAll()
    }

    // ── 백업/복원 ───────────────────────────────────────

    suspend fun exportBackupJson(): String {
        val bookmarks = bookmarkDao.getAll().first()
        val history = historyDao.getAll().first()

        val bookmarksArr = org.json.JSONArray()
        bookmarks.forEach { b ->
            bookmarksArr.put(
                org.json.JSONObject().apply {
                    put("seriesId", b.seriesId)
                    put("title", b.title)
                    put("author", b.author)
                    put("coverUrl", b.coverUrl)
                    put("sourceUrl", b.sourceUrl)
                    put("addedAt", b.addedAt)
                }
            )
        }
        val historyArr = org.json.JSONArray()
        history.forEach { h ->
            historyArr.put(
                org.json.JSONObject().apply {
                    put("chapterUrl", h.chapterUrl)
                    put("seriesId", h.seriesId)
                    put("seriesUrl", h.seriesUrl)
                    put("seriesTitle", h.seriesTitle)
                    put("coverUrl", h.coverUrl)
                    put("chapterTitle", h.chapterTitle)
                    put("lastPage", h.lastPage)
                    put("totalPages", h.totalPages)
                    put("readAt", h.readAt)
                }
            )
        }
        return org.json.JSONObject().apply {
            put("bookmarks", bookmarksArr)
            put("history", historyArr)
        }.toString(2)
    }

    suspend fun importBackupJson(json: String) {
        val root = org.json.JSONObject(json)
        root.optJSONArray("bookmarks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                bookmarkDao.insert(
                    BookmarkEntity(
                        seriesId = o.getString("seriesId"),
                        title = o.optString("title"),
                        author = o.optString("author"),
                        coverUrl = o.optString("coverUrl"),
                        sourceUrl = o.optString("sourceUrl"),
                        addedAt = o.optLong("addedAt", System.currentTimeMillis())
                    )
                )
            }
        }
        root.optJSONArray("history")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                historyDao.insert(
                    ReadHistoryEntity(
                        chapterUrl = o.getString("chapterUrl"),
                        seriesId = o.optString("seriesId"),
                        seriesUrl = o.optString("seriesUrl"),
                        seriesTitle = o.optString("seriesTitle"),
                        coverUrl = o.optString("coverUrl"),
                        chapterTitle = o.optString("chapterTitle"),
                        lastPage = o.optInt("lastPage", 0),
                        totalPages = o.optInt("totalPages", 0),
                        readAt = o.optLong("readAt", System.currentTimeMillis())
                    )
                )
            }
        }
    }
}
