package com.fubuki.manarabbit.ui.auth

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fubuki.manarabbit.network.USER_AGENT

/**
 * Cloudflare 챌린지 전용 WebView.
 * Bootstrap/jQuery 리소스가 로드되면 챌린지 통과로 판단하고 자동으로 닫힘.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareScreen(
    url: String,
    onCookieReceived: (Map<String, String>) -> Unit,
    onBack: () -> Unit
) {
    var statusText by remember { mutableStateOf("클라우드플레어 인증을 완료해주세요") }
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

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
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
                        text = statusText,
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

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ): WebResourceResponse? {
                                // WebView 실제 UA를 OkHttp와 동기화
                                request.requestHeaders["User-Agent"]?.takeIf { it.isNotEmpty() }
                                    ?.let { USER_AGENT = it }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageFinished(view: WebView, resUrl: String) {
                                // 광고 제거
                                view.evaluateJavascript("""
                                    document.querySelectorAll('[class*="id_bbn"]')
                                        .forEach(function(el) { el.style.display='none'; });
                                """.trimIndent(), null)
                                // cf_clearance 쿠키가 생겼을 때만 완료 처리
                                val cookies = CookieManager.getInstance().getCookie(resUrl) ?: ""
                                if (cookies.contains("cf_clearance")) {
                                    finish()
                                }
                                super.onPageFinished(view, resUrl)
                            }
                        }
                        // 만화 목록 페이지로 바로 진입 → CF 챌린지가 더 잘 발동됨
                        loadUrl(url.trimEnd('/') + "/comic")
                    }
                }
            )
        }
    }
}
