package com.fubuki.manarabbit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fubuki.manarabbit.ui.theme.ManaRabbitTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store = remember { SettingsDataStore(this) }
            val theme by store.theme.collectAsState(initial = "system")
            val darkTheme = when (theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            ManaRabbitTheme(darkTheme = darkTheme) {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedManga by remember { mutableStateOf<MangaItem?>(null) }
    var selectedEpisode by remember { mutableStateOf<Triple<Int, String, List<EpisodeItem>>?>(null) }
    var moreListData by remember { mutableStateOf<Pair<String, List<MangaItem>>?>(null) }
    var showCloudflareScreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val baseUrl by store.baseUrl.collectAsState(initial = "")
    var recentListData by remember { mutableStateOf<List<RecentMangaItem>?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    val recentMangaStr by store.recentMangaV2.collectAsState(initial = "")
    var bookmarkListData by remember { mutableStateOf<List<MangaItem>?>(null) }
    val bookmarkStr by store.bookmarkManga.collectAsState(initial = "")

    BackHandler(enabled = selectedEpisode != null) {
        selectedEpisode = null
    }
    BackHandler(enabled = selectedManga != null) {
        selectedManga = null
    }
    BackHandler(enabled = moreListData != null) {
        moreListData = null
    }
    BackHandler(enabled = recentListData != null) {
        recentListData = null
    }
    BackHandler(enabled = showCloudflareScreen) {
        showCloudflareScreen = false
    }
    BackHandler(enabled = bookmarkListData != null) {
        bookmarkListData = null
    }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("접속 오류") },
            text = { Text("콘텐츠를 불러오지 못했습니다.\nCAPTCHA 인증이 필요할 수 있어요.") },
            confirmButton = {
                TextButton(onClick = {
                    showAuthDialog = false
                    showCloudflareScreen = true
                }) {
                    Text("CAPTCHA 인증")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) {
                    Text("닫기")
                }
            }
        )
    }

    // CAPTCHA 인증 다이얼로그
    if (showCloudflareScreen && baseUrl.isNotEmpty()) {
        CloudflareScreen(
            url = baseUrl,
            onCookieReceived = { cookies ->
                showCloudflareScreen = false
                kotlinx.coroutines.GlobalScope.launch {
                    store.saveCfCookies(cookies)
                }
            },
            onBack = { showCloudflareScreen = false }
        )
        return
    }

    // 뷰어 화면
    if (selectedEpisode != null) {
        ViewerScreen(
            episodeId = selectedEpisode!!.first,
            episodeTitle = selectedEpisode!!.second,
            episodeList = selectedEpisode!!.third,
            onBack = { selectedEpisode = null },
            onList = { seriesId ->
                selectedEpisode = null
                selectedManga = MangaItem(seriesId, "", "", "")
            },
            onAuthNeeded = { showAuthDialog = true }
        )
        return
    }

    // 에피소드 목록 화면
    if (selectedManga != null) {
        EpisodeScreen(
            mangaId = selectedManga!!.id,
            mangaName = selectedManga!!.name,
            onBack = { selectedManga = null },
            onEpisodeClick = { id, title, list ->
                selectedEpisode = Triple(id, title, list)
            },
            onAuthNeeded = { showAuthDialog = true }
        )
        return
    }

    if (recentListData != null) {
        RecentListScreen(
            items = recentListData!!,
            onMangaClick = { item ->
                recentListData = null
                selectedManga = MangaItem(item.mangaId, item.mangaName, item.thumb, item.referer)
            },
            onBack = { recentListData = null }
        )
        return
    }

    if (bookmarkListData != null) {
        BookmarkListScreen(
            items = bookmarkListData!!,
            onMangaClick = { item ->
                bookmarkListData = null
                selectedManga = item
            },
            onBack = { bookmarkListData = null }
        )
        return
    }

    // 더보기 화면
    if (moreListData != null) {
        MoreListScreen(
            title = moreListData!!.first,
            items = moreListData!!.second,
            onMangaClick = {
                selectedManga = it
                moreListData = null
            },
            onBack = { moreListData = null }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Home, "홈") },
                    label = { Text("홈") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Search, "검색") },
                    label = { Text("검색") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Person, "마이") },
                    label = { Text("마이") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.Settings, "설정") },
                    label = { Text("설정") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    onMangaClick = { manga ->
                        if (manga.isEpisode) {
                            selectedEpisode = Triple(manga.id, manga.name, emptyList())
                        } else {
                            selectedManga = manga
                        }
                    },
                    onMoreUpdated = { moreListData = Pair("최신 만화", it) },
                    onMoreRecent = { recentListData = it },
                    onAuthNeeded = { showAuthDialog = true }
                )
                1 -> Text("검색 화면")
                2 -> MyScreen(
                    onRecentClick = { recentListData = store.parseRecentMangaList(recentMangaStr) },
                    onFavoriteClick = { bookmarkListData = store.parseMangaList(bookmarkStr) }
                )
                3 -> SettingsScreen(onCfAuthClick = { showCloudflareScreen = true })
            }
        }
    }
}

@Composable
fun SettingsScreen(onCfAuthClick: () -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val savedUrl by store.baseUrl.collectAsState(initial = "")
    var urlInput by remember(savedUrl) { mutableStateOf(savedUrl) }
    val theme by store.theme.collectAsState(initial = "system")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("서버 주소", style = MaterialTheme.typography.labelLarge)

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            placeholder = { Text("예: https://example.com", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = { scope.launch { store.saveBaseUrl(urlInput) } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("저장")
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))

        Text("CAPTCHA", style = MaterialTheme.typography.labelLarge)
        Text(
            "서버 접속이 안될 때 인증해주세요.",
            style = MaterialTheme.typography.bodySmall
        )
        Button(
            onClick = onCfAuthClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = savedUrl.isNotEmpty()
        ) {
            Text("인증")
        }

        if (savedUrl.isEmpty()) {
            Text(
                "서버 주소를 먼저 입력해주세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))

        Text("테마", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "시스템", "light" to "라이트", "dark" to "다크").forEach { (value, label) ->
                FilterChip(
                    selected = theme == value,
                    onClick = { scope.launch { store.saveTheme(value) } },
                    label = { Text(label) }
                )
            }
        }
    }
}