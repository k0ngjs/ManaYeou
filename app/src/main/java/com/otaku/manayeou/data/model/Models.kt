package com.otaku.manayeou.data.model

data class Series(
    val id: String,
    val title: String,
    val author: String = "",
    val coverUrl: String = "",
    val synopsis: String = "",
    val genre: String = "",
    val latestChapter: String = "",
    val sourceUrl: String
)

data class Chapter(
    val id: String,
    val seriesId: String,
    val number: String,
    val title: String,
    val date: String = "",
    val url: String,
    val isRead: Boolean = false
)

data class MangaPage(
    val index: Int,
    val imageUrl: String
)

enum class ViewerMode { WEBTOON, MANGA }

enum class HomeSection(val label: String) {
    LATEST("최신 만화"),
    POPULAR("인기 만화"),
    BOOKMARKED("북마크"),
    RECENT("최근 본 만화")
}
