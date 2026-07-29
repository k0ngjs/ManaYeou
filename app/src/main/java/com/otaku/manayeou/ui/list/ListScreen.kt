package com.otaku.manayeou.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.otaku.manayeou.data.model.Series
import com.otaku.manayeou.ui.common.CenteredMessage
import com.otaku.manayeou.ui.common.SeriesListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    category: String,
    onBack: () -> Unit,
    onSeriesClick: (Series) -> Unit
) {
    val context = LocalContext.current.applicationContext as android.app.Application
    val viewModel: ListViewModel = viewModel(
        key = category,
        factory = viewModelFactory {
            initializer { ListViewModel(context, category) }
        }
    )
    val state by viewModel.state.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (state.selectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedIds.size}개 선택") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "선택 취소")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(state.title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> CenteredMessage(
                    message = state.error!!,
                    isError = true,
                    retryLabel = "다시 시도",
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.align(Alignment.Center)
                )
                state.items.isEmpty() -> CenteredMessage(
                    message = "목록이 비어 있습니다",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn {
                    items(state.items, key = { it.id }) { series ->
                        SeriesListItem(
                            series = series,
                            selectable = state.selectionMode,
                            selected = series.id in state.selectedIds,
                            onClick = {
                                if (state.selectionMode) viewModel.toggleSelection(series.id)
                                else onSeriesClick(series)
                            },
                            onLongClick = if (viewModel.isDeletable) {
                                { viewModel.startSelection(series.id) }
                            } else null
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("삭제") },
            text = { Text("선택한 ${state.selectedIds.size}개 항목을 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelected()
                    showDeleteConfirm = false
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") }
            }
        )
    }
}
