package com.fubuki.manarabbit.network

import com.fubuki.manarabbit.data.Episode
import com.fubuki.manarabbit.data.MangaDetail
import com.fubuki.manarabbit.data.MangaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

suspend fun fetchMangaDetail(
    baseUrl: String,
    mangaId: Int,
    cookieStr: String = ""
): MangaDetail {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$cleanUrl/comic/$mangaId")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext MangaDetail()
            response.close()

            val doc = Jsoup.parse(body)
            val header = doc.selectFirst("div.view-title")
            val name = header?.selectFirst("div.view-content b")?.ownText() ?: ""
            val thumb = header?.selectFirst("div.view-img img")?.attr("src") ?: ""
            var author = ""
            var release = ""
            val tags = mutableListOf<String>()

            header?.select("div.view-content")?.forEach { el ->
                when (el.selectFirst("strong")?.ownText()) {
                    "작가" -> author = el.selectFirst("a")?.ownText() ?: ""
                    "발행구분" -> release = el.selectFirst("a")?.ownText() ?: ""
                    "분류" -> el.select("a").forEach { tags.add(it.ownText()) }
                }
            }

            val info = MangaInfo(name, thumb, author, tags, release)
            val episodes = mutableListOf<Episode>()
            val listBody = doc.selectFirst("ul.list-body") ?: return@withContext MangaDetail(info)

            for (e in listBody.select("li.list-item")) {
                val anchor = e.selectFirst("a.item-subject") ?: continue
                val href = anchor.attr("href")
                val id = href.split("comic/").getOrNull(1)
                    ?.split("?")?.firstOrNull()
                    ?.filter { it.isDigit() }?.toIntOrNull() ?: continue
                val title = anchor.ownText().trim()
                val date = e.selectFirst("div.wr-date")?.ownText()?.trim()
                    ?: e.selectFirst("div.item-details span")?.text()?.trim() ?: ""
                episodes.add(Episode(id, title, date))
            }

            MangaDetail(info, episodes)
        } catch (e: Exception) {
            MangaDetail()
        }
    }
}

suspend fun fetchEpisodeList(baseUrl: String, mangaId: Int, cookieStr: String = ""): List<Episode> {
    return fetchMangaDetail(baseUrl, mangaId, cookieStr).episodes
}