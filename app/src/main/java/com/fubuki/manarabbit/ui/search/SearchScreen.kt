package com.fubuki.manarabbit.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { onQueryChange(it) },
                placeholder = { Text("만화 제목 검색") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (query.isNotEmpty()) onSearch(query)
                    }
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        if (query.isNotEmpty()) onSearch(query)
                    }) {
                        Icon(Icons.Filled.Search, "검색")
                    }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                searched && results.isEmpty() -> Text(
                    "검색 결과가 없어요",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(results) { manga ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMangaClick(manga) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(modifier = Modifier.size(width = 70.dp, height = 95.dp)) {
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
                            Spacer(Modifier.width(12.dp))
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