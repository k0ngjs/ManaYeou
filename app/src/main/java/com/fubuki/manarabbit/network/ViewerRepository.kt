package com.fubuki.manarabbit.network

import com.fubuki.manarabbit.data.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder

data class ViewerResult(
    val images: List<String>,
    val episodes: List<Episode>,
    val seriesId: Int,
    val mangaName: String,
    val thumb: String
)

/**
 * /comic/{episodeId} 한 번 + /comic/{seriesId} 한 번 = 총 2개 요청으로
 * 이미지 목록 + 에피소드 목록 + 만화 정보를 모두 가져옴
 */
suspend fun fetchViewerData(baseUrl: String, episodeId: Int, cookieStr: String = ""): ViewerResult {
    return withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trimEnd('/')

        // 1번 요청: /comic/{episodeId} — 이미지 + seriesId 동시 파싱
        val request = Request.Builder()
            .url("$cleanUrl/comic/$episodeId")
            .header("User-Agent", USER_AGENT)
            .header("Referer", cleanUrl)
            .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
            .build()
        val response = httpClient.newCall(request).execute()
        if (response.code == 403) { response.close(); throw AuthRequiredException() }
        val body = response.use { it.body?.string() }
            ?: return@withContext ViewerResult(emptyList(), emptyList(), 0, "", "")

        val doc = Jsoup.parse(body)

        // 이미지 파싱
        val images = parseImages(doc, cleanUrl)

        // seriesId 파싱
        val seriesId = doc.selectFirst("div.toon-nav")
            ?.select("a")?.last()
            ?.attr("href")?.split("comic/")?.getOrNull(1)
            ?.split("?")?.firstOrNull()
            ?.filter { it.isDigit() }?.toIntOrNull() ?: 0

        if (seriesId == 0) {
            return@withContext ViewerResult(images, emptyList(), 0, "", "")
        }

        // 2번 요청: /comic/{seriesId} — 에피소드 목록 + 만화 이름 + 썸네일
        val detail = fetchMangaDetail(cleanUrl, seriesId, cookieStr)
        ViewerResult(
            images = images,
            episodes = detail.episodes,
            seriesId = seriesId,
            mangaName = detail.info.name,
            thumb = detail.info.thumb
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
