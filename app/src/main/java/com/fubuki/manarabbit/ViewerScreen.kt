package com.fubuki.manarabbit

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder
import androidx.activity.compose.BackHandler

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    episodeId: Int,
    episodeTitle: String,
    episodeList: List<EpisodeItem> = emptyList(),
    onBack: () -> Unit,
    onList: ((Int) -> Unit)? = null,
    onAuthNeeded: () -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val baseUrl by store.baseUrl.collectAsState(initial = "")
    val cfCookies by store.cfCookies.collectAsState(initial = "")
    val viewerMode by store.viewerMode.collectAsState(initial = "scroll")
    val viewerDouble by store.viewerDouble.collectAsState(initial = false)
    val viewerDoubleFirst by store.viewerDoubleFirst.collectAsState(initial = "single")
    val viewerDirection by store.viewerDirection.collectAsState(initial = "ltr")
    val darkTheme = isSystemInDarkTheme()
    val theme by store.theme.collectAsState(initial = "system")
    val isDark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> darkTheme
    }

    var currentId by remember { mutableIntStateOf(episodeId) }
    var currentTitle by remember { mutableStateOf(episodeTitle) }
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("") }
    var showBars by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var currentImageIndex by remember { mutableIntStateOf(0) }
    var loadedEpisodeList by remember { mutableStateOf(episodeList) }
    var seriesMangaId by remember { mutableIntStateOf(0) }

    val currentIndex = loadedEpisodeList.indexOfFirst { it.id == currentId }
    val hasPrev = currentIndex < loadedEpisodeList.size - 1
    val hasNext = currentIndex > 0

    fun loadEpisode(id: Int, title: String) {
        currentId = id
        currentTitle = title
        images = emptyList()
        isLoading = true
        status = ""
        currentImageIndex = 0
        scope.launch {
            val result = fetchViewerImages(baseUrl, id, cfCookies)
            if (result.isEmpty()) {
                status = "이미지를 불러오지 못했습니다"
                onAuthNeeded()
            } else images = result
            isLoading = false
        }
    }

    val pages = remember(images, viewerDouble, viewerDoubleFirst, viewerDirection) {
        buildPages(images, viewerDouble, viewerDoubleFirst, viewerDirection == "rtl")
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { if (pages.isEmpty()) 1 else pages.size }
    )

    val listState = rememberLazyListState()

    // pages가 바뀔 때 (1페이지↔2페이지 전환 시) 현재 이미지 위치로 이동
    LaunchedEffect(pages) {
        if (pages.isNotEmpty() && images.isNotEmpty()) {
            val targetImg = images.getOrNull(currentImageIndex) ?: return@LaunchedEffect
            val targetPage = pages.indexOfFirst { it.contains(targetImg) }.takeIf { it >= 0 } ?: 0
            pagerState.scrollToPage(targetPage)
        }
    }

    // 페이지 넘길 때 currentImageIndex 업데이트
    LaunchedEffect(pagerState.currentPage) {
        val page = pages.getOrNull(pagerState.currentPage)
        if (page != null) {
            val idx = images.indexOf(page.first())
            if (idx >= 0) currentImageIndex = idx
        }
    }

    // 스크롤 시 currentImageIndex 업데이트
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (viewerMode == "scroll") {
            currentImageIndex = listState.firstVisibleItemIndex
        }
    }

    val currentPage = currentImageIndex + 1
    val totalPages = images.size

    LaunchedEffect(currentId, baseUrl) {
        if (baseUrl.isNotEmpty()) {
            android.util.Log.d("ManaRabbit", "episodeList size: ${loadedEpisodeList.size}")
            loadEpisode(currentId, currentTitle)
            if (loadedEpisodeList.isEmpty()) {
                android.util.Log.d("ManaRabbit", "fetching episode list...")
                scope.launch {
                    val pair = fetchEpisodeListAndSeriesId(baseUrl, currentId, cfCookies)
                    if (pair.second > 0) seriesMangaId = pair.second
                    if (pair.first.isNotEmpty()) {
                        loadedEpisodeList = pair.first
                        val seriesData = fetchEpisodePageData(baseUrl, pair.second, cfCookies)
                        store.saveRecentMangaV2(
                            RecentMangaItem(
                                mangaId = pair.second,
                                mangaName = seriesData.detail.name,
                                thumb = seriesData.detail.thumb,
                                referer = baseUrl,
                                lastEpisodeId = currentId,
                                lastEpisodeTitle = currentTitle
                            )
                        )
                    }
                }
            }
        }
    }

    BackHandler {
        onBack()
    }

    if (showSettings) {
        ViewerSettingsDialog(
            store = store,
            onDismiss = { showSettings = false }
        )
    }

    val bgColor = if (isDark) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { showBars = !showBars }
        ) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                status.isNotEmpty() -> Text(status, modifier = Modifier.align(Alignment.Center))
                else -> {
                    if (viewerMode == "scroll") {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(images) { imageUrl ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .addHeader("Referer", baseUrl)
                                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            reverseLayout = viewerDirection == "rtl",
                            pageSpacing = 0.dp
                        ) { pageIndex ->
                            val page = if (pages.isNotEmpty()) pages[pageIndex] else emptyList()

                            if (page.size == 1) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(page[0])
                                        .addHeader("Referer", baseUrl)
                                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                BoxWithConstraints(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val isLandscape = maxWidth > maxHeight
                                    if (isLandscape) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            page.forEach { url ->
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(url)
                                                        .addHeader("Referer", baseUrl)
                                                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                                                        .build(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxHeight(),
                                                    contentScale = ContentScale.FillHeight
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            page.forEach { url ->
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(url)
                                                        .addHeader("Referer", baseUrl)
                                                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                                                        .build(),
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showBars) {
            TopAppBar(
                title = { Text(currentTitle, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, "설정")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (showBars && loadedEpisodeList.isNotEmpty()) {
            BottomAppBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewerMode == "scroll") {
                        Spacer(Modifier.weight(1f))
                    } else {
                        Text(
                            text = "$currentPage / $totalPages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row {
                        IconButton(
                            onClick = {
                                if (hasPrev) {
                                    val prev = loadedEpisodeList[currentIndex + 1]
                                    loadEpisode(prev.id, prev.title)
                                }
                            },
                            enabled = hasPrev
                        ) {
                            Icon(Icons.Filled.ArrowBack, "이전화", tint = Color.White)
                        }
                        IconButton(onClick = {
                            if (onList != null && seriesMangaId > 0) {
                                onList(seriesMangaId)
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(Icons.Filled.List, "목록", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                if (hasNext) {
                                    val next = loadedEpisodeList[currentIndex - 1]
                                    loadEpisode(next.id, next.title)
                                }
                            },
                            enabled = hasNext
                        ) {
                            Icon(Icons.Filled.ArrowForward, "다음화", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

fun buildPages(
    images: List<String>,
    doubleMode: Boolean,
    doubleFirst: String,
    rtl: Boolean = false
): List<List<String>> {
    if (!doubleMode) return images.map { listOf(it) }
    val pages = mutableListOf<List<String>>()
    var i = 0
    if (doubleFirst == "single" && images.isNotEmpty()) {
        pages.add(listOf(images[0]))
        i = 1
    }
    while (i < images.size) {
        if (i + 1 < images.size) {
            if (rtl) {
                pages.add(listOf(images[i + 1], images[i]))
            } else {
                pages.add(listOf(images[i], images[i + 1]))
            }
            i += 2
        } else {
            pages.add(listOf(images[i]))
            i++
        }
    }
    return pages
}

@Composable
fun ViewerSettingsDialog(
    store: SettingsDataStore,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val viewerMode by store.viewerMode.collectAsState(initial = "scroll")
    val viewerDouble by store.viewerDouble.collectAsState(initial = false)
    val viewerDoubleFirst by store.viewerDoubleFirst.collectAsState(initial = "single")
    val viewerDirection by store.viewerDirection.collectAsState(initial = "ltr")

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("뷰어 설정", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                Text("읽기 방식", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("scroll" to "스크롤 보기", "pager" to "페이지 보기").forEach { (value, label) ->
                        FilterChip(
                            selected = viewerMode == value,
                            onClick = { scope.launch { store.saveViewerMode(value) } },
                            label = { Text(label) }
                        )
                    }
                }

                if (viewerMode == "pager") {
                    Spacer(Modifier.height(16.dp))
                    Text("페이지 방향", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ltr" to "좌에서 우로", "rtl" to "우에서 좌로").forEach { (value, label) ->
                            FilterChip(
                                selected = viewerDirection == value,
                                onClick = { scope.launch { store.saveViewerDirection(value) } },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("2페이지 보기", style = MaterialTheme.typography.labelLarge)
                        Switch(
                            checked = viewerDouble,
                            onCheckedChange = { scope.launch { store.saveViewerDouble(it) } }
                        )
                    }

                    if (viewerDouble) {
                        Spacer(Modifier.height(8.dp))
                        Text("첫 페이지", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("single" to "1장", "double" to "2장").forEach { (value, label) ->
                                FilterChip(
                                    selected = viewerDoubleFirst == value,
                                    onClick = { scope.launch { store.saveViewerDoubleFirst(value) } },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("닫기")
                }
            }
        }
    }
}

suspend fun fetchViewerImages(baseUrl: String, episodeId: Int, cookieStr: String = ""): List<String> {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$cleanUrl/comic/$episodeId")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            response.close()

            val doc = Jsoup.parse(body)
            val result = mutableListOf<String>()

            val viewPaddings = doc.select("div.view-padding")
            if (viewPaddings.size < 2) return@withContext emptyList()

            val script = viewPaddings[1].selectFirst("script")?.data()
                ?: return@withContext emptyList()

            val encodedData = StringBuilder("%")
            for (line in script.split("\n")) {
                if (line.contains("html_data+=")) {
                    val start = line.indexOf('\'') + 1
                    val end = line.lastIndexOf('\'')
                    if (start < end) {
                        encodedData.append(line.substring(start, end).replace(".", "%"))
                    }
                }
            }
            if (encodedData.endsWith("%")) {
                encodedData.deleteCharAt(encodedData.length - 1)
            }

            val imgHtml = URLDecoder.decode(encodedData.toString(), "UTF-8")
            val imgDoc = Jsoup.parse(imgHtml)

            for (img in imgDoc.select("img")) {
                val style = img.attr("style")
                if (style.isNotEmpty()) continue
                var url = ""
                for (attr in img.attributes()) {
                    if (attr.key.contains("data")) {
                        val v = attr.value
                        if (v.isNotEmpty() && !v.contains("blank") && !v.contains("loading")) {
                            url = if (v.startsWith("/")) "$cleanUrl$v" else v
                            break
                        }
                    }
                }
                if (url.isEmpty()) {
                    val src = img.attr("src")
                    if (src.isNotEmpty() && !src.contains("blank") && !src.contains("loading")) {
                        url = if (src.startsWith("/")) "$cleanUrl$src" else src
                    }
                }
                if (url.isNotEmpty()) result.add(url)
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}

suspend fun fetchEpisodeListAndSeriesId(baseUrl: String, episodeId: Int, cookieStr: String = ""): Pair<List<EpisodeItem>, Int> {
    return withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trimEnd('/')
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$cleanUrl/comic/$episodeId")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                .header("Referer", cleanUrl)
                .apply { if (cookieStr.isNotEmpty()) header("Cookie", cookieStr) }
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Pair(emptyList(), 0)
            response.close()

            val doc = Jsoup.parse(body)
            val navbar = doc.selectFirst("div.toon-nav") ?: return@withContext Pair(emptyList(), 0)
            val seriesId = navbar.select("a").last()
                ?.attr("href")?.split("comic/")?.getOrNull(1)
                ?.split("?")?.firstOrNull()
                ?.filter { it.isDigit() }?.toIntOrNull() ?: return@withContext Pair(emptyList(), 0)

            val episodes = fetchEpisodes(cleanUrl, seriesId, cookieStr)
            Pair(episodes, seriesId)
        } catch (e: Exception) {
            Pair(emptyList(), 0)
        }
    }
}