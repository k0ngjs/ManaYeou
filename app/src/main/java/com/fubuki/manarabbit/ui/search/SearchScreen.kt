package com.fubuki.manarabbit.ui.search

import com.fubuki.manarabbit.network.USER_AGENT

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fubuki.manarabbit.data.Manga

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String = "",
    results: List<Manga> = emptyList(),
    isLoading: Boolean = false,
    searched: Boolean = false,
    onQueryChange: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onMangaClick: (Manga) -> Unit = {}
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("검색", style = MaterialTheme.typography.titleLarge) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { onQueryChange(it) },
                placeholder = { Text("만화 제목 검색") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { if (query.isNotEmpty()) onSearch(query) }
                ),
                trailingIcon = {
                    IconButton(onClick = { if (query.isNotEmpty()) onSearch(query) }) {
                        Icon(Icons.Filled.Search, "검색")
                    }
                }
            )

            Spacer(Modifier.height(8.dp))
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                searched && results.isEmpty() -> Text(
                    "검색 결과가 없어요",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(results) { manga ->
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
    }
}
