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

// 원본 회차 제목에서 헤더 등에 이미 나온 시리즈명만 제거해 한 줄로 표시.
// 번호(number)는 스크래퍼의 인덱스 기반 fallback이라 신뢰도가 낮아, 완전히 빈 경우에만 최후 수단으로 사용.
fun Chapter.displayTitle(seriesTitle: String): String =
    title.let { if (seriesTitle.isNotBlank()) it.replace(seriesTitle, "") else it }
        .trim(' ', '-', '–', '·', ':', '|')
        .ifBlank { "${number}화" }

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
