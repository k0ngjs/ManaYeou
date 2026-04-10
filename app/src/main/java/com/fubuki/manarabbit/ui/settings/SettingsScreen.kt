package com.fubuki.manarabbit.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
    val focusManager = LocalFocusManager.current

    val savedUrl by store.baseUrl.collectAsState(initial = "")
    val theme by store.theme.collectAsState(initial = "system")
    val autoResolve by store.autoResolve.collectAsState(initial = false)
    var showImportSuccess by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var urlInput by remember(savedUrl) { mutableStateOf(savedUrl) }

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
        TopAppBar(
            title = { Text("설정", style = MaterialTheme.typography.titleLarge) },
            actions = {
                IconButton(onClick = { showHelp = true }) {
                    Icon(Icons.Outlined.Help, contentDescription = "도움말")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        if (showHelp) {
            AlertDialog(
                onDismissRequest = { showHelp = false },
                title = { Text("서버 주소 도움말") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("자동 탐색 ON", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(
                            "텔레그램 공지에서 최신 주소를 자동으로 가져옵니다.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        HorizontalDivider()
                        Text("자동 탐색 OFF", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(
                            "주소를 직접 입력합니다.\n예: https://manatoki469.net/",
                            style = MaterialTheme.typography.bodySmall
                        )
                        HorizontalDivider()
                        Text(
                            "접속이 안 될 때는 CAPTCHA 인증을 시도해보세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelp = false }) { Text("확인") }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    onCheckedChange = { scope.launch { store.saveAutoResolve(it) } },
                    colors = SwitchDefaults.colors(
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                )
            }

            // 자동/수동 공통 카드 UI
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (autoResolve) {
                        // 읽기 전용
                        Text(
                            text = if (savedUrl.isNotEmpty()) savedUrl else "앱 시작 시 자동으로 가져옵니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (savedUrl.isNotEmpty())
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // 직접 입력 (키보드 완료로 저장)
                        BasicTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                scope.launch { store.saveBaseUrl(urlInput.trimEnd('/') + "/") }
                                focusManager.clearFocus()
                            }),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (urlInput.isEmpty()) {
                                        Text(
                                            "https://manatoki469.net/",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // CAPTCHA
            HorizontalDivider()

            Text("CAPTCHA", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
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
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.Black
                        )
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
