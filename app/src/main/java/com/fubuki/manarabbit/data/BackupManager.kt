package com.fubuki.manarabbit.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.fubuki.manarabbit.network.fetchMangaDetail

object BackupManager {

    fun generateFileName(): String {
        val dateFormat = SimpleDateFormat("yyMMddHHmm", Locale.getDefault())
        return "mana_${dateFormat.format(Date())}.yeou"
    }

    suspend fun exportBackup(store: SettingsDataStore): String {
        val recentList = store.parseRecentMangaList(store.recentManga.first())
        val bookmarkList = store.parseBookmarkList(store.bookmarkManga.first())

        val recentArray = JSONArray()
        for (item in recentList) {
            val arr = JSONArray()
            arr.put(item.mangaId)
            arr.put(item.lastEpisodeId)
            recentArray.put(arr)
        }

        val bookmarkArray = JSONArray()
        for (item in bookmarkList) {
            bookmarkArray.put(item.id)
        }

        val json = JSONObject()
        json.put("r", recentArray)
        json.put("b", bookmarkArray)

        return json.toString()
    }

    suspend fun importBackup(context: Context, uri: Uri, store: SettingsDataStore, baseUrl: String, cfCookies: String, onComplete: () -> Unit) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return

            val json = JSONObject(content)
            val cleanUrl = baseUrl.trimEnd('/')

            val bookmarkArray = json.optJSONArray("b") ?: JSONArray()
            val bookmarkList = mutableListOf<Manga>()
            for (i in 0 until bookmarkArray.length()) {
                val mangaId = bookmarkArray.getInt(i)
                try {
                    val detail = fetchMangaDetail(cleanUrl, mangaId, cfCookies)
                    if (detail.info.name.isNotEmpty()) {
                        bookmarkList.add(Manga(mangaId, detail.info.name, detail.info.thumb, cleanUrl))
                    } else {
                        bookmarkList.add(Manga(mangaId, "", "", cleanUrl))
                    }
                } catch (e: Exception) {
                    bookmarkList.add(Manga(mangaId, "", "", cleanUrl))
                }
            }
            val existingBookmark = store.parseBookmarkList(store.bookmarkManga.first())
            val mergedBookmark = (bookmarkList + existingBookmark).distinctBy { it.id }
            store.saveBookmarkList(mergedBookmark)

            val recentArray = json.optJSONArray("r") ?: JSONArray()
            val recentList = mutableListOf<RecentManga>()
            for (i in 0 until recentArray.length()) {
                val arr = recentArray.getJSONArray(i)
                val mangaId = arr.getInt(0)
                val lastEpisodeId = arr.getInt(1)
                try {
                    val detail = fetchMangaDetail(cleanUrl, mangaId, cfCookies)
                    val episode = detail.episodes.find { it.id == lastEpisodeId }
                    recentList.add(
                        RecentManga(
                            mangaId = mangaId,
                            mangaName = detail.info.name,
                            thumb = detail.info.thumb,
                            referer = cleanUrl,
                            lastEpisodeId = lastEpisodeId,
                            lastEpisodeTitle = episode?.title ?: ""
                        )
                    )
                } catch (e: Exception) {
                    recentList.add(RecentManga(mangaId, "", "", cleanUrl, lastEpisodeId, ""))
                }
            }
            val existingRecent = store.parseRecentMangaList(store.recentManga.first())
            val mergedRecent = (recentList + existingRecent).distinctBy { it.mangaId }.take(20)
            store.saveRecentMangaList(mergedRecent)

            onComplete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}