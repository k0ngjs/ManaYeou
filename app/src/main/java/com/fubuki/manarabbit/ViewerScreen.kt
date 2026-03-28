package com.fubuki.manarabbit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    episodeId: Int,
    episodeTitle: String,
    episodeList: List<EpisodeItem> = emptyList(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val baseUrl by store.baseUrl.collectAsState(initial = "")
    val cfCookies by store.cfCookies.collectAsState(initial = "")
    val viewerMode by store.viewerMode.collectAsState(initial = "scroll")
    val viewerDouble by store.viewerDouble.collectAsState(initial = false)
    val viewerDoubleFirst by store.viewerDoubleFirst.collectAsState(initial = "single")

    var currentId by remember { mutableIntStateOf(episodeId) }
    var currentTitle by remember { mutableStateOf(episodeTitle) }
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("") }
    var showBars by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val currentIndex = episodeList.indexOfFirst { it.id == currentId }
    val hasPrev = currentIndex < episodeList.size - 1
    val hasNext = currentIndex > 0

    fun loadEpisode(id: Int, title: String) {
        currentId = id
        currentTitle = title
        images = emptyList()
        isLoading = true
        status = ""
        scope.launch {
            val result = fetchViewerImages(baseUrl, id, cfCookies)
            if (result.isEmpty()) status = "이미지를 불러오지 못했습니다"
            else images = result
            isLoading = false
        }
    }

    LaunchedEffect(currentId, baseUrl) {
        if (baseUrl.isNotEmpty()) loadEpisode(currentId, currentTitle)
    }

    if (showSettings) {
        ViewerSettingsDialog(
            store = store,
            onDismiss = { showSettings = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                        val pages = buildPages(images, viewerDouble, viewerDoubleFirst)
                        val pagerState = rememberPagerState(pageCount = { pages.size })
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIndex ->
                            val page = pages[pageIndex]
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
                                Row(modifier = Modifier.fillMaxSize()) {
                                    page.forEach { url ->
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(url)
                                                .addHeader("Referer", baseUrl)
                                                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
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

        if (showBars && episodeList.isNotEmpty()) {
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
                    Text(
                        "${episodeList.size - currentIndex}화 / ${episodeList.size}화",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Row {
                        IconButton(
                            onClick = {
                                if (hasPrev) {
                                    val prev = episodeList[currentIndex + 1]
                                    loadEpisode(prev.id, prev.title)
                                }
                            },
                            enabled = hasPrev
                        ) {
                            Icon(Icons.Filled.ArrowBack, "이전화", tint = Color.White)
                        }
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.List, "목록", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                if (hasNext) {
                                    val next = episodeList[currentIndex - 1]
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
    doubleFirst: String
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
            pages.add(listOf(images[i], images[i + 1]))
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

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("뷰어 설정", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                Text("읽기 방향", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("scroll" to "세로 스크롤", "pager" to "페이지 넘기기").forEach { (value, label) ->
                        FilterChip(
                            selected = viewerMode == value,
                            onClick = { scope.launch { store.saveViewerMode(value) } },
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