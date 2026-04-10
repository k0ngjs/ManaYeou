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
import com.fubuki.manarabbit.data.RecentManga

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentListScreen(
    items: List<RecentManga>,
    onMangaClick: (RecentManga) -> Unit,
    onDeleteItems: (List<RecentManga>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var editMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Int>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("최근 본 만화") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                val toDelete = items.filter { it.mangaId in selected }
                                onDeleteItems(toDelete)
                                selected = emptySet()
                                editMode = false
                            },
                            enabled = selected.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Delete, "삭제",
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
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(items, key = { it.mangaId }) { manga ->
                val isSelected = manga.mangaId in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (editMode) {
                                selected = if (isSelected)
                                    selected - manga.mangaId
                                else
                                    selected + manga.mangaId
                            } else {
                                onMangaClick(manga)
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
                                .data(manga.thumb)
                                .addHeader("Referer", manga.referer)
                                .addHeader("User-Agent", USER_AGENT)
                                .crossfade(true)
                                .build(),
                            contentDescription = manga.mangaName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = manga.mangaName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = manga.lastEpisodeTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}
