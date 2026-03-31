package com.fubuki.manarabbit

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareScreen(
    url: String,
    onCookieReceived: (Map<String, String>) -> Unit,
    onBack: () -> Unit
) {
    var statusText by remember { mutableStateOf("인증 페이지 로딩 중...") }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.background
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, "닫기")
                        }
                        Text(
                            statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
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
                    HorizontalDivider()
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)

                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView, resUrl: String) {
                                        statusText = if (resUrl.contains("captcha")) {
                                            "캡챠를 완료해주세요"
                                        } else {
                                            "인증 완료 후 완료 버튼을 눌러주세요"
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
        }
    }
}