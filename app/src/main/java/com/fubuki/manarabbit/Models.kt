package com.fubuki.manarabbit

data class MangaItem(
    val id: Int,
    val name: String,
    val thumb: String,
    val referer: String = "",
    val isEpisode: Boolean = false
)

data class RecentMangaItem(
    val mangaId: Int,
    val mangaName: String,
    val thumb: String,
    val referer: String,
    val lastEpisodeId: Int,
    val lastEpisodeTitle: String
)