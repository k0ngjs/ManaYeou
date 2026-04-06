package com.fubuki.manarabbit.ui.home

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
import com.fubuki.manarabbit.data.HomeContent
import com.fubuki.manarabbit.data.Manga
import com.fubuki.manarabbit.data.RecentManga
import com.fubuki.manarabbit.data.SettingsDataStore
import com.fubuki.manarabbit.ui.common.PullToRefreshWrapper

@Composable
fun HomeScreen(
    homeContent: HomeContent,
    isLoading: Boolean,
    status: String,
    onRefresh: suspend () -> Unit = {},
    onMangaClick: (Manga) -> Unit = {},
    onMoreUpdated: (List<Manga>) -> Unit = {},
    onMoreRecent: (List<RecentManga>) -> Unit = {},
    onMoreBookmark: (List<Manga>) -> Unit = {},
    onAuthNeeded: () -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }

    val recentMangaStr by store.recentManga.collectAsState(initial = "")
    val recentManga = remember(recentMangaStr) { store.parseMangaList(recentMangaStr) }
    val bookmarkStr by store.bookmarkManga.collectAsState(initial = "")
    val bookmarkManga = remember(bookmarkStr) { store.parseBookmarkList(bookmarkStr) }

    PullToRefreshWrapper(
        onRefresh = { onRefresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            status.isNotEmpty() -> Text(status, modifier = Modifier.align(Alignment.Center))
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    if (homeContent.updated.isNotEmpty()) {
                        item {
                            SectionTitle("최신 만화", onMoreClick = { onMoreUpdated(homeContent.updated) })
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(homeContent.updated.take(8)) { manga ->
                                    MangaCard(manga, onClick = { onMangaClick(manga) })
                                }
                            }
                        }
                    }

                    if (recentManga.isNotEmpty()) {
                        item {
                            SectionTitle("최근 본 만화", onMoreClick = { onMoreRecent(recentManga) })
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(recentManga.take(8)) { manga ->
                                    RecentMangaCard(manga, onClick = {
                                        onMangaClick(Manga(manga.mangaId, manga.mangaName, manga.thumb, manga.referer))
                                    })
                                }
                            }
                        }
                    }

                    if (bookmarkManga.isNotEmpty()) {
                        item {
                            SectionTitle("북마크", onMoreClick = { onMoreBookmark(bookmarkManga) })
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(bookmarkManga.take(8)) { manga ->
                                    MangaCard(manga, onClick = { onMangaClick(manga) })
                                }
                            }
                        }
                    }

                    if (homeContent.popular.isNotEmpty()) {
                        item {
                            SectionTitle("인기 만화")
                        }
                        items(homeContent.popular) { manga ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMangaClick(manga) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = manga.name,
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
fun MangaCard(manga: Manga, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    Column(modifier = Modifier.width(110.dp).clickable { onClick() }) {
        Card(modifier = Modifier.width(110.dp).height(150.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(manga.thumb)
                    .addHeader("Referer", manga.referer)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                    .crossfade(true)
                    .build(),
                contentDescription = manga.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = manga.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun RecentMangaCard(manga: RecentManga, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    Column(modifier = Modifier.width(110.dp).clickable { onClick() }) {
        Card(modifier = Modifier.width(110.dp).height(150.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(manga.thumb)
                    .addHeader("Referer", manga.referer)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                    .crossfade(true)
                    .build(),
                contentDescription = manga.mangaName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = manga.mangaName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}