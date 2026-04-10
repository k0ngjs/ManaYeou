package com.fubuki.manarabbit.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fubuki.manarabbit.data.BackupManager
import com.fubuki.manarabbit.data.SettingsDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onCfAuthClick: () -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val savedUrl by store.baseUrl.collectAsState(initial = "")
    val theme by store.theme.collectAsState(initial = "system")
    val autoResolve by store.autoResolve.collectAsState(initial = false)
    var showImportSuccess by remember { mutableStateOf(false) }

    // 자동 모드일 땐 저장된 URL에서 숫자를 제거해 보여줌 (예: manatoki470.net → manatoki.net)
    val displayUrl = if (autoResolve) stripNumberFromUrl(savedUrl) else savedUrl
    var urlInput by remember(displayUrl) { mutableStateOf(displayUrl) }

    val cfCookies by store.cfCookies.collectAsState(initial = "")

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val content = BackupManager.exportBackup(store)
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(content.toByteArray())
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                BackupManager.importBackup(context, uri, store, savedUrl, cfCookies) {
                    showImportSuccess = true
                }
            }
        }
    }

    if (showImportSuccess) {
        AlertDialog(
            onDismissRequest = { showImportSuccess = false },
            title = { Text("완료") },
            text = { Text("데이터를 불러왔어요. 앱을 재시작해주세요.") },
            confirmButton = {
                TextButton(onClick = {
                    showImportSuccess = false
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }) { Text("확인") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("설정", style = MaterialTheme.typography.titleLarge) })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 서버 주소
            Text("서버 주소", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)

            // 자동 / 수동 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("자동 탐색", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = autoResolve,
                    onCheckedChange = { scope.launch { store.saveAutoResolve(it) } }
                )
            }

            // 모드별 안내 + 입력 필드
            Text(
                if (autoResolve)
                    "숫자를 제외한 기본 주소를 입력하세요.\n번호가 바뀌어도 자동으로 찾아 접속합니다."
                else
                    "접속할 전체 주소를 입력하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text(if (autoResolve) "기본 주소 (숫자 제외)" else "서버 주소") },
                placeholder = {
                    Text(if (autoResolve) "https://manatoki.net/" else "https://manatoki469.net/")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = { scope.launch { store.saveBaseUrl(urlInput.trimEnd('/') + "/") } },
                modifier = Modifier.fillMaxWidth(),
                enabled = urlInput.isNotBlank()
            ) {
                Text("저장")
            }

            // CAPTCHA
            HorizontalDivider()

            Text("CAPTCHA", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            Text(
                "서버 접속이 안될 때 인증해주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onCfAuthClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = savedUrl.isNotEmpty()
            ) {
                Text("인증")
            }
            if (savedUrl.isEmpty()) {
                Text(
                    "서버 주소를 먼저 입력해주세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // 테마
            HorizontalDivider()

            Text("테마", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "시스템", "light" to "라이트", "dark" to "다크").forEach { (value, label) ->
                    FilterChip(
                        selected = theme == value,
                        onClick = { scope.launch { store.saveTheme(value) } },
                        label = { Text(label) }
                    )
                }
            }

            // 데이터 백업
            HorizontalDivider()

            Text("데이터 백업", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { exportLauncher.launch(BackupManager.generateFileName()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("내보내기")
                }
                Button(
                    onClick = { importLauncher.launch("*/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("불러오기")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 저장된 URL에서 숫자 부분을 제거해 기본 주소만 반환. (예: https://manatoki470.net/ → https://manatoki.net/) */
private fun stripNumberFromUrl(url: String): String {
    return try {
        val trimmed = url.trimEnd('/')
        val parsed = java.net.URL(trimmed)
        val host = parsed.host
        val stripped = host.replace(Regex("""^([a-zA-Z0-9\-]+?)\d+(\.[a-zA-Z.]+)$"""), "$1$2")
        if (stripped != host) "${parsed.protocol}://$stripped/" else "$trimmed/"
    } catch (_: Exception) {
        url
    }
}
