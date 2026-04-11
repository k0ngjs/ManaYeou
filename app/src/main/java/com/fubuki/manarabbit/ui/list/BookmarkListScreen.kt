package com.fubuki.manarabbit.ui.list

import com.fubuki.manarabbit.network.USER_AGENT

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
import coil.request.ImageRequest
import com.fubuki.manarabbit.data.BookmarkedManga
import com.fubuki.manarabbit.data.Manga
import com.fubuki.manarabbit.data.SettingsDataStore
import com.fubuki.manarabbit.network.fetchEpisodeList
import com.fubuki.manarabbit.network.fetchMangaDetail
import com.fubuki.manarabbit.ui.common.PullToRefreshWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
    var isRefreshing by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Int>()) }

    suspend fun loadBookmarks(refresh: Boolean = false) {
        if (refresh) isRefreshing = true else isLoading = true
        val result = coroutineScope {
            items.map { manga ->
                async(Dispatchers.IO) {
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
            }.awaitAll()
        }
        bookmarkItems = result.sortedByDescending { it.latestEpisodeId }
        onItemsLoaded(bookmarkItems)
        if (refresh) isRefreshing = false else isLoading = false
    }

    LaunchedEffect(items, cfCookies) {
        if (cachedItems.isNotEmpty()) {
            bookmarkItems = cachedItems
            isLoading = false
            return@LaunchedEffect
        }
        if (cfCookies.isEmpty()) return@LaunchedEffect
        loadBookmarks()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
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
                onRefresh = { loadBookmarks(refresh = true) },
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
                                    model = ImageRequest.Builder(context)
                                        .data(item.manga.thumb)
                                        .addHeader("Referer", item.manga.referer)
                                        .addHeader("User-Agent", USER_AGENT)
                                        .crossfade(true)
                                        .build(),
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
