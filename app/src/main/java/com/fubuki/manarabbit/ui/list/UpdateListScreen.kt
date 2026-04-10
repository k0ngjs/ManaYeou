package com.fubuki.manarabbit.ui.list

import com.fubuki.manarabbit.network.USER_AGENT

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
import com.fubuki.manarabbit.data.Manga

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateListScreen(
    title: String,
    items: List<Manga>,
    onMangaClick: (Manga) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(items) { manga ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMangaClick(manga) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(modifier = Modifier.size(width = 70.dp, height = 95.dp)) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(manga.thumb)
                                .addHeader("Referer", manga.referer)
                                .addHeader("User-Agent", USER_AGENT)
                                .crossfade(true)
                                .build(),
                            contentDescription = manga.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = manga.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}