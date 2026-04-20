package com.fubuki.manarabbit.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder

data class ViewerResult(
    val images: List<String>,
    val prevId: Int,   // 이전화 episode ID (없으면 0)
    val nextId: Int,   // 다음화 episode ID (없으면 0)
    val episodeTitle: String, // 현재 에피소드 제목 (og:title에서 파싱)
    val seriesId: Int,
    val mangaName: String,
    val thumb: String
)

/**
 * /comic/{episodeId} 한 번만 요청해서
 * 이미지 + 이전화/다음화 ID + seriesId + 만화 이름 + 썸네일 모두 파싱
 */
suspend fun fetchViewerData(baseUrl: String, episodeId: Int, cookieStr: String = ""): ViewerResult {
    return withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trimEnd('/')

        val request = Request.Builder()
            .url("$cleanUrl/comic/$episodeId")
            .header("User-Agent", USER_AGENT)
            .header("Referer", cleanUrl)
            .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) { response.close(); throw Exception("HTTP ${response.code}") }
        val body = response.use { it.body?.string() }
            ?: return@withContext ViewerResult(emptyList(), 0, 0, "", 0, "", "")

        val doc = Jsoup.parse(body)

        // 이미지 파싱
        val images = parseImages(doc, cleanUrl)

        // toon-nav에서 이전화/다음화/seriesId 파싱
        val nav = doc.selectFirst("div.toon-nav")
        val prevHref = nav?.selectFirst("a#goPrevBtn")?.attr("href") ?: ""
        val nextHref = nav?.selectFirst("a#goNextBtn")?.attr("href") ?: ""
        val prevId = prevHref.split("comic/").getOrNull(1)
            ?.split("?")?.firstOrNull()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val nextId = nextHref.split("comic/").getOrNull(1)
            ?.split("?")?.firstOrNull()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        // 목록 링크(last)에서 seriesId 파싱
        val seriesId = nav?.select("a")?.last()
            ?.attr("href")?.split("comic/")?.getOrNull(1)
            ?.split("?")?.firstOrNull()
            ?.filter { it.isDigit() }?.toIntOrNull() ?: 0

        // 만화 이름 + 에피소드 제목 + 썸네일 파싱
        // og:title 형식: "에피소드 제목 | 사이트명" 또는 "만화 - 에피소드 | 사이트명"
        val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content") ?: ""
        val episodeTitle = ogTitle.split("|").firstOrNull()?.trim() ?: ogTitle
        val mangaName = doc.selectFirst("div.toon-title h1")?.text()
            ?: episodeTitle.split("-").firstOrNull()?.trim() ?: episodeTitle
        val thumb = doc.selectFirst("meta[property=og:image]")?.attr("content") ?: ""

        ViewerResult(
            images = images,
            prevId = prevId,
            nextId = nextId,
            episodeTitle = episodeTitle,
            seriesId = seriesId,
            mangaName = mangaName,
            thumb = thumb
        )
    }
}

private fun parseImages(doc: org.jsoup.nodes.Document, cleanUrl: String): List<String> {
    val result = mutableListOf<String>()
    try {
        val viewPaddings = doc.select("div.view-padding")
        if (viewPaddings.size < 2) return result

        val script = viewPaddings[1].selectFirst("script")?.data() ?: return result

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
        if (encodedData.endsWith("%")) encodedData.deleteCharAt(encodedData.length - 1)

        val imgHtml = URLDecoder.decode(encodedData.toString(), "UTF-8")
        val imgDoc = Jsoup.parse(imgHtml)

        for (img in imgDoc.select("img")) {
            if (img.attr("style").isNotEmpty()) continue
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
    } catch (_: Exception) {}
    return result
}
