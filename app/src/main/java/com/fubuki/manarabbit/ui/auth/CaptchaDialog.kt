package com.fubuki.manarabbit.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fubuki.manarabbit.network.USER_AGENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 마나토끼 숫자 캡챠 다이얼로그.
 * 1. PHPSESSID 세션 초기화
 * 2. 캡챠 이미지 로드
 * 3. 사용자 입력 후 제출
 * 4. 완료 후 onDone 호출
 */
@Composable
fun CaptchaDialog(
    baseUrl: String,
    cookieStr: String,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var answer by remember { mutableStateOf("") }
    var captchaImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var phpSessionId by remember { mutableStateOf("") }
    var retryCount by remember { mutableIntStateOf(0) }

    val client = remember {
        OkHttpClient.Builder().followRedirects(true).build()
    }

    fun buildCookieHeader(extra: String = ""): String {
        val parts = mutableListOf<String>()
        if (cookieStr.isNotEmpty()) parts.add(cookieStr)
        if (extra.isNotEmpty()) parts.add(extra)
        return parts.joinToString("; ")
    }

    LaunchedEffect(retryCount) {
        isLoading = true
        captchaImage = null
        withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')

                // 1. 세션 초기화 → PHPSESSID 획득 (모든 Set-Cookie 헤더 검사)
                val sessionResp = client.newCall(
                    Request.Builder()
                        .url("$cleanUrl/plugin/kcaptcha/kcaptcha_session.php")
                        .post(FormBody.Builder().build())
                        .header("User-Agent", USER_AGENT)
                        .header("Referer", cleanUrl)
                        .header("Cookie", cookieStr)
                        .build()
                ).execute()
                phpSessionId = sessionResp.headers("Set-Cookie")
                    .firstOrNull { it.contains("PHPSESSID=") }
                    ?.let { Regex("PHPSESSID=([^;]+)").find(it)?.groupValues?.get(1) } ?: ""
                sessionResp.close()

                // 2. 캡챠 이미지 로드
                val imgResp = client.newCall(
                    Request.Builder()
                        .url("$cleanUrl/plugin/kcaptcha/kcaptcha_image.php?t=${System.currentTimeMillis()}")
                        .header("User-Agent", USER_AGENT)
                        .header("Referer", cleanUrl)
                        .header("Cookie", buildCookieHeader("PHPSESSID=$phpSessionId"))
                        .build()
                ).execute()
                val bytes = imgResp.body?.bytes()
                imgResp.close()
                if (bytes != null) {
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    captchaImage = bmp?.asImageBitmap()
                }
            } catch (_: Exception) { }
            isLoading = false
        }
    }

    suspend fun submit() {
        if (answer.isBlank()) return
        isSubmitting = true
        withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                client.newCall(
                    Request.Builder()
                        .url("$cleanUrl/bbs/captcha_check.php")
                        .post(
                            FormBody.Builder()
                                .addEncoded("url", cleanUrl)
                                .addEncoded("captcha_key", answer)
                                .build()
                        )
                        .header("User-Agent", USER_AGENT)
                        .header("Referer", cleanUrl)
                        .header("Cookie", buildCookieHeader("PHPSESSID=$phpSessionId"))
                        .build()
                ).execute().close()
            } catch (_: Exception) { }
        }
        isSubmitting = false
        onDone()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CAPTCHA 인증") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    isLoading -> CircularProgressIndicator()
                    captchaImage != null -> Image(
                        bitmap = captchaImage!!,
                        contentDescription = "캡챠 이미지",
                        modifier = Modifier.fillMaxWidth().height(70.dp)
                    )
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("이미지를 불러오지 못했습니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { retryCount++ }) { Text("재시도") }
                    }
                }
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("숫자 입력") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val scope = rememberCoroutineScope()
            Button(
                onClick = { scope.launch { submit() } },
                enabled = answer.isNotBlank() && !isSubmitting
            ) { Text(if (isSubmitting) "인증 중..." else "확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
