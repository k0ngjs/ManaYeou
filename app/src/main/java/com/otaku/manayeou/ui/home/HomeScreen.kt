package com.otaku.manayeou.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otaku.manayeou.data.model.Series
import com.otaku.manayeou.ui.common.CenteredMessage
import com.otaku.manayeou.ui.common.SeriesCarouselSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSeriesClick: (Series) -> Unit,
    onMoreClick: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val hasContent = state.latest.isNotEmpty() || state.popular.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ManaYeou") },
                actions = {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(horizontal = 16.dp).size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                // 새로고침 중에도 이미 있던 콘텐츠는 그대로 보여주고, 최초 로딩일 때만 전체 화면 스피너
                state.isLoading && !hasContent -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null && !hasContent -> CenteredMessage(
                    message = state.error!!,
                    isError = true,
                    retryLabel = "다시 시도",
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.align(Alignment.Center)
                )
                !hasContent -> CenteredMessage(
                    message = "콘텐츠가 없습니다",
                    retryLabel = "새로고침",
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        state.error?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        SeriesCarouselSection(
                            title = "최신 만화",
                            items = state.latest,
                            onItemClick = onSeriesClick,
                            onMoreClick = { onMoreClick("latest") }
                        )
                        SeriesCarouselSection(
                            title = "인기 만화",
                            items = state.popular,
                            onItemClick = onSeriesClick,
                            onMoreClick = { onMoreClick("popular") }
                        )
                        SeriesCarouselSection(
                            title = "북마크",
                            items = state.bookmarked,
                            onItemClick = onSeriesClick,
                            onMoreClick = { onMoreClick("bookmarked") }
                        )
                        SeriesCarouselSection(
                            title = "최근 본 만화",
                            items = state.recent,
                            onItemClick = onSeriesClick,
                            onMoreClick = { onMoreClick("recent") }
                        )
                    }
                }
            }
        }
    }
}
