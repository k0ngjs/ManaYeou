package com.fubuki.manarabbit.network

import com.fubuki.manarabbit.data.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder

suspend fun fetchViewerImages(baseUrl: String, episodeId: Int, cookieStr: String = ""): List<String> {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')
            val request = Request.Builder()
                .url("$cleanUrl/comic/$episodeId")
                .header("User-Agent", USER_AGENT)
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val body = httpClient.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext emptyList()

            val doc = Jsoup.parse(body)
            val result = mutableListOf<String>()

            val viewPaddings = doc.select("div.view-padding")
            if (viewPaddings.size < 2) return@withContext emptyList()

            val script = viewPaddings[1].selectFirst("script")?.data()
                ?: return@withContext emptyList()

            val encodedData = StringBuilder("%")
            for (line in script.split("\n")) {
                if (line.contains("html_data+=")) {
                    val start = line.indexOf('\'') + 1
                    val end = line.lastIndexOf('\'')
                    if (start < end) {
                        encodedData.append(line.substring(start, end).replace(".", "%"))
                    }
                }
            }
            if (encodedData.endsWith("%")) {
                encodedData.deleteCharAt(encodedData.length - 1)
            }

            val imgHtml = URLDecoder.decode(encodedData.toString(), "UTF-8")
            val imgDoc = Jsoup.parse(imgHtml)

            for (img in imgDoc.select("img")) {
                val style = img.attr("style")
                if (style.isNotEmpty()) continue
                var url = ""
                for (attr in img.attributes()) {
                    if (attr.key.contains("data")) {
                        val v = attr.value
                        if (v.isNotEmpty() && !v.contains("blank") && !v.contains("loading")) {
                            url = if (v.startsWith("/")) "$cleanUrl$v" else v
                            break
                        }
                    }
                }
                if (url.isEmpty()) {
                    val src = img.attr("src")
                    if (src.isNotEmpty() && !src.contains("blank") && !src.contains("loading")) {
                        url = if (src.startsWith("/")) "$cleanUrl$src" else src
                    }
                }
                if (url.isNotEmpty()) result.add(url)
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}

suspend fun fetchEpisodeListWithSeriesId(baseUrl: String, episodeId: Int, cookieStr: String = ""): Pair<List<Episode>, Int> {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')
            val request = Request.Builder()
                .url("$cleanUrl/comic/$episodeId")
                .header("User-Agent", USER_AGENT)
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val body = httpClient.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext Pair(emptyList(), 0)

            val doc = Jsoup.parse(body)
            val navbar = doc.selectFirst("div.toon-nav") ?: return@withContext Pair(emptyList(), 0)
            val seriesId = navbar.select("a").last()
                ?.attr("href")?.split("comic/")?.getOrNull(1)
                ?.split("?")?.firstOrNull()
                ?.filter { it.isDigit() }?.toIntOrNull() ?: return@withContext Pair(emptyList(), 0)

            val episodes = fetchEpisodeList(cleanUrl, seriesId, cookieStr)
            Pair(episodes, seriesId)
        } catch (e: Exception) {
            Pair(emptyList(), 0)
        }
    }
}
