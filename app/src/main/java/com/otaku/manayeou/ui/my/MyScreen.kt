package com.otaku.manayeou.ui.my

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private data class MyMenuItem(val title: String, val icon: ImageVector, val count: Int, val category: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    onMoreClick: (String) -> Unit,
    viewModel: MyViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    val menuItems = listOf(
        MyMenuItem("북마크", Icons.Default.Bookmark, state.bookmarked.size, "bookmarked"),
        MyMenuItem("최근 본 만화", Icons.Default.History, state.recent.size, "recent")
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("마이") }) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(menuItems) { menuItem ->
                MyMenuCard(item = menuItem, onClick = { onMoreClick(menuItem.category) })
            }
        }
    }
}

@Composable
private fun MyMenuCard(item: MyMenuItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.aspectRatio(1f)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${item.count}개",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
