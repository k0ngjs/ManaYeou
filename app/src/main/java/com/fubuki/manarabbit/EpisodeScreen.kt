package com.fubuki.manarabbit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

data class EpisodeItem(
    val id: Int,
    val title: String,
    val date: String
)

data class MangaDetail(
    val name: String = "",
    val thumb: String = "",
    val author: String = "",
    val tags: List<String> = emptyList(),
    val release: String = ""
)

data class EpisodePageData(
    val detail: MangaDetail = MangaDetail(),
    val episodes: List<EpisodeItem> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeScreen(
    mangaId: Int,
    mangaName: String,
    onBack: () -> Unit,
    onEpisodeClick: (Int, String, List<EpisodeItem>) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val baseUrl by store.baseUrl.collectAsState(initial = "")
    val cfCookies by store.cfCookies.collectAsState(initial = "")
    val recentMangaStr by store.recentMangaV2.collectAsState(initial = "")
    val recentManga = remember(recentMangaStr) { store.parseRecentMangaList(recentMangaStr) }
    val lastEpisodeId = recentManga.find { it.mangaId == mangaId }?.lastEpisodeId
    var pageData by remember { mutableStateOf(EpisodePageData()) }
    var isLoading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(mangaId, baseUrl) {
        if (baseUrl.isNotEmpty()) {
            isLoading = true
            scope.launch {
                val result = fetchEpisodePageData(baseUrl, mangaId, cfCookies)
                if (result.episodes.isEmpty()) {
                    status = "에피소드를 불러오지 못했습니다"
                } else {
                    pageData = result
                    // 최근 본 만화 저장
                    store.saveRecentManga(
                        MangaItem(
                            id = mangaId,
                            name = result.detail.name,
                            thumb = result.detail.thumb,
                            referer = baseUrl
                        )
                    )
                }
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pageData.detail.name.ifEmpty { mangaName }, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                status.isNotEmpty() -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(status)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        isLoading = true
                        status = ""
                        scope.launch {
                            val result = fetchEpisodePageData(baseUrl, mangaId, cfCookies)
                            if (result.episodes.isEmpty()) {
                                status = "에피소드를 불러오지 못했습니다"
                            } else {
                                pageData = result
                            }
                            isLoading = false
                        }
                    }) { Text("재시도") }
                }
                else -> {
                    LazyColumn {
                        item {
                            MangaInfoHeader(
                                detail = pageData.detail,
                                baseUrl = baseUrl,
                                onFirstEpisodeClick = {
                                    val first = pageData.episodes.lastOrNull()
                                    if (first != null) {
                                        onEpisodeClick(first.id, first.title, pageData.episodes)
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                        items(pageData.episodes) { ep ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = ep.title,
                                        color = if (ep.id == lastEpisodeId)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                supportingContent = {
                                    Text(ep.date, style = MaterialTheme.typography.bodySmall)
                                },
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        store.saveRecentMangaV2(
                                            RecentMangaItem(
                                                mangaId = mangaId,
                                                mangaName = pageData.detail.name,
                                                thumb = pageData.detail.thumb,
                                                referer = baseUrl,
                                                lastEpisodeId = ep.id,
                                                lastEpisodeTitle = ep.title
                                            )
                                        )
                                    }
                                    onEpisodeClick(ep.id, ep.title, pageData.episodes)
                                }
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MangaInfoHeader(
    detail: MangaDetail,
    baseUrl: String,
    onFirstEpisodeClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Card(
            modifier = Modifier.size(width = 100.dp, height = 140.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(detail.thumb)
                    .addHeader("Referer", baseUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                    .crossfade(true)
                    .build(),
                contentDescription = detail.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (detail.author.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "작가: ${detail.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (detail.release.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "발행: ${detail.release}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (detail.tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = detail.tags.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onFirstEpisodeClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("첫화보기")
            }
        }
    }
}

suspend fun fetchEpisodePageData(
    baseUrl: String,
    mangaId: Int,
    cookieStr: String = ""
): EpisodePageData {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$cleanUrl/comic/$mangaId")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext EpisodePageData()
            response.close()

            val doc = Jsoup.parse(body)
            val header = doc.selectFirst("div.view-title")
            val name = header?.selectFirst("div.view-content b")?.ownText() ?: ""
            val thumb = header?.selectFirst("div.view-img img")?.attr("src") ?: ""
            var author = ""
            var release = ""
            val tags = mutableListOf<String>()

            header?.select("div.view-content")?.forEach { el ->
                when (el.selectFirst("strong")?.ownText()) {
                    "작가" -> author = el.selectFirst("a")?.ownText() ?: ""
                    "발행구분" -> release = el.selectFirst("a")?.ownText() ?: ""
                    "분류" -> el.select("a").forEach { tags.add(it.ownText()) }
                }
            }

            val detail = MangaDetail(name, thumb, author, tags, release)
            val episodes = mutableListOf<EpisodeItem>()
            val listBody = doc.selectFirst("ul.list-body") ?: return@withContext EpisodePageData(detail)

            for (e in listBody.select("li.list-item")) {
                val anchor = e.selectFirst("a.item-subject") ?: continue
                val href = anchor.attr("href")
                val id = href.split("comic/").getOrNull(1)
                    ?.split("?")?.firstOrNull()
                    ?.filter { it.isDigit() }?.toIntOrNull() ?: continue
                val title = anchor.ownText().trim()
                val date = e.selectFirst("div.wr-date")?.ownText()?.trim()
                    ?: e.selectFirst("div.item-details span")?.text()?.trim() ?: ""
                episodes.add(EpisodeItem(id, title, date))
            }

            EpisodePageData(detail, episodes)
        } catch (e: Exception) {
            EpisodePageData()
        }
    }
}

suspend fun fetchEpisodes(baseUrl: String, mangaId: Int, cookieStr: String = ""): List<EpisodeItem> {
    return fetchEpisodePageData(baseUrl, mangaId, cookieStr).episodes
}