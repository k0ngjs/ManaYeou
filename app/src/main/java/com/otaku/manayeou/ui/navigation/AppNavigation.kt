package com.otaku.manayeou.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.otaku.manayeou.ui.detail.DetailScreen
import com.otaku.manayeou.ui.home.HomeScreen
import com.otaku.manayeou.ui.list.ListScreen
import com.otaku.manayeou.ui.my.MyScreen
import com.otaku.manayeou.ui.search.SearchScreen
import com.otaku.manayeou.ui.settings.SettingsScreen
import com.otaku.manayeou.ui.viewer.ViewerScreen
import java.net.URLDecoder
import java.net.URLEncoder

private data class TabItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabItems = listOf(
    TabItem("home", "홈", Icons.Default.Home),
    TabItem("search", "검색", Icons.Default.Search),
    TabItem("my", "마이", Icons.Default.Person),
    TabItem("settings", "설정", Icons.Default.Settings)
)

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabItems.any { it.route == currentRoute }

    Scaffold(
        // 내부 화면들이 각자 자기 Scaffold로 상태바/네비게이션바 인셋을 처리하므로,
        // 바깥 Scaffold는 탭 바 높이만큼만 공간을 차지하고 시스템 인셋은 중복으로 소비하지 않음
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabItems.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(bottom = scaffoldPadding.calculateBottomPadding())
        ) {

            composable("home") {
                HomeScreen(
                    onSeriesClick = { series ->
                        val encoded = URLEncoder.encode(series.sourceUrl, "UTF-8")
                        nav.navigate("detail/$encoded")
                    },
                    onMoreClick = { category ->
                        nav.navigate("list/$category")
                    }
                )
            }

            composable("search") {
                SearchScreen(
                    onSeriesClick = { series ->
                        val encoded = URLEncoder.encode(series.sourceUrl, "UTF-8")
                        nav.navigate("detail/$encoded")
                    }
                )
            }

            composable("my") {
                MyScreen(
                    onMoreClick = { category ->
                        nav.navigate("list/$category")
                    }
                )
            }

            composable("settings") {
                SettingsScreen()
            }

            composable(
                "list/{category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { back ->
                val category = back.arguments?.getString("category") ?: return@composable
                ListScreen(
                    category = category,
                    onBack = { nav.popBackStack() },
                    onSeriesClick = { series ->
                        val encoded = URLEncoder.encode(series.sourceUrl, "UTF-8")
                        nav.navigate("detail/$encoded")
                    }
                )
            }

            composable(
                "detail/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { back ->
                val url = URLDecoder.decode(back.arguments?.getString("url") ?: "", "UTF-8")
                DetailScreen(
                    seriesUrl = url,
                    onBack = { nav.popBackStack() },
                    onChapterClick = { chapter, series ->
                        val encodedChapterUrl = URLEncoder.encode(chapter.url, "UTF-8")
                        val titleEncoded = URLEncoder.encode(chapter.title.ifBlank { "${chapter.number}화" }, "UTF-8")
                        val seriesIdEncoded = URLEncoder.encode(series.id, "UTF-8")
                        val seriesUrlEncoded = URLEncoder.encode(series.sourceUrl, "UTF-8")
                        val seriesTitleEncoded = URLEncoder.encode(series.title, "UTF-8")
                        val coverUrlEncoded = URLEncoder.encode(series.coverUrl, "UTF-8")
                        nav.navigate("viewer/$encodedChapterUrl/$titleEncoded/$seriesIdEncoded/$seriesUrlEncoded/$seriesTitleEncoded/$coverUrlEncoded")
                    }
                )
            }

            composable(
                "viewer/{chapterUrl}/{title}/{seriesId}/{seriesUrl}/{seriesTitle}/{coverUrl}",
                arguments = listOf(
                    navArgument("chapterUrl") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                    navArgument("seriesId") { type = NavType.StringType },
                    navArgument("seriesUrl") { type = NavType.StringType },
                    navArgument("seriesTitle") { type = NavType.StringType },
                    navArgument("coverUrl") { type = NavType.StringType }
                )
            ) { back ->
                val chapterUrl = URLDecoder.decode(back.arguments?.getString("chapterUrl") ?: "", "UTF-8")
                val title = URLDecoder.decode(back.arguments?.getString("title") ?: "", "UTF-8")
                val seriesId = URLDecoder.decode(back.arguments?.getString("seriesId") ?: "", "UTF-8")
                val seriesUrl = URLDecoder.decode(back.arguments?.getString("seriesUrl") ?: "", "UTF-8")
                val seriesTitle = URLDecoder.decode(back.arguments?.getString("seriesTitle") ?: "", "UTF-8")
                val coverUrl = URLDecoder.decode(back.arguments?.getString("coverUrl") ?: "", "UTF-8")
                ViewerScreen(
                    chapterUrl = chapterUrl,
                    chapterTitle = title,
                    seriesId = seriesId,
                    seriesUrl = seriesUrl,
                    seriesTitle = seriesTitle,
                    coverUrl = coverUrl,
                    onBack = { nav.popBackStack() },
                    onNavigateChapter = { newChapterUrl, newTitle ->
                        val encodedChapterUrl = URLEncoder.encode(newChapterUrl, "UTF-8")
                        val titleEncoded = URLEncoder.encode(newTitle, "UTF-8")
                        val seriesIdEncoded = URLEncoder.encode(seriesId, "UTF-8")
                        val seriesUrlEncoded = URLEncoder.encode(seriesUrl, "UTF-8")
                        val seriesTitleEncoded = URLEncoder.encode(seriesTitle, "UTF-8")
                        val coverUrlEncoded = URLEncoder.encode(coverUrl, "UTF-8")
                        // 챕터를 계속 넘기면 뒤로가기 스택이 쌓이지 않도록 현재 뷰어를 교체
                        nav.popBackStack()
                        nav.navigate("viewer/$encodedChapterUrl/$titleEncoded/$seriesIdEncoded/$seriesUrlEncoded/$seriesTitleEncoded/$coverUrlEncoded")
                    }
                )
            }
        }
    }
}
