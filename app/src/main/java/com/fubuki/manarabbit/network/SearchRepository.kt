package com.fubuki.manarabbit.network

import com.fubuki.manarabbit.data.Manga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

suspend fun searchManga(baseUrl: String, query: String, cookieStr: String = ""): List<Manga> {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$cleanUrl/comic?stx=$encodedQuery")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            response.close()

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
        } catch (e: Exception) {
            emptyList()
        }
    }
}