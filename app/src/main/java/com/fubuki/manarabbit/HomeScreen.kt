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

data class MangaItem(
    val id: Int,
    val name: String,
    val thumb: String,
    val referer: String = "",
    val isEpisode: Boolean = false
)

data class HomeData(
    val updated: List<MangaItem> = emptyList(),
    val popular: List<MangaItem> = emptyList()
)

@Composable
fun HomeScreen(
    onMangaClick: (MangaItem) -> Unit = {},
    onMoreUpdated: (List<MangaItem>) -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val baseUrl by store.baseUrl.collectAsState(initial = "")
    val cfCookies by store.cfCookies.collectAsState(initial = "")
    var homeData by remember { mutableStateOf(HomeData()) }
    var status by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(baseUrl) {
        if (baseUrl.isNotEmpty()) {
            isLoading = true
            status = ""
            scope.launch {
                val result = fetchHomeData(baseUrl, cfCookies)
                if (result.updated.isEmpty() && result.popular.isEmpty()) {
                    status = "목록을 불러오지 못했습니다"
                } else {
                    homeData = result
                }
                isLoading = false
            }
        } else {
            status = "설정에서 서버 주소를 입력해주세요"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            status.isNotEmpty() -> Text(status, modifier = Modifier.align(Alignment.Center))
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    if (homeData.updated.isNotEmpty()) {
                        item {
                            SectionTitle("최신 만화", onMoreClick = { onMoreUpdated(homeData.updated) })
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(homeData.updated.take(10)) { item ->
                                    MangaCard(item, onClick = { onMangaClick(item) })
                                }
                            }
                        }
                    }
                    if (homeData.popular.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            SectionTitle("인기 만화")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(homeData.popular.take(10)) { item ->
                                    MangaCard(item, onClick = { onMangaClick(item) })
                                }
                            }
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

suspend fun fetchHomeData(baseUrl: String, cookieStr: String = ""): HomeData {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')
            val client = OkHttpClient()

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

            val popularRequest = Request.Builder()
                .url("$cleanUrl/page/popular")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val popularResponse = client.newCall(popularRequest).execute()
            val popularBody = popularResponse.body?.string() ?: ""
            popularResponse.close()

            val popularDoc = Jsoup.parse(popularBody)
            val popular = mutableListOf<MangaItem>()
            for (e in popularDoc.select("div.media.post-list").take(20)) {
                val seriesId = e.selectFirst("a.btn-primary")?.attr("rel")?.toIntOrNull() ?: continue
                val thumb = e.selectFirst("img")?.attr("src") ?: ""
                val name = e.selectFirst("div.post-subject a")?.ownText()?.trim() ?: continue
                popular.add(MangaItem(seriesId, name, thumb, cleanUrl))
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