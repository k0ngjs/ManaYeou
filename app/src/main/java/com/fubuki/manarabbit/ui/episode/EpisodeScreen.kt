package com.fubuki.manarabbit.ui.episode

import com.fubuki.manarabbit.network.USER_AGENT

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
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
import com.fubuki.manarabbit.data.Episode
import com.fubuki.manarabbit.data.Manga
import com.fubuki.manarabbit.data.MangaDetail
import com.fubuki.manarabbit.data.MangaInfo
import com.fubuki.manarabbit.data.RecentManga
import com.fubuki.manarabbit.data.SettingsDataStore
import com.fubuki.manarabbit.network.fetchMangaDetail
import com.fubuki.manarabbit.ui.common.PullToRefreshWrapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeScreen(
    mangaId: Int,
    mangaName: String,
    cachedDetail: MangaDetail = MangaDetail(),
    onDetailLoaded: (MangaDetail) -> Unit = {},
    onBack: () -> Unit,
    onEpisodeClick: (Int, String) -> Unit = { _, _ -> },
    onAuthNeeded: () -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val baseUrl by store.baseUrl.collectAsState(initial = "")
    val cfCookies by store.cfCookies.collectAsState(initial = "")
    val recentMangaStr by store.recentManga.collectAsState(initial = "")
    val recentMangaList = remember(recentMangaStr) { store.parseRecentMangaList(recentMangaStr) }
    val lastEpisodeId = recentMangaList.find { it.mangaId == mangaId }?.lastEpisodeId
    val episodeCacheStr by store.episodeCache.collectAsState(initial = "")
    // DataStore 캐시 또는 인메모리 캐시 우선 적용
    val initialDetail = remember(mangaId) {
        if (cachedDetail.episodes.isNotEmpty()) cachedDetail
        else MangaDetail()
    }
    var mangaDetail by remember { mutableStateOf(initialDetail) }
    var isLoading by remember { mutableStateOf(initialDetail.episodes.isEmpty()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val bookmarkStr by store.bookmarkManga.collectAsState(initial = "")
    val isBookmarked = store.isBookmarked(mangaId, bookmarkStr)

    suspend fun loadDetail(refresh: Boolean = false) {
        if (refresh) isRefreshing = true else isLoading = true
        status = ""
        try {
            val result = fetchMangaDetail(baseUrl, mangaId, cfCookies)
            if (result.episodes.isEmpty()) {
                status = "에피소드를 불러오지 못했습니다"
            } else {
                mangaDetail = result
                onDetailLoaded(result)
                // 에피소드 목록 DataStore에 캐시
                scope.launch { store.saveEpisodeCache(mangaId, result.episodes) }
                val existing = store.parseRecentMangaList(store.recentManga.first())
                    .find { it.mangaId == mangaId }
                if (existing != null) {
                    store.saveRecentManga(
                        existing.copy(
                            mangaName = result.info.name,
                            thumb = result.info.thumb,
                            referer = baseUrl
                        )
                    )
                }
            }
        } catch (e: Exception) {
            status = "에피소드를 불러오지 못했습니다"
        }
        if (refresh) isRefreshing = false else isLoading = false
    }

    LaunchedEffect(mangaId, baseUrl) {
        if (baseUrl.isEmpty()) return@LaunchedEffect
        if (mangaDetail.episodes.isEmpty()) {
            // DataStore 캐시 확인
            val cached = store.parseEpisodeCache(episodeCacheStr, mangaId)
            if (cached.isNotEmpty()) {
                mangaDetail = mangaDetail.copy(episodes = cached)
                isLoading = false
            } else {
                loadDetail()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            TopAppBar(
                title = { Text(mangaDetail.info.name.ifEmpty { mangaName }, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "뒤로")
                    }
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                        scope.launch { loadDetail() }
                    }) { Text("재시도") }
                }
                else -> PullToRefreshWrapper(
                    onRefresh = { loadDetail(refresh = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn {
                        item {
                            MangaInfoHeader(
                                info = mangaDetail.info,
                                baseUrl = baseUrl,
                                isBookmarked = isBookmarked,
                                onFirstEpisodeClick = {
                                    val first = mangaDetail.episodes.lastOrNull()
                                    if (first != null) {
                                        scope.launch {
                                            store.saveRecentManga(
                                                RecentManga(
                                                    mangaId = mangaId,
                                                    mangaName = mangaDetail.info.name,
                                                    thumb = mangaDetail.info.thumb,
                                                    referer = baseUrl,
                                                    lastEpisodeId = first.id,
                                                    lastEpisodeTitle = first.title
                                                )
                                            )
                                        }
                                        onEpisodeClick(first.id, first.title)
                                    }
                                },
                                onBookmarkClick = {
                                    scope.launch {
                                        store.toggleBookmark(
                                            Manga(
                                                id = mangaId,
                                                name = mangaDetail.info.name,
                                                thumb = mangaDetail.info.thumb,
                                                referer = baseUrl
                                            )
                                        )
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                        items(mangaDetail.episodes) { episode ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = episode.title,
                                        color = if (episode.id == lastEpisodeId)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                supportingContent = {
                                    Text(episode.date, style = MaterialTheme.typography.bodySmall)
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                                ),
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        store.saveRecentManga(
                                            RecentManga(
                                                mangaId = mangaId,
                                                mangaName = mangaDetail.info.name,
                                                thumb = mangaDetail.info.thumb,
                                                referer = baseUrl,
                                                lastEpisodeId = episode.id,
                                                lastEpisodeTitle = episode.title
                                            )
                                        )
                                    }
                                    onEpisodeClick(episode.id, episode.title)
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
    info: MangaInfo,
    baseUrl: String,
    isBookmarked: Boolean = false,
    onFirstEpisodeClick: () -> Unit,
    onBookmarkClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Card(
            modifier = Modifier.size(width = 100.dp, height = 140.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(info.thumb)
                    .addHeader("Referer", baseUrl)
                    .addHeader("User-Agent", USER_AGENT)
                    .crossfade(true)
                    .build(),
                contentDescription = info.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f).heightIn(min = 140.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = info.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (info.author.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "작가: ${info.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (info.release.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "발행: ${info.release}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (info.tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = info.tags.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onFirstEpisodeClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("첫화보기")
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "북마크"
                    )
                }
            }
        }
    }
}