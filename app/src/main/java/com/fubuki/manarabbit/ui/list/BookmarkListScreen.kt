package com.fubuki.manarabbit.ui.list

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
import com.fubuki.manarabbit.data.BookmarkedManga
import com.fubuki.manarabbit.data.Manga
import com.fubuki.manarabbit.data.SettingsDataStore
import com.fubuki.manarabbit.network.fetchEpisodeList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkListScreen(
    items: List<Manga>,
    onMangaClick: (Manga) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()
    val baseUrl by store.baseUrl.collectAsState(initial = "")
    val cfCookies by store.cfCookies.collectAsState(initial = "")

    var bookmarkItems by remember { mutableStateOf<List<BookmarkedManga>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(items, cfCookies) {
        if (cfCookies.isEmpty()) return@LaunchedEffect
        isLoading = true
        val result = items.map { manga ->
            scope.async(Dispatchers.IO) {
                try {
                    val episodes = fetchEpisodeList(baseUrl, manga.id, cfCookies)
                    val latest = episodes.firstOrNull()
                    BookmarkedManga(
                        manga = manga,
                        latestEpisodeId = latest?.id ?: 0,
                        latestEpisodeTitle = latest?.title ?: "",
                        latestEpisodeDate = latest?.date ?: ""
                    )
                } catch (e: Exception) {
                    BookmarkedManga(manga = manga)
                }
            }
        }.map { it.await() }
        bookmarkItems = result.sortedByDescending { it.latestEpisodeId }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("북마크") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "뒤로")
                    }
                }
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
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(bookmarkItems) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMangaClick(item.manga) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(modifier = Modifier.size(width = 70.dp, height = 95.dp)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.manga.thumb)
                                    .addHeader("Referer", item.manga.referer)
                                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.manga.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.width(12.dp))
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