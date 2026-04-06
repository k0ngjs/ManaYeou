package com.fubuki.manarabbit.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fubuki.manarabbit.data.BackupManager
import com.fubuki.manarabbit.data.SettingsDataStore
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onCfAuthClick: () -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val savedUrl by store.baseUrl.collectAsState(initial = "")
    var urlInput by remember(savedUrl) { mutableStateOf(savedUrl) }
    val theme by store.theme.collectAsState(initial = "system")
    var showExportSuccess by remember { mutableStateOf(false) }
    var showImportSuccess by remember { mutableStateOf(false) }

    val cfCookies by store.cfCookies.collectAsState(initial = "")

    // 내보내기 런처
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val content = BackupManager.exportBackup(store)
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(content.toByteArray())
                }
                showExportSuccess = true
            }
        }
    }

    // 불러오기 런처
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
                    // 앱 재시작
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }) { Text("확인") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("서버 주소", style = MaterialTheme.typography.labelLarge)

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            placeholder = { Text("예: https://example.com", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = { scope.launch { store.saveBaseUrl(urlInput) } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("저장")
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))

        Text("CAPTCHA", style = MaterialTheme.typography.labelLarge)
        Text(
            "서버 접속이 안될 때 인증해주세요.",
            style = MaterialTheme.typography.bodySmall
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

        Spacer(Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))

        Text("테마", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "시스템", "light" to "라이트", "dark" to "다크").forEach { (value, label) ->
                FilterChip(
                    selected = theme == value,
                    onClick = { scope.launch { store.saveTheme(value) } },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))

        Text("데이터 백업", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    exportLauncher.launch(BackupManager.generateFileName())
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("내보내기")
            }
            Button(
                onClick = {
                    importLauncher.launch("*/*")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("불러오기")
            }
        }
    }
}