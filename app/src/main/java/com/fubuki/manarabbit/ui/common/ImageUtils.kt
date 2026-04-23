package com.fubuki.manarabbit.ui.common

import android.content.Context
import coil.request.ImageRequest
import com.fubuki.manarabbit.network.USER_AGENT

fun mangaImageRequest(context: Context, url: String, referer: String): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .addHeader("Referer", referer)
        .addHeader("User-Agent", USER_AGENT)
        .crossfade(true)
        .build()
