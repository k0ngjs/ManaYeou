package com.fubuki.manarabbit.network

import com.fubuki.manarabbit.data.HomeContent
import com.fubuki.manarabbit.data.Manga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

suspend fun fetchHomeContent(baseUrl: String, cookieStr: String = ""): HomeContent {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')
            val client = OkHttpClient()

            // 최신 만화
            val updateRequest = Request.Builder()
                .url("$cleanUrl/page/update")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val updateResponse = client.newCall(updateRequest).execute()
            val updateBody = updateResponse.body?.string() ?: ""
            updateResponse.close()

            val updateDoc = Jsoup.parse(updateBody)
            val updated = mutableListOf<Manga>()
            for (e in updateDoc.select("div.media.post-list").take(70)) {
                val seriesId = e.selectFirst("a.btn-primary")?.attr("rel")?.toIntOrNull() ?: continue
                val thumb = e.selectFirst("img")?.attr("src") ?: ""
                val name = e.selectFirst("div.post-subject a")?.ownText()?.trim() ?: continue
                updated.add(Manga(seriesId, name, thumb, cleanUrl))
            }

            // 인기 만화
            val mainRequest = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val mainResponse = client.newCall(mainRequest).execute()
            val mainBody = mainResponse.body?.string() ?: ""
            mainResponse.close()

            val mainDoc = Jsoup.parse(mainBody)
            val popular = mutableListOf<Manga>()
            val weeklySection = mainDoc.select("div.div-tab").firstOrNull { div ->
                div.selectFirst("a")?.text()?.contains("주간 베스트") == true
            }
            for (e in weeklySection?.select("ul.post-list li.post-row")?.take(20) ?: emptyList()) {
                val anchor = e.selectFirst("a") ?: continue
                val href = anchor.attr("href")
                val episodeId = href.split("/").lastOrNull()
                    ?.filter { it.isDigit() }?.toIntOrNull() ?: continue
                val name = anchor.ownText().trim()
                if (name.isEmpty()) continue
                popular.add(Manga(episodeId, name, "", cleanUrl, isEpisode = true))
            }

            HomeContent(updated, popular)
        } catch (e: Exception) {
            HomeContent()
        }
    }
}

suspend fun fetchUpdatedMangaList(baseUrl: String, cookieStr: String = ""): List<Manga> {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$cleanUrl/page/update")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            response.close()

            val doc = Jsoup.parse(body)
            val result = mutableListOf<Manga>()
            for (e in doc.select("div.media.post-list")) {
                val seriesId = e.selectFirst("a.btn-primary")?.attr("rel")?.toIntOrNull() ?: continue
                val thumb = e.selectFirst("img")?.attr("src") ?: ""
                val name = e.selectFirst("div.post-subject a")?.ownText()?.trim() ?: continue
                result.add(Manga(seriesId, name, thumb, cleanUrl))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}