package com.otaku.manayeou.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

private const val CHANNEL_URL = "https://t.me/s/toonlink11"
private const val TARGET_DOMAIN = "kmana10.net"

suspend fun fetchChannelUrls(): List<String> = withContext(Dispatchers.IO) {
    val request = Request.Builder().url(CHANNEL_URL).build()
    val html = sharedHttpClient.newCall(request).execute().use {
        it.body?.string() ?: return@withContext emptyList()
    }

    val doc = Jsoup.parse(html)
    doc.select("a[href]")
        .map { it.attr("abs:href") }
        .filter { it.contains(TARGET_DOMAIN) }
        .distinct()
}
