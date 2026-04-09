package com.fubuki.manarabbit.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"

val httpClient = OkHttpClient()

val resolveClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()
