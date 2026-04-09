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
    var urlInput by remember(savedUrl) { mutableStateOf(savedUrl) }
    val theme by store.theme.collectAsState(initial = "system")
    val autoResolve by store.autoResolve.collectAsState(initial = false)
    var showImportSuccess by remember { mutableStateOf(false) }

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

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("주소") },
                placeholder = { Text("예: https://manatoki469.net") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !autoResolve
            )
            Button(
                onClick = { scope.launch { store.saveBaseUrl(urlInput) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !autoResolve
            ) {
                Text("저장")
            }

            // 자동 탐색 토글
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("주소 자동 탐색", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "사이트 번호가 바뀌어도 자동으로 찾아 접속합니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoResolve,
                    onCheckedChange = { enabled ->
                        scope.launch { store.saveAutoResolve(enabled) }
                    }
                )
            }

            if (autoResolve) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "자동 탐색 사용 중",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (savedUrl.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "현재 접속 주소: $savedUrl",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "앱 시작 시 접속 가능한 번호를 자동으로 탐색합니다.\n주소를 한 번만 입력해두면 이후 번호 변경에 자동 대응합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
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
