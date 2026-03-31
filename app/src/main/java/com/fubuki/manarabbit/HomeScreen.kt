package com.fubuki.manarabbit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

data class HomeData(
    val updated: List<MangaItem> = emptyList(),
    val popular: List<MangaItem> = emptyList()
)

@Composable
fun HomeScreen(
    onMangaClick: (MangaItem) -> Unit = {},
    onMoreUpdated: (List<MangaItem>) -> Unit = {},
    onMoreRecent: (List<RecentMangaItem>) -> Unit = {},
    onMoreBookmark: (List<MangaItem>) -> Unit = {},
    onAuthNeeded: () -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val baseUrl by store.baseUrl.collectAsState(initial = "")
    val cfCookies by store.cfCookies.collectAsState(initial = "")
    val recentMangaStr by store.recentMangaV2.collectAsState(initial = "")
    val recentManga = remember(recentMangaStr) { store.parseRecentMangaList(recentMangaStr) }

    var homeData by remember { mutableStateOf(HomeData()) }
    var status by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val bookmarkStr by store.bookmarkManga.collectAsState(initial = "")
    val bookmarkManga = remember(bookmarkStr) { store.parseMangaList(bookmarkStr) }

    LaunchedEffect(baseUrl) {
        if (baseUrl.isEmpty()) return@LaunchedEffect
        isLoading = true
        status = ""
        scope.launch {
            val result = fetchHomeData(baseUrl, cfCookies)
            if (result.updated.isEmpty() && result.popular.isEmpty()) {
                status = "목록을 불러오지 못했습니다"
                if (cfCookies.isEmpty()) onAuthNeeded()  // 쿠키가 없을 때만 인증 요청
            } else {
                homeData = result
            }
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            status.isNotEmpty() -> Text(status, modifier = Modifier.align(Alignment.Center))
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    // 최신 만화
                    if (homeData.updated.isNotEmpty()) {
                        item {
                            SectionTitle("최신 만화", onMoreClick = { onMoreUpdated(homeData.updated) })
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(homeData.updated.take(8)) { item ->
                                    MangaCard(item, onClick = { onMangaClick(item) })
                                }
                            }
                        }
                    }

                    // 최근 본 만화
                    if (recentManga.isNotEmpty()) {
                        item {
                            SectionTitle("최근 본 만화", onMoreClick = { onMoreRecent(recentManga) })
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(recentManga.take(8)) { item ->
                                    RecentMangaCard(item, onClick = { onMangaClick(MangaItem(item.mangaId, item.mangaName, item.thumb, item.referer)) })
                                }
                            }
                        }
                    }

                    // 북마크
                    if (bookmarkManga.isNotEmpty()) {
                        item {
                            SectionTitle("북마크", onMoreClick = { onMoreBookmark(bookmarkManga) })
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(bookmarkManga.take(8)) { item ->
                                    MangaCard(item, onClick = { onMangaClick(item) })
                                }
                            }
                        }
                    }

                    // 인기 만화
                    if (homeData.popular.isNotEmpty()) {
                        item {
                            SectionTitle("인기 만화")
                        }
                        items(homeData.popular) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMangaClick(item) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, onMoreClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (onMoreClick != null) {
            TextButton(onClick = onMoreClick) { Text("더보기") }
        }
    }
}

@Composable
fun MangaCard(item: MangaItem, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    Column(modifier = Modifier.width(110.dp).clickable { onClick() }) {
        Card(modifier = Modifier.width(110.dp).height(150.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.thumb)
                    .addHeader("Referer", item.referer)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun RecentMangaCard(item: RecentMangaItem, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    Column(modifier = Modifier.width(110.dp).clickable { onClick() }) {
        Box {
            Card(modifier = Modifier.width(110.dp).height(150.dp)) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.thumb)
                        .addHeader("Referer", item.referer)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                        .crossfade(true)
                        .build(),
                    contentDescription = item.mangaName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Text(
            text = item.mangaName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

suspend fun fetchHomeData(baseUrl: String, cookieStr: String = ""): HomeData {
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
            val updated = mutableListOf<MangaItem>()
            for (e in updateDoc.select("div.media.post-list").take(20)) {
                val seriesId = e.selectFirst("a.btn-primary")?.attr("rel")?.toIntOrNull() ?: continue
                val thumb = e.selectFirst("img")?.attr("src") ?: ""
                val name = e.selectFirst("div.post-subject a")?.ownText()?.trim() ?: continue
                updated.add(MangaItem(seriesId, name, thumb, cleanUrl))
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
            val popular = mutableListOf<MangaItem>()
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
                popular.add(MangaItem(episodeId, name, "", cleanUrl, isEpisode = true))
            }

            HomeData(updated, popular)
        } catch (e: Exception) {
            HomeData()
        }
    }
}

suspend fun fetchMangaList(baseUrl: String, cookieStr: String = ""): List<MangaItem> {
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
            val result = mutableListOf<MangaItem>()
            for (e in doc.select("div.media.post-list")) {
                val seriesId = e.selectFirst("a.btn-primary")?.attr("rel")?.toIntOrNull() ?: continue
                val thumb = e.selectFirst("img")?.attr("src") ?: ""
                val name = e.selectFirst("div.post-subject a")?.ownText()?.trim() ?: continue
                result.add(MangaItem(seriesId, name, thumb, cleanUrl))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}