package com.otaku.manayeou.ui.viewer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.otaku.manayeou.data.local.ReadingDirection
import com.otaku.manayeou.data.model.MangaPage
import com.otaku.manayeou.data.model.ViewerMode
import com.otaku.manayeou.data.model.displayTitle

@Composable
fun ViewerScreen(
    chapterUrl: String,
    chapterTitle: String,
    seriesId: String,
    seriesUrl: String,
    seriesTitle: String,
    seriesAuthor: String,
    coverUrl: String,
    onBack: () -> Unit,
    onNavigateChapter: (chapterUrl: String, chapterTitle: String) -> Unit
) {
    val app = LocalContext.current.applicationContext as android.app.Application
    val viewModel: ViewerViewModel = viewModel(
        key = chapterUrl,
        factory = viewModelFactory {
            initializer { ViewerViewModel(app, chapterUrl, chapterTitle, seriesId, seriesUrl, seriesTitle, seriesAuthor, coverUrl) }
        }
    )
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
            state.error != null -> Text(
                state.error!!,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
            else -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black,
                    onClick = { viewModel.toggleControls() },
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) {
                    when (state.mode) {
                        ViewerMode.WEBTOON -> WebtoonViewer(
                            pages = state.pages,
                            onPageVisible = { viewModel.onPageChanged(it) }
                        )
                        ViewerMode.MANGA -> MangaViewer(
                            pages = state.pages,
                            reverseLayout = state.readingDirection == ReadingDirection.RTL,
                            twoPageMode = state.twoPageMode,
                            firstPageSingle = state.firstPageSingle,
                            onPageChanged = { viewModel.onPageChanged(it) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.showControls && !state.isLoading,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ViewerTopBar(
                title = chapterTitle,
                onBack = onBack,
                onSettingsClick = { viewModel.toggleSettings() }
            )
        }

        AnimatedVisibility(
            visible = state.showControls && !state.isLoading && state.pages.isNotEmpty(),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ViewerBottomBar(
                current = state.currentPage + 1,
                total = state.pages.size,
                prevEnabled = state.prevChapter != null,
                nextEnabled = state.nextChapter != null,
                onPrev = { state.prevChapter?.let { onNavigateChapter(it.url, it.displayTitle(seriesTitle)) } },
                onNext = { state.nextChapter?.let { onNavigateChapter(it.url, it.displayTitle(seriesTitle)) } }
            )
        }
    }

    if (state.showSettings) {
        ViewerSettingsSheet(
            state = state,
            onDismiss = { viewModel.toggleSettings() },
            onModeChange = viewModel::setMode,
            onReadingDirectionChange = viewModel::setReadingDirection,
            onTwoPageModeChange = viewModel::setTwoPageMode,
            onFirstPageSingleChange = viewModel::setFirstPageSingle
        )
    }
}

@Composable
private fun WebtoonViewer(pages: List<MangaPage>, onPageVisible: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(pages) { index, page ->
            ZoomableImage(
                imageUrl = page.imageUrl,
                modifier = Modifier.fillMaxWidth()
            )
            SideEffect { onPageVisible(index) }
        }
    }
}

// 두 페이지씩 볼 때 화면 하나에 들어갈 원본 페이지 인덱스 묶음("스프레드") 계산
private fun buildSpreads(pageCount: Int, twoPageMode: Boolean, firstPageSingle: Boolean): List<List<Int>> {
    if (!twoPageMode || pageCount == 0) return (0 until pageCount).map { listOf(it) }
    val spreads = mutableListOf<List<Int>>()
    var i = 0
    if (firstPageSingle) {
        spreads.add(listOf(0))
        i = 1
    }
    while (i < pageCount) {
        spreads.add(if (i + 1 < pageCount) listOf(i, i + 1) else listOf(i))
        i += 2
    }
    return spreads
}

@Composable
private fun MangaViewer(
    pages: List<MangaPage>,
    reverseLayout: Boolean,
    twoPageMode: Boolean,
    firstPageSingle: Boolean,
    onPageChanged: (Int) -> Unit
) {
    val spreads = remember(pages.size, twoPageMode, firstPageSingle) {
        buildSpreads(pages.size, twoPageMode, firstPageSingle)
    }
    val pagerState = rememberPagerState(pageCount = { spreads.size })
    LaunchedEffect(pagerState.currentPage, spreads) {
        spreads.getOrNull(pagerState.currentPage)?.firstOrNull()?.let { onPageChanged(it) }
    }

    HorizontalPager(state = pagerState, reverseLayout = reverseLayout, modifier = Modifier.fillMaxSize()) { pagerIndex ->
        val spread = spreads[pagerIndex]
        if (spread.size == 1) {
            ZoomableImage(
                imageUrl = pages[spread[0]].imageUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            val ordered = if (reverseLayout) spread.reversed() else spread
            Row(modifier = Modifier.fillMaxSize()) {
                ordered.forEach { pageIndex ->
                    ZoomableImage(
                        imageUrl = pages[pageIndex].imageUrl,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ZoomableImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillWidth
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .addHeader("Referer", "https://kmana10.net/")
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            // 확대되지 않은 상태의 팬 제스처는 소비하지 않고 상위 HorizontalPager로 넘겨 페이지 스와이프가 되게 함
            .transformable(state = transformableState, canPan = { scale > 1f })
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerTopBar(
    title: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = { Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "뷰어 설정", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.7f))
    )
}

@Composable
private fun ViewerBottomBar(
    current: Int,
    total: Int,
    prevEnabled: Boolean,
    nextEnabled: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, enabled = prevEnabled) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "이전화",
                tint = if (prevEnabled) Color.White else Color.Gray
            )
        }
        Text("$current / $total", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onNext, enabled = nextEnabled) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "다음화",
                tint = if (nextEnabled) Color.White else Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerSettingsSheet(
    state: ViewerUiState,
    onDismiss: () -> Unit,
    onModeChange: (ViewerMode) -> Unit,
    onReadingDirectionChange: (ReadingDirection) -> Unit,
    onTwoPageModeChange: (Boolean) -> Unit,
    onFirstPageSingleChange: (Boolean) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("뷰어 설정", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(20.dp))

            Text("보기 모드", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.mode == ViewerMode.WEBTOON,
                    onClick = { onModeChange(ViewerMode.WEBTOON) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("스크롤") }
                SegmentedButton(
                    selected = state.mode == ViewerMode.MANGA,
                    onClick = { onModeChange(ViewerMode.MANGA) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("드래그") }
            }

            if (state.mode == ViewerMode.MANGA) {
                Spacer(Modifier.height(20.dp))
                Text("읽기 방향", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.readingDirection == ReadingDirection.LTR,
                        onClick = { onReadingDirectionChange(ReadingDirection.LTR) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("좌→우") }
                    SegmentedButton(
                        selected = state.readingDirection == ReadingDirection.RTL,
                        onClick = { onReadingDirectionChange(ReadingDirection.RTL) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("우→좌") }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("두 페이지씩 보기", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = state.twoPageMode, onCheckedChange = onTwoPageModeChange)
                }

                if (state.twoPageMode) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "첫 페이지(표지)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = state.firstPageSingle,
                            onClick = { onFirstPageSingleChange(true) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("한 페이지만") }
                        SegmentedButton(
                            selected = !state.firstPageSingle,
                            onClick = { onFirstPageSingleChange(false) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("두 페이지") }
                    }
                }
            }
        }
    }
}
