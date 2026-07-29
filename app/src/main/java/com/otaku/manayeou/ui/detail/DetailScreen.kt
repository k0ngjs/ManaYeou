package com.otaku.manayeou.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.otaku.manayeou.data.model.Chapter
import com.otaku.manayeou.data.model.Series

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    seriesUrl: String,
    onBack: () -> Unit,
    onChapterClick: (Chapter, Series) -> Unit
) {
    val app = LocalContext.current.applicationContext as android.app.Application
    val viewModel: DetailViewModel = viewModel(
        key = seriesUrl,
        factory = viewModelFactory {
            initializer { DetailViewModel(app, seriesUrl) }
        }
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.series?.title ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        state.error?.let {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        LazyColumn(contentPadding = padding) {
            // 시리즈 헤더
            state.series?.let { series ->
                item {
                    var synopsisExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = series.coverUrl,
                            contentDescription = series.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 110.dp, height = 150.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                series.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (series.author.isNotBlank())
                                Text(series.author, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (series.genre.isNotBlank())
                                Text(series.genre, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (series.synopsis.isNotBlank())
                                Text(
                                    series.synopsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            if (series.latestChapter.isNotBlank())
                                Text("최근: ${series.latestChapter}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.toggleBookmark() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (state.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (state.isBookmarked) "북마크됨" else "북마크")
                        }
                        Button(
                            onClick = {
                                state.chapters.lastOrNull()?.let { firstChapter ->
                                    onChapterClick(firstChapter, series)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = state.chapters.isNotEmpty()
                        ) {
                            Text("처음부터")
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            items(state.chapters) { chapter ->
                ChapterItem(
                    chapter = chapter,
                    seriesTitle = state.series?.title ?: "",
                    isLastRead = chapter.url == state.lastReadChapterUrl,
                    onClick = { state.series?.let { series -> onChapterClick(chapter, series) } }
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChapterItem(chapter: Chapter, seriesTitle: String, isLastRead: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isLastRead) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surface
    ) {
        // 헤더에 이미 나온 시리즈명만 제목에서 제거하고 한 줄로 표시
        val displayTitle = chapter.title
            .let { if (seriesTitle.isNotBlank()) it.replace(seriesTitle, "") else it }
            .trim(' ', '-', '–', '·', ':', '|')
            .ifBlank { "${chapter.number}화" }

        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isLastRead) MaterialTheme.colorScheme.primary else Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (chapter.date.isNotBlank()) {
                Text(
                    chapter.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
