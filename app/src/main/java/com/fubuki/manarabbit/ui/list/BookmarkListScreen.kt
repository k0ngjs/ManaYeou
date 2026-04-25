package com.fubuki.manarabbit.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fubuki.manarabbit.data.BookmarkedManga
import com.fubuki.manarabbit.data.Manga
import com.fubuki.manarabbit.data.SettingsDataStore
import com.fubuki.manarabbit.network.fetchEpisodeList
import com.fubuki.manarabbit.network.fetchMangaDetail
import com.fubuki.manarabbit.ui.common.PullToRefreshWrapper
import com.fubuki.manarabbit.ui.common.mangaImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkListScreen(
    items: List<Manga>,
    cachedItems: List<BookmarkedManga> = emptyList(),
    onItemsLoaded: (List<BookmarkedManga>) -> Unit = {},
    onMangaClick: (Manga) -> Unit,
    onDeleteItems: (List<Manga>) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()
    val baseUrl by store.baseUrl.collectAsState(initial = "")
    val cfCookies by store.cfCookies.collectAsState(initial = "")

    var bookmarkItems by remember { mutableStateOf<List<BookmarkedManga>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var editMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Int>()) }

    val bookmarkDetailCache by store.bookmarkDetail.collectAsState(initial = "")

    suspend fun fetchAndSave() {
        // 한 번에 3개씩 순차 처리하여 서버 과부하 방지
        val result = mutableListOf<BookmarkedManga>()
        for (chunk in items.chunked(3)) {
            for (manga in chunk) {
                val item = withContext(Dispatchers.IO) {
                    try {
                        if (manga.thumb.isEmpty()) {
                            val detail = fetchMangaDetail(baseUrl, manga.id, cfCookies)
                            val latest = detail.episodes.firstOrNull()
                            val updatedManga = manga.copy(
                                name = detail.info.name.ifEmpty { manga.name },
                                thumb = detail.info.thumb,
                                referer = baseUrl
                            )
                            store.updateBookmarkThumb(updatedManga)
                            BookmarkedManga(
                                manga = updatedManga,
                                latestEpisodeId = latest?.id ?: 0,
                                latestEpisodeTitle = latest?.title ?: "",
                                latestEpisodeDate = latest?.date ?: ""
                            )
                        } else {
                            val episodes = fetchEpisodeList(baseUrl, manga.id, cfCookies)
                            val latest = episodes.firstOrNull()
                            BookmarkedManga(
                                manga = manga,
                                latestEpisodeId = latest?.id ?: 0,
                                latestEpisodeTitle = latest?.title ?: "",
                                latestEpisodeDate = latest?.date ?: ""
                            )
                        }
                    } catch (e: Exception) {
                        BookmarkedManga(manga = manga)
                    }
                }
                result.add(item)
            }
            if (chunk.size == 3) delay(300)
        }
        val sorted = result.sortedByDescending { it.latestEpisodeId }
        bookmarkItems = sorted
        onItemsLoaded(sorted)
        store.saveBookmarkList(sorted.map { it.manga })
        store.saveBookmarkDetails(sorted)
    }

    LaunchedEffect(items) {
        val itemIds = items.map { it.id }.toSet()

        // 1. 인메모리 캐시가 현재 목록과 완전히 일치하면 즉시 표시
        if (cachedItems.isNotEmpty() && cachedItems.map { it.manga.id }.toSet() == itemIds) {
            bookmarkItems = cachedItems
            isLoading = false
            return@LaunchedEffect
        }

        // 2. DataStore 캐시 확인 (현재 items와 일치하는 항목만 필터)
        val cached = store.parseBookmarkDetails(bookmarkDetailCache).filter { it.manga.id in itemIds }
        if (cached.map { it.manga.id }.toSet() == itemIds) {
            bookmarkItems = cached
            onItemsLoaded(cached)
            isLoading = false
            return@LaunchedEffect
        }

        // 3. 캐시 불일치 → 가진 것 먼저 표시 (최소한 이름·썸네일은 보여줌)
        val partial = items.map { manga ->
            cached.find { it.manga.id == manga.id } ?: BookmarkedManga(manga = manga)
        }
        bookmarkItems = partial
        isLoading = false

        // 4. cfCookies가 있으면 에피소드 정보 백그라운드 갱신
        if (cfCookies.isEmpty() || baseUrl.isEmpty()) return@LaunchedEffect
        fetchAndSave()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("북마크") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (editMode) {
                            editMode = false
                            selected = emptySet()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, "뒤로")
                    }
                },
                actions = {
                    if (editMode) {
                        IconButton(
                            onClick = {
                                val toDelete = bookmarkItems
                                    .filter { it.manga.id in selected }
                                    .map { it.manga }
                                bookmarkItems = bookmarkItems.filter { it.manga.id !in selected }
                                onDeleteItems(toDelete)
                                selected = emptySet()
                                editMode = false
                            },
                            enabled = selected.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Filled.Delete, "삭제",
                                tint = if (selected.isNotEmpty())
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        TextButton(onClick = { editMode = true }) {
                            Text("편집")
                        }
                    }
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("업데이트 확인 중...", style = MaterialTheme.typography.bodySmall)
                }
            }
            items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("북마크한 만화가 없어요", style = MaterialTheme.typography.bodyMedium)
            }
            else -> PullToRefreshWrapper(
                onRefresh = { fetchAndSave() },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(bookmarkItems, key = { it.manga.id }) { item ->
                        val isSelected = item.manga.id in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (editMode) {
                                        selected = if (isSelected)
                                            selected - item.manga.id
                                        else
                                            selected + item.manga.id
                                    } else {
                                        onMangaClick(item.manga)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (editMode) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Outlined.CheckBox
                                                  else Icons.Outlined.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Card(modifier = Modifier.size(width = 70.dp, height = 95.dp)) {
                                AsyncImage(
                                    model = mangaImageRequest(context, item.manga.thumb, item.manga.referer),
                                    contentDescription = item.manga.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.manga.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.latestEpisodeTitle.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = item.latestEpisodeTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.latestEpisodeDate.isNotEmpty()) {
                                        Text(
                                            text = item.latestEpisodeDate,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
