package com.fubuki.manarabbit.network

import com.fubuki.manarabbit.data.HomeContent
import com.fubuki.manarabbit.data.Manga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

suspend fun fetchHomeContent(baseUrl: String, cookieStr: String = ""): HomeContent {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')

            fun buildRequest(url: String) = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()

            fun executeChecked(url: String): String {
                val response = httpClient.newCall(buildRequest(url)).execute()
                if (response.code == 403) { response.close(); throw AuthRequiredException() }
                return response.use { it.body?.string() ?: "" }
            }

            val updateDeferred = async { executeChecked("$cleanUrl/page/update") }
            val mainDeferred = async { executeChecked(cleanUrl) }

            val updateDoc = Jsoup.parse(updateDeferred.await())
            val updated = mutableListOf<Manga>()
            for (e in updateDoc.select("div.media.post-list").take(70)) {
                val seriesId = e.selectFirst("a.btn-primary")?.attr("rel")?.toIntOrNull() ?: continue
                val thumb = e.selectFirst("img")?.attr("src") ?: ""
                val name = e.selectFirst("div.post-subject a")?.ownText()?.trim() ?: continue
                updated.add(Manga(seriesId, name, thumb, cleanUrl))
            }

            val mainDoc = Jsoup.parse(mainDeferred.await())
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
        } catch (e: AuthRequiredException) {
            throw e
        } catch (e: Exception) {
            HomeContent()
        }
    }
}

suspend fun fetchUpdatedMangaList(baseUrl: String, cookieStr: String = ""): List<Manga> {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')
            val request = Request.Builder()
                .url("$cleanUrl/page/update")
                .header("User-Agent", USER_AGENT)
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.code == 403) { response.close(); throw AuthRequiredException() }
            val body = response.use { it.body?.string() } ?: return@withContext emptyList()

            val doc = Jsoup.parse(body)
            val result = mutableListOf<Manga>()
            for (e in doc.select("div.media.post-list")) {
                val seriesId = e.selectFirst("a.btn-primary")?.attr("rel")?.toIntOrNull() ?: continue
                val thumb = e.selectFirst("img")?.attr("src") ?: ""
                val name = e.selectFirst("div.post-subject a")?.ownText()?.trim() ?: continue
                result.add(Manga(seriesId, name, thumb, cleanUrl))
            }
            result
        } catch (e: AuthRequiredException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }
}
