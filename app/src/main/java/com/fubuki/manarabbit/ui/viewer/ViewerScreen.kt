package com.fubuki.manarabbit.ui.viewer

import com.fubuki.manarabbit.network.USER_AGENT

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Download
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
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fubuki.manarabbit.data.Episode
import com.fubuki.manarabbit.data.RecentManga
import com.fubuki.manarabbit.data.SettingsDataStore
import com.fubuki.manarabbit.network.fetchViewerData
import com.fubuki.manarabbit.network.httpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    episodeId: Int,
    episodeTitle: String,
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
    var loadFailed by remember { mutableStateOf(false) }
    var showBars by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var currentImageIndex by remember { mutableIntStateOf(0) }
    var prevId by remember { mutableIntStateOf(0) }
    var nextId by remember { mutableIntStateOf(0) }
    var seriesMangaId by remember { mutableIntStateOf(0) }
    var savedPage by remember { mutableIntStateOf(0) }

    // 이미지 저장 권한 요청 (Android 9 이하)
    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val url = images.getOrNull(currentImageIndex)
            if (url != null) {
                scope.launch {
                    val ok = saveImageToGallery(context, url, baseUrl)
                    Toast.makeText(
                        context,
                        if (ok) "이미지 저장 완료" else "저장 실패",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(context, "저장 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadCurrentImage() {
        val url = images.getOrNull(currentImageIndex) ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        scope.launch {
            val ok = saveImageToGallery(context, url, baseUrl)
            Toast.makeText(
                context,
                if (ok) "이미지 저장 완료" else "저장 실패",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val hasPrev = prevId > 0
    val hasNext = nextId > 0

    fun loadEpisode(id: Int, title: String, startPage: Int = 0) {
        currentId = id
        currentTitle = title
        images = emptyList()
        isLoading = true
        status = ""
        loadFailed = false
        currentImageIndex = startPage
        savedPage = startPage
        prevId = 0
        nextId = 0
        scope.launch {
            try {
                // fetchViewerData: /comic/{id} 1번만 요청
                val result = fetchViewerData(baseUrl, id, cfCookies)
                if (result.images.isEmpty()) {
                    status = "이미지를 불러오지 못했습니다"
                    loadFailed = true
                    onAuthNeeded()
                } else {

                    images = result.images
                    prevId = result.prevId
                    nextId = result.nextId
                    if (result.seriesId > 0) {
                        seriesMangaId = result.seriesId
                        store.saveRecentManga(
                            RecentManga(
                                mangaId = result.seriesId,
                                mangaName = result.mangaName,
                                thumb = result.thumb,
                                referer = baseUrl,
                                lastEpisodeId = id,
                                lastEpisodeTitle = title,
                                lastPage = startPage
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                status = "이미지를 불러오지 못했습니다"
                loadFailed = true
                onAuthNeeded()
            }
            isLoading = false
        }
    }

    // 인증 완료 후 cfCookies가 업데이트되면 자동으로 재시도
    LaunchedEffect(cfCookies) {
        if (loadFailed && cfCookies.isNotEmpty() && baseUrl.isNotEmpty()) {
            loadEpisode(currentId, currentTitle, currentImageIndex)
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

    LaunchedEffect(pages) {
        if (pages.isNotEmpty() && images.isNotEmpty()) {
            val restoreTo = savedPage.coerceIn(0, images.size - 1)
            val targetImg = images.getOrNull(restoreTo) ?: return@LaunchedEffect
            val targetPage = pages.indexOfFirst { it.contains(targetImg) }.takeIf { it >= 0 } ?: 0
            pagerState.scrollToPage(targetPage)
            if (restoreTo > 0) listState.scrollToItem(restoreTo)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val page = pages.getOrNull(pagerState.currentPage)
        if (page != null) {
            val idx = images.indexOf(page.first())
            if (idx >= 0) currentImageIndex = idx
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (viewerMode == "scroll") {
            currentImageIndex = listState.firstVisibleItemIndex
        }
    }

    val currentPage = currentImageIndex + 1
    val totalPages = images.size

    LaunchedEffect(currentId, baseUrl) {
        if (baseUrl.isNotEmpty()) {
            val recentStr = store.recentManga.first()
            val savedRecentPage = store.parseRecentMangaList(recentStr)
                .find { it.lastEpisodeId == currentId }?.lastPage ?: 0
            loadEpisode(currentId, currentTitle, savedRecentPage)
        }
    }

    // 페이지 변경 시 저장
    LaunchedEffect(currentImageIndex) {
        if (images.isNotEmpty() && seriesMangaId > 0 && currentImageIndex > 0) {
            store.saveRecentMangaPage(seriesMangaId, currentId, currentImageIndex)
        }
    }

    BackHandler { onBack() }

    if (showSettings) {
        ViewerSettingsDialog(store = store, onDismiss = { showSettings = false })
    }

    val bgColor = if (isDark) Color.Black else Color.White

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
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
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            items(images) { imageUrl ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .addHeader("Referer", baseUrl)
                                        .addHeader("User-Agent", USER_AGENT)
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
                                        .addHeader("User-Agent", USER_AGENT)
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
                                                        .addHeader("User-Agent", USER_AGENT)
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
                                                        .addHeader("User-Agent", USER_AGENT)
                                                        .build(),
                                                    contentDescription = null,
                                                    modifier = Modifier.weight(1f).fillMaxHeight(),
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
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "뒤로") }
                },
                actions = {
                    IconButton(
                        onClick = { downloadCurrentImage() },
                        enabled = images.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Download, "저장", tint = Color.White)
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, "설정", tint = Color.White)
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

        if (showBars && (hasPrev || hasNext || seriesMangaId > 0)) {
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
                            onClick = { if (hasPrev) loadEpisode(prevId, "") },
                            enabled = hasPrev
                        ) { Icon(Icons.Filled.ArrowBack, "이전화", tint = Color.White) }
                        IconButton(onClick = {
                            if (onList != null && seriesMangaId > 0) onList(seriesMangaId)
                            else onBack()
                        }) { Icon(Icons.Filled.List, "목록", tint = Color.White) }
                        IconButton(
                            onClick = { if (hasNext) loadEpisode(nextId, "") },
                            enabled = hasNext
                        ) { Icon(Icons.Filled.ArrowForward, "다음화", tint = Color.White) }
                    }
                }
            }
        }
    }
}

private suspend fun saveImageToGallery(context: android.content.Context, imageUrl: String, referer: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(imageUrl)
                .addHeader("Referer", referer)
                .addHeader("User-Agent", USER_AGENT)
                .build()
            val response = httpClient.newCall(request).execute()
            val bytes = response.body?.bytes() ?: return@withContext false
            response.close()

            val filename = "manarabbit_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ManaRabbit")
                }
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
            ) ?: return@withContext false
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            true
        } catch (_: Exception) {
            false
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
            if (rtl) pages.add(listOf(images[i + 1], images[i]))
            else pages.add(listOf(images[i], images[i + 1]))
            i += 2
        } else {
            pages.add(listOf(images[i]))
            i++
        }
    }
    return pages
}

@Composable
fun ViewerSettingsDialog(store: SettingsDataStore, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val viewerMode by store.viewerMode.collectAsState(initial = "scroll")
    val viewerDouble by store.viewerDouble.collectAsState(initial = false)
    val viewerDoubleFirst by store.viewerDoubleFirst.collectAsState(initial = "single")
    val viewerDirection by store.viewerDirection.collectAsState(initial = "ltr")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
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
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.Black
                            )
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
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                )
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
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("닫기")
                }
            }
        }
    }
}
