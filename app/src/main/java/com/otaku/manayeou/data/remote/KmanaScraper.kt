package com.otaku.manayeou.data.remote

import android.util.Log
import com.otaku.manayeou.data.model.Chapter
import com.otaku.manayeou.data.model.MangaPage
import com.otaku.manayeou.data.model.Series
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

private const val TAG = "KmanaScraper"

// config.js 에 정의된 이미지 CDN 도메인 (사이트가 종종 도메인을 교체하므로 하드코딩 대신 캐시)
private var cachedDnsSrc: String? = null
private var cachedDnsSrc2: String? = null

// "//host/path" 또는 상대경로를 절대 URL로 변환
private fun resolveImgUrl(raw: String, baseUrl: String = "https://kmana10.net"): String {
    val v = raw.trim()
    return when {
        v.isBlank() -> ""
        v.startsWith("http") -> v
        v.startsWith("//") -> "https:$v"
        else -> baseUrl.trimEnd('/') + "/" + v.trimStart('/')
    }
}

private suspend fun fetchImageDns(baseUrl: String): Pair<String, String> {
    cachedDnsSrc?.let { s1 -> cachedDnsSrc2?.let { s2 -> return s1 to s2 } }
    return try {
        val request = Request.Builder().url("$baseUrl/js/config.js").build()
        val js = withContext(Dispatchers.IO) {
            sharedHttpClient.newCall(request).execute().use { it.body?.string() ?: "" }
        }
        val dnsSrc = Regex("""dnsSrc\s*=\s*"([^"]*)"""").find(js)?.groupValues?.get(1)
            ?.let { resolveImgUrl(it) }?.ifBlank { null } ?: "https://www.11angle.net"
        val dnsSrc2 = Regex("""dnsSrc2\s*=\s*"([^"]*)"""").find(js)?.groupValues?.get(1)
            ?.let { resolveImgUrl(it) }?.ifBlank { null } ?: "https://www.pl3040.com"
        cachedDnsSrc = dnsSrc
        cachedDnsSrc2 = dnsSrc2
        Log.d(TAG, "fetchImageDns: dnsSrc=$dnsSrc dnsSrc2=$dnsSrc2")
        dnsSrc to dnsSrc2
    } catch (e: Exception) {
        Log.e(TAG, "fetchImageDns failed, using defaults", e)
        "https://www.11angle.net" to "https://www.pl3040.com"
    }
}

private suspend fun fetchDoc(url: String): Document = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(url)
        .header("Referer", "https://kmana10.net")
        .build()
    val html = sharedHttpClient.newCall(request).execute().use {
        it.body?.string() ?: ""
    }
    Log.d(TAG, "fetchDoc($url) html length=${html.length}")
    Jsoup.parse(html, url)
}

// 홈 목록(/latest, 인기순)은 서버가 정적으로는 항상 같은(기본 정렬) HTML을 내려주고,
// 실제 정렬은 클라이언트 JS가 /api/toon/list 를 orderType 파라미터와 함께 다시 호출해 교체하는 구조.
// (toonList.js 확인: URL 경로의 "hit"는 탭 활성 표시에만 쓰이고 실제 orderType은 클릭 이벤트로만 설정됨 —
//  그래서 /latest 와 /latest/hit 정적 HTML은 바이트 단위로 동일했음. API를 직접 호출해 우회.)
suspend fun scrapeLatest(baseUrl: String): List<Series> = fetchToonList(baseUrl, orderType = "")

suspend fun scrapePopular(baseUrl: String): List<Series> = fetchToonList(baseUrl, orderType = "hit")

private suspend fun fetchToonList(baseUrl: String, orderType: String): List<Series> = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder()
            .url("$baseUrl/api/toon/list?keyWord=&page=1&type=0&orderType=$orderType&toonListFrom=latestToonList")
            .header("Referer", "$baseUrl/latest")
            .header("X-Requested-With", "XMLHttpRequest")
            .build()
        val body = sharedHttpClient.newCall(request).execute().use { it.body?.string() ?: "" }
        Log.d(TAG, "fetchToonList(orderType=$orderType) response length=${body.length}")
        parseToonListJson(body, baseUrl)
    } catch (e: Exception) {
        Log.e(TAG, "fetchToonList failed", e)
        emptyList()
    }
}

// 검색 결과 페이지(/search)는 JS로 /api/search를 호출해 렌더링하므로 API를 직접 호출
suspend fun searchSeries(baseUrl: String, keyword: String): List<Series> = withContext(Dispatchers.IO) {
    try {
        val encoded = java.net.URLEncoder.encode(keyword, "UTF-8")
        val request = Request.Builder()
            .url("$baseUrl/api/search?key=$encoded")
            .header("Referer", "$baseUrl/search?key=$encoded")
            .header("X-Requested-With", "XMLHttpRequest")
            .build()
        val body = sharedHttpClient.newCall(request).execute().use { it.body?.string() ?: "" }
        Log.d(TAG, "searchSeries($keyword) raw response: ${body.take(1500)}")
        parseToonListJson(body, baseUrl)
    } catch (e: Exception) {
        Log.e(TAG, "searchSeries failed", e)
        emptyList()
    }
}

// /api/toon/list 와 /api/search 모두 { code, result: { total, list: [{ id, title, content, tags, author, src, ... }] } } 형태.
// /api/toon/list 의 항목엔 "src"에 커버 URL이 직접 오지만 /api/search 항목엔 없어서, 없을 때만 id 기반 패턴으로 대체.
private fun parseToonListJson(body: String, baseUrl: String): List<Series> {
    if (body.isBlank()) return emptyList()
    return try {
        val array = org.json.JSONObject(body).optJSONObject("result")?.optJSONArray("list") ?: org.json.JSONArray()
        (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val title = obj.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val coverRaw = obj.optString("src")
            val cover = if (coverRaw.isNotBlank()) resolveImgUrl(coverRaw) else "https://smallimage.11toon8.com/data/toon_category/$id.webp"
            Series(
                id = id,
                title = title,
                author = obj.optString("author"),
                coverUrl = cover,
                synopsis = obj.optString("content"),
                genre = obj.optString("tags"),
                sourceUrl = "$baseUrl/episode/$id/1/1"
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "parseToonListJson failed, body=${body.take(300)}", e)
        emptyList()
    }
}

// /episode/ID/1/1 페이지에서 시리즈 정보 + 챕터 목록 추출
suspend fun scrapeSeries(url: String): Series? {
    return try {
        val doc = fetchDoc(url)
        val id = url.substringAfter("/episode/").substringBefore("/")
        val title = (doc.selectFirst("h4") ?: doc.selectFirst("h3") ?: doc.selectFirst("h2"))
            ?.text()?.trim() ?: ""
        // doc.selectFirst("img") 는 헤더 로고를 잘못 잡으므로 시리즈 정보 영역으로 범위 한정
        val coverRaw = doc.selectFirst(".contents_view_info [data-src]")?.attr("data-src")
            ?: doc.selectFirst(".contents_view_info img")?.let { it.attr("data-src").ifBlank { it.attr("src") } }
            ?: ""
        val cover = resolveImgUrl(coverRaw)

        // 장르/작가/소개는 .date_info_item 안에 .d_name(라벨) + .d_info(값) 쌍으로 존재
        var author = ""
        var genre = ""
        var synopsis = ""
        doc.select(".date_info_item").forEach { item ->
            val label = item.selectFirst(".d_name")?.text()?.trim() ?: return@forEach
            val value = (item.selectFirst(".mEpisodeMoreText") ?: item.selectFirst(".d_info"))
                ?.text()?.trim() ?: ""
            when {
                label.startsWith("장르") -> genre = value
                label.startsWith("작가") -> author = value
                label.startsWith("소개") -> synopsis = value
            }
        }

        Log.d(TAG, "scrapeSeries($url) title=$title author=$author genre=$genre")
        if (title.isBlank()) null
        else Series(
            id = id,
            title = title,
            coverUrl = cover,
            author = author,
            genre = genre,
            synopsis = synopsis,
            sourceUrl = url
        )
    } catch (e: Exception) {
        Log.e(TAG, "scrapeSeries failed", e)
        null
    }
}

// href="/detail/ID/chapter-id" 패턴의 링크 추출 ("처음부터" 바로가기(class="first")는 실제 챕터 항목이 아니므로 제외)
private fun parseChapterListDoc(doc: Document, seriesId: String): List<Chapter> {
    val chapterLinks = doc.select("a[href*='/detail/']").filterNot { it.hasClass("first") }
    return chapterLinks.mapIndexed { i, el ->
        val href = el.attr("abs:href")
        val chapterId = href.substringAfterLast("/")
        val chapterTitle = (el.selectFirst("h5") ?: el.selectFirst("h4"))
            ?.text()?.trim() ?: el.text().trim()
        // 날짜는 항상 첫 번째 .view_date_item; 조회수/댓글수와 섞이지 않도록 span 전체가 아닌 이 컨테이너로 한정
        val date = el.select(".view_date_item").firstOrNull()?.text()?.trim() ?: ""
        // 소수 회차(예: "205.5화")가 있어 문자열 맨 끝에 붙는 번호를 기준으로 매칭 — 중간에서 첫 매치를 잡으면
        // "205.5화"의 ".5화"만 걸려 번호가 5로 잘못 끊기는 문제가 있었음
        val number = Regex("""(\d+(?:\.\d+)?)화$""").find(chapterTitle)?.groupValues?.get(1)
            ?: "${chapterLinks.size - i}"

        Chapter(
            id = chapterId,
            seriesId = seriesId,
            number = number,
            title = chapterTitle,
            date = date,
            url = href
        )
    }
}

// /episode/ID/1/1 페이지에서 챕터 목록 추출.
// 챕터 목록도 사이트 자체가 페이지네이션을 씀(정적 HTML엔 최근 pageSize개만 옴) — URL의 두 번째 세그먼트가
// 페이지 번호라서(/episode/{id}/{page}/{order}) episode.js 인라인 변수(total, pageSize)로 필요한 만큼 순회.
suspend fun scrapeChapters(seriesUrl: String): List<Chapter> {
    return try {
        val seriesId = seriesUrl.substringAfter("/episode/").substringBefore("/")
        val baseUrl = Regex("""^(https?://[^/]+)""").find(seriesUrl)?.groupValues?.get(1) ?: "https://kmana10.net"

        val firstDoc = fetchDoc(seriesUrl)
        val scriptText = firstDoc.select("script").joinToString("\n") { it.html() }
        val pageSize = Regex("""const\s+pageSize\s*=\s*(\d+)""").find(scriptText)?.groupValues?.get(1)?.toIntOrNull() ?: 100
        val total = Regex("""const\s+total\s*=\s*(\d+)""").find(scriptText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val totalPages = if (pageSize > 0 && total > 0) (total + pageSize - 1) / pageSize else 1

        val allChapters = mutableListOf<Chapter>()
        allChapters += parseChapterListDoc(firstDoc, seriesId)
        for (page in 2..totalPages) {
            val doc = fetchDoc("$baseUrl/episode/$seriesId/$page/1")
            allChapters += parseChapterListDoc(doc, seriesId)
        }

        Log.d(TAG, "scrapeChapters($seriesUrl): total=$total pageSize=$pageSize totalPages=$totalPages chapters=${allChapters.size}")
        allChapters.distinctBy { it.id }
    } catch (e: Exception) {
        Log.e(TAG, "scrapeChapters failed", e)
        emptyList()
    }
}

// /detail/ID/chapter-id 뷰어 페이지에서 이미지 URL 추출
suspend fun scrapePages(chapterUrl: String): List<MangaPage> {
    return try {
        val doc = fetchDoc(chapterUrl)
        val scriptText = doc.select("script").joinToString("\n") { it.html() }

        // 뷰어 페이지는 정적 img 없이 인라인 script의 const 변수로 이미지 URL을 조립함:
        // const id, parentId, folder, folder2, urls (콤마로 구분된 파일명 목록)
        fun extractVar(name: String): String =
            Regex("""const\s+$name\s*=\s*"([^"]*)"""").find(scriptText)?.groupValues?.get(1) ?: ""

        val chapterId = extractVar("id")
        val parentId = extractVar("parentId")
        val folder = extractVar("folder")
        val folder2 = extractVar("folder2")
        val urls = extractVar("urls")

        var images = if (urls.isNotBlank()) {
            val baseUrl = Regex("""^(https?://[^/]+)""").find(chapterUrl)?.groupValues?.get(1) ?: "https://kmana10.net"
            val (dnsSrc, dnsSrc2) = fetchImageDns(baseUrl)
            urls.split(",").mapNotNull { fileName ->
                val f = fileName.trim()
                if (f.isBlank()) return@mapNotNull null
                when {
                    folder2.isNotBlank() -> {
                        val lastSegment = f.substringAfterLast("/")
                        val firstFolder2 = folder2.substringBefore("/")
                        "$dnsSrc2/kr/$firstFolder2/$parentId/$chapterId/$lastSegment"
                    }
                    folder.isNotBlank() -> "$dnsSrc/image_pst-123/$folder/$parentId/$f"
                    else -> "$dnsSrc/toon_pst-123/$f"
                }
            }
        } else {
            emptyList()
        }
        Log.d(TAG, "scrapePages inline-script urls: ${images.size}")

        // 폴백 1: 정적 img 태그
        if (images.isEmpty()) {
            images = doc.select("img[src], img[data-src]").map { img ->
                img.attr("abs:src").ifBlank {
                    img.attr("data-src").let { if (it.startsWith("http")) it else "https://kmana10.net$it" }
                }
            }.filter { it.contains(".jpg") || it.contains(".jpeg") || it.contains(".png") || it.contains(".webp") }
            Log.d(TAG, "scrapePages static imgs: ${images.size}")
        }

        // 폴백 2: script 내 완전한 이미지 URL 정규식 추출
        if (images.isEmpty()) {
            val urlPattern = Regex("""["'](https?://[^"']*\.(?:jpg|jpeg|png|webp)(?:\?[^"']*)?)["']""")
            images = urlPattern.findAll(scriptText).map { it.groupValues[1] }.toList()
            Log.d(TAG, "scrapePages script imgs: ${images.size}")
        }

        images.mapIndexed { index, url -> MangaPage(index, url) }
    } catch (e: Exception) {
        Log.e(TAG, "scrapePages failed", e)
        emptyList()
    }
}