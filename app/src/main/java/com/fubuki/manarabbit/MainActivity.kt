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
import com.fubuki.manarabbit.data.Manga
import com.fubuki.manarabbit.data.RecentManga
import com.fubuki.manarabbit.data.HomeContent
import com.fubuki.manarabbit.data.SettingsDataStore
import com.fubuki.manarabbit.network.fetchHomeContent
import com.fubuki.manarabbit.network.fetchUrlFromTelegram
import com.fubuki.manarabbit.network.searchManga
import com.fubuki.manarabbit.ui.auth.CaptchaDialog
import com.fubuki.manarabbit.ui.auth.CloudflareScreen
import com.fubuki.manarabbit.ui.episode.EpisodeScreen
import com.fubuki.manarabbit.ui.home.HomeScreen
import com.fubuki.manarabbit.ui.list.BookmarkListScreen
import com.fubuki.manarabbit.ui.list.RecentListScreen
import com.fubuki.manarabbit.ui.my.MyScreen
import com.fubuki.manarabbit.ui.search.SearchScreen
import com.fubuki.manarabbit.ui.settings.SettingsScreen
import com.fubuki.manarabbit.ui.theme.ManaRabbitTheme
import com.fubuki.manarabbit.ui.list.UpdateListScreen
import com.fubuki.manarabbit.ui.viewer.ViewerScreen
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
    var selectedManga by remember { mutableStateOf<Manga?>(null) }
    var selectedEpisode by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var updateListData by remember { mutableStateOf<Pair<String, List<Manga>>?>(null) }
    var showCloudflareScreen by remember { mutableStateOf(false) }
    var showCaptchaDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    // CF 완료 시 쿠키를 바로 CaptchaDialog에 전달 (DataStore 업데이트 대기 불필요)
    var pendingCfCookies by remember { mutableStateOf("") }
    // CAPTCHA 완료 시 뷰어에 재로드 신호
    var viewerAuthTrigger by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val baseUrl by store.baseUrl.collectAsState(initial = "")
    val cfCookies by store.cfCookies.collectAsState(initial = "")
    var recentListData by remember { mutableStateOf<List<RecentManga>?>(null) }
    var bookmarkListData by remember { mutableStateOf<List<Manga>?>(null) }
    val recentMangaStr by store.recentManga.collectAsState(initial = "")
    val bookmarkStr by store.bookmarkManga.collectAsState(initial = "")
    val autoResolve by store.autoResolve.collectAsState(initial = false)

    var homeContent by remember { mutableStateOf(HomeContent()) }
    var homeLoading by remember { mutableStateOf(true) }
    var homeStatus by remember { mutableStateOf("") }
    var homeLoaded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val homeContentCache by store.homeContent.collectAsState(initial = "")

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var searchSearched by remember { mutableStateOf(false) }
    var cachedMangaId by remember { mutableIntStateOf(0) }
    var cachedMangaDetail by remember { mutableStateOf(com.fubuki.manarabbit.data.MangaDetail()) }
    var cachedBookmarkItems by remember { mutableStateOf<List<com.fubuki.manarabbit.data.BookmarkedManga>>(emptyList()) }

    val scope = rememberCoroutineScope()

    val selectTab: (Int) -> Unit = { tab ->
        selectedTab = tab
        selectedManga = null
        updateListData = null
        recentListData = null
        bookmarkListData = null
    }

    // 인증 진행 중이면 오류 다이얼로그 억제
    fun showAuthIfNeeded() {
        if (!showCloudflareScreen && !showCaptchaDialog) showAuthDialog = true
    }

    suspend fun loadHomeContent(forceRefresh: Boolean = false) {
        // 캐시가 있고 강제 새로고침이 아니면 캐시를 즉시 표시하고 백그라운드 갱신 생략
        if (!forceRefresh && homeLoaded) return
        if (!forceRefresh) {
            // 캐시된 홈 콘텐츠가 있으면 즉시 표시
            val cached = store.parseHomeContent(homeContentCache)
            if (cached.updated.isNotEmpty()) {
                homeContent = cached
                homeLoaded = true
                homeLoading = false
                return
            }
        }
        if (!homeLoaded) homeLoading = true
        homeStatus = ""
        try {
            val result = fetchHomeContent(baseUrl, cfCookies)
            if (result.updated.isEmpty() && result.popular.isEmpty()) {
                if (homeLoaded) {
                    scope.launch { snackbarHostState.showSnackbar("목록을 불러오지 못했습니다") }
                } else {
                    homeStatus = "목록을 불러오지 못했습니다"
                    showAuthIfNeeded()
                }
            } else {
                homeContent = result
                homeLoaded = true
                scope.launch { store.saveHomeContent(result) }
            }
        } catch (e: Exception) {
            if (!homeLoaded) {
                homeStatus = "목록을 불러오지 못했습니다"
                showAuthIfNeeded()
            }
        }
        homeLoading = false
    }

    // 자동 주소 탐색: baseUrl 혹은 autoResolve 설정이 바뀔 때 실행
    LaunchedEffect(baseUrl, autoResolve) {
        if (!autoResolve) {
            // 수동 모드: 저장된 URL 그대로 사용
            if (baseUrl.isEmpty()) {
                homeLoading = false
                homeStatus = "설정에서 서버 주소를 입력해주세요"
            } else {
                loadHomeContent()
            }
            return@LaunchedEffect
        }

        // 자동 모드: 텔레그램에서 최신 주소 가져오기
        homeLoading = true
        homeStatus = ""
        val telegramUrl = fetchUrlFromTelegram()
        if (telegramUrl != null) {
            if (telegramUrl != baseUrl) {
                // 새 주소 저장 → LaunchedEffect 재실행
                store.saveBaseUrl(telegramUrl)
                return@LaunchedEffect
            }
            // 이미 최신 주소 → 바로 로드
            loadHomeContent()
            return@LaunchedEffect
        }

        // 텔레그램 실패
        homeLoading = false
        homeStatus = "주소를 가져오지 못했습니다.\n네트워크 상태를 확인해주세요."
    }

    BackHandler(enabled = selectedEpisode != null) { selectedEpisode = null }
    BackHandler(enabled = selectedManga != null) { selectedManga = null }
    BackHandler(enabled = updateListData != null) { updateListData = null }
    BackHandler(enabled = recentListData != null) { recentListData = null }
    BackHandler(enabled = bookmarkListData != null) { bookmarkListData = null }
    BackHandler(enabled = showCloudflareScreen) { showCloudflareScreen = false }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("접속 오류") },
            text = { Text("콘텐츠를 불러오지 못했습니다.\nCAPTCHA 인증이 필요할 수 있어요.") },
            confirmButton = {
                TextButton(onClick = {
                    showAuthDialog = false
                    showCloudflareScreen = true
                }) { Text("CAPTCHA 인증") }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) { Text("닫기") }
            }
        )
    }

    if (showCloudflareScreen && baseUrl.isNotEmpty()) {
        CloudflareScreen(
            url = baseUrl,
            onCookieReceived = { cookies ->
                showCloudflareScreen = false
                // 쿠키를 로컬에 바로 저장 후 DataStore에도 저장 (순서 보장)
                pendingCfCookies = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                scope.launch {
                    store.saveCfCookies(cookies)
                    showCaptchaDialog = true
                }
            },
            onBack = { showCloudflareScreen = false }
        )
        return
    }

    if (showCaptchaDialog && baseUrl.isNotEmpty()) {
        CaptchaDialog(
            baseUrl = baseUrl,
            cookieStr = pendingCfCookies.ifEmpty { cfCookies },
            onDone = {
                showCaptchaDialog = false
                viewerAuthTrigger++
                scope.launch { loadHomeContent() }
            },
            onDismiss = { showCaptchaDialog = false }
        )
    }

    if (selectedEpisode != null) {
        ViewerScreen(
            episodeId = selectedEpisode!!.first,
            episodeTitle = selectedEpisode!!.second,
            authTrigger = viewerAuthTrigger,
            onBack = { selectedEpisode = null },
            onList = { seriesId ->
                selectedEpisode = null
                selectedManga = Manga(seriesId, "", "", "")
            },
            onAuthNeeded = { showAuthIfNeeded() }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectTab(0) },
                    icon = { Icon(Icons.Filled.Home, "홈", modifier = Modifier.size(22.dp)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectTab(1) },
                    icon = { Icon(Icons.Filled.Search, "검색", modifier = Modifier.size(22.dp)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectTab(2) },
                    icon = { Icon(Icons.Filled.Person, "마이", modifier = Modifier.size(22.dp)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectTab(3) },
                    icon = { Icon(Icons.Filled.Settings, "설정", modifier = Modifier.size(22.dp)) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                selectedManga != null -> EpisodeScreen(
                    mangaId = selectedManga!!.id,
                    mangaName = selectedManga!!.name,
                    cachedDetail = if (cachedMangaId == selectedManga!!.id) cachedMangaDetail else com.fubuki.manarabbit.data.MangaDetail(),
                    onDetailLoaded = { detail ->
                        cachedMangaId = selectedManga!!.id
                        cachedMangaDetail = detail
                    },
                    onBack = { selectedManga = null },
                    onEpisodeClick = { id, title ->
                        selectedEpisode = Pair(id, title)
                    },
                    onAuthNeeded = { showAuthIfNeeded() }
                )
                updateListData != null -> UpdateListScreen(
                    title = updateListData!!.first,
                    items = updateListData!!.second,
                    onMangaClick = {
                        selectedManga = it
                        updateListData = null
                    },
                    onBack = { updateListData = null }
                )
                recentListData != null -> RecentListScreen(
                    items = recentListData!!,
                    onMangaClick = { manga ->
                        selectedManga = Manga(manga.mangaId, manga.mangaName, manga.thumb, manga.referer)
                    },
                    onDeleteItems = { toDelete ->
                        val updated = recentListData!!.filter { it !in toDelete }
                        scope.launch { store.saveRecentMangaList(updated) }
                        recentListData = updated
                    },
                    onBack = { recentListData = null }
                )
                bookmarkListData != null -> BookmarkListScreen(
                    items = bookmarkListData!!,
                    cachedItems = cachedBookmarkItems,
                    onItemsLoaded = { cachedBookmarkItems = it },
                    onMangaClick = { manga -> selectedManga = manga },
                    onDeleteItems = { toDelete ->
                        val updated = bookmarkListData!!.filter { it !in toDelete }
                        scope.launch { store.saveBookmarkList(updated) }
                        bookmarkListData = updated
                        cachedBookmarkItems = cachedBookmarkItems.filter { it.manga !in toDelete }
                    },
                    onBack = { bookmarkListData = null }
                )
                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectedTab) {
                        0 -> HomeScreen(
                            homeContent = homeContent,
                            isLoading = homeLoading,
                            status = homeStatus,
                            onRefresh = { loadHomeContent(forceRefresh = true) },
                            onMangaClick = { manga ->
                                if (manga.isEpisode) {
                                    selectedEpisode = Pair(manga.id, manga.name)
                                } else {
                                    selectedManga = manga
                                }
                            },
                            onMoreUpdated = { updateListData = Pair("최신 만화", it) },
                            onMoreRecent = { recentListData = store.parseRecentMangaList(recentMangaStr) },
                            onMoreBookmark = { bookmarkListData = store.parseBookmarkList(bookmarkStr) },
                            onAuthNeeded = { showAuthIfNeeded() }
                        )
                        1 -> SearchScreen(
                            query = searchQuery,
                            results = searchResults,
                            isLoading = searchLoading,
                            searched = searchSearched,
                            onQueryChange = { searchQuery = it },
                            onSearch = { query ->
                                searchLoading = true
                                searchSearched = true
                                scope.launch {
                                    try {
                                        searchResults = searchManga(baseUrl, query, cfCookies)
                                    } catch (e: Exception) {
                                        showAuthIfNeeded()
                                    }
                                    searchLoading = false
                                }
                            },
                            onMangaClick = { manga -> selectedManga = manga }
                        )
                        2 -> MyScreen(
                            onRecentClick = { recentListData = store.parseRecentMangaList(recentMangaStr) },
                            onBookmarkClick = { bookmarkListData = store.parseBookmarkList(bookmarkStr) }
                        )
                        3 -> SettingsScreen(onCfAuthClick = { showCloudflareScreen = true })
                    }
                }
            }
        }
    }
}
