package com.fubuki.manarabbit.ui.auth

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fubuki.manarabbit.network.USER_AGENT

/**
 * Cloudflare 챌린지 전용 전체화면 WebView.
 * Dialog 없이 직접 전체화면으로 렌더링 (ViewerScreen과 동일한 방식).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareScreen(
    url: String,
    onCookieReceived: (Map<String, String>) -> Unit,
    onBack: () -> Unit
) {
    var completed by remember { mutableStateOf(false) }

    fun finish() {
        if (completed) return
        val cookieStr = CookieManager.getInstance().getCookie(url) ?: return
        val cookieMap = cookieStr.split("; ").mapNotNull { s ->
            val idx = s.indexOf("=")
            if (idx > 0) s.substring(0, idx).trim() to s.substring(idx + 1).trim() else null
        }.toMap()
        if (cookieMap.isNotEmpty()) {
            completed = true
            onCookieReceived(cookieMap)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "닫기")
                }
                Text(
                    text = "클라우드플레어 인증을 완료해주세요",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { finish() }) { Text("완료") }
            }
        }
        HorizontalDivider()

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    CookieManager.getInstance().setAcceptCookie(true)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = USER_AGENT
                    // WebView가 실제로 사용하는 UA를 OkHttp와 동기화
                    // (에뮬레이터 등에서 WebView가 UA를 변형할 수 있음)
                    settings.userAgentString.takeIf { it.isNotEmpty() }?.let { USER_AGENT = it }

                    webViewClient = object : WebViewClient() {
                        private var pageLoadCount = 0

                        override fun onPageFinished(view: WebView, resUrl: String) {
                            // 광고 제거
                            view.evaluateJavascript(
                                """document.querySelectorAll('[class*="id_bbn"]')
                                    .forEach(function(el) { el.style.display='none'; });""",
                                null
                            )
                            pageLoadCount++
                            if (pageLoadCount >= 2) {
                                val cookies = CookieManager.getInstance().getCookie(resUrl) ?: ""
                                if (cookies.contains("cf_clearance")) {
                                    finish()
                                }
                            }
                            super.onPageFinished(view, resUrl)
                        }
                    }
                    loadUrl(url.trimEnd('/') + "/comic")
                }
            }
        )
    }
}
