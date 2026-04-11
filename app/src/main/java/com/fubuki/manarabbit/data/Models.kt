package com.fubuki.manarabbit.data

data class Manga(
    val id: Int,
    val name: String,
    val thumb: String,
    val referer: String = "",
    val isEpisode: Boolean = false
)

data class RecentManga(
    val mangaId: Int,
    val mangaName: String,
    val thumb: String,
    val referer: String,
    val lastEpisodeId: Int,
    val lastEpisodeTitle: String,
    val lastPage: Int = 0
)

data class BookmarkedManga(
    val manga: Manga,
    val latestEpisodeId: Int = 0,
    val latestEpisodeTitle: String = "",
    val latestEpisodeDate: String = ""
)

data class Episode(
    val id: Int,
    val title: String,
    val date: String
)

data class MangaInfo(
    val name: String = "",
    val thumb: String = "",
    val author: String = "",
    val tags: List<String> = emptyList(),
    val release: String = ""
)

data class MangaDetail(
    val info: MangaInfo = MangaInfo(),
    val episodes: List<Episode> = emptyList()
)

data class HomeContent(
    val updated: List<Manga> = emptyList(),
    val popular: List<Manga> = emptyList()
)