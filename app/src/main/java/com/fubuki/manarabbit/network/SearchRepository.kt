package com.fubuki.manarabbit.network

import com.fubuki.manarabbit.data.Manga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

suspend fun searchManga(baseUrl: String, query: String, cookieStr: String = ""): List<Manga> {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("$cleanUrl/comic?stx=$encodedQuery")
                .header("User-Agent", USER_AGENT)
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.code == 403) { response.close(); throw AuthRequiredException() }
            val body = response.use { it.body?.string() } ?: return@withContext emptyList()

            val doc = Jsoup.parse(body)
            val result = mutableListOf<Manga>()

            for (e in doc.select("div.imgframe")) {
                val anchor = e.selectFirst("div.in-lable a") ?: continue
                val href = anchor.attr("href")
                val seriesId = href.split("comic/").getOrNull(1)
                    ?.split("?")?.firstOrNull()
                    ?.filter { it.isDigit() }?.toIntOrNull() ?: continue
                val name = e.selectFirst("span.title")?.text()?.trim() ?: continue
                val thumb = e.selectFirst("img")?.attr("src") ?: ""
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
