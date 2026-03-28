package com.fubuki.manarabbit

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudflareScreen(
    url: String,
    onCookieReceived: (Map<String, String>) -> Unit,
    onBack: () -> Unit
) {
    var statusText by remember { mutableStateOf("인증 페이지 로딩 중...") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(statusText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "뒤로")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val cookieStr = CookieManager.getInstance().getCookie(url)
                        if (!cookieStr.isNullOrEmpty()) {
                            val cookieMap = mutableMapOf<String, String>()
                            for (s in cookieStr.split("; ")) {
                                val idx = s.indexOf("=")
                                if (idx > 0) {
                                    cookieMap[s.substring(0, idx).trim()] = s.substring(idx + 1).trim()
                                }
                            }
                            onCookieReceived(cookieMap)
                        }
                    }) {
                        Text("완료")
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = { context ->
                WebView(context).apply {
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.removeAllCookies(null)
                    cookieManager.setAcceptCookie(true)

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, resUrl: String) {
                            statusText = if (resUrl.contains("captcha")) {
                                "캡챠를 완료해주세요"
                            } else {
                                "인증 완료 후 상단 '완료' 버튼을 눌러주세요"
                            }
                            super.onPageFinished(view, resUrl)
                        }
                    }
                    loadUrl(url)
                }
            }
        )
    }
}