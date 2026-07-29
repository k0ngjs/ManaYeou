package com.otaku.manayeou.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otaku.manayeou.BuildConfig
import com.otaku.manayeou.data.local.ThemeMode
import com.otaku.manayeou.data.local.UrlMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val GITHUB_URL = "https://github.com/k0ngjs/ManaYeou"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackup(it) } }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "도움말")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionTitle("콘텐츠 소스")
            ListItem(
                headlineContent = { Text("URL 설정") },
                trailingContent = {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = state.urlMode == UrlMode.AUTO,
                            onClick = { viewModel.setUrlMode(UrlMode.AUTO) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.weight(1f)
                        ) { Text("자동") }
                        SegmentedButton(
                            selected = state.urlMode == UrlMode.MANUAL,
                            onClick = { viewModel.setUrlMode(UrlMode.MANUAL) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.weight(1f)
                        ) { Text("수동") }
                    }
                }
            )
            if (state.urlMode == UrlMode.MANUAL) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.manualBaseUrl,
                        onValueChange = viewModel::onManualUrlChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("https://kmana10.net") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.saveManualUrl() })
                    )
                    TextButton(onClick = { viewModel.saveManualUrl() }) { Text("저장") }
                }
            }

            HorizontalDivider()

            SettingsSectionTitle("화면")
            ListItem(
                headlineContent = { Text("테마") }
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                SegmentedButton(
                    selected = state.themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    modifier = Modifier.weight(1f)
                ) { Text("라이트") }
                SegmentedButton(
                    selected = state.themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    modifier = Modifier.weight(1f)
                ) { Text("시스템") }
                SegmentedButton(
                    selected = state.themeMode == ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    modifier = Modifier.weight(1f)
                ) { Text("다크") }
            }

            HorizontalDivider()

            SettingsSectionTitle("데이터")
            ListItem(
                headlineContent = { Text("백업 내보내기") },
                modifier = Modifier.clickable {
                    val timestamp = SimpleDateFormat("yyMMddHHmm", Locale.getDefault()).format(Date())
                    exportLauncher.launch("ManaYeouBackUp_$timestamp.json")
                }
            )
            ListItem(
                headlineContent = { Text("백업 불러오기") },
                modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json")) }
            )
            ListItem(
                headlineContent = { Text("데이터 삭제") },
                modifier = Modifier.clickable { showDeleteConfirm = true }
            )

            HorizontalDivider()

            SettingsSectionTitle("저장공간")
            ListItem(
                headlineContent = { Text("이미지 캐시 삭제") },
                modifier = Modifier.clickable { viewModel.clearImageCache() }
            )

            Text(
                "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                textAlign = TextAlign.Center
            )
            Text(
                "Github",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(GITHUB_URL) }
                    .padding(top = 2.dp, bottom = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("도움말") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("· 홈에서 만화를 고르고 회차를 선택하면 바로 뷰어로 이동해요.")
                    Text("· 뷰어 상단의 설정 아이콘에서 스크롤/드래그 보기, 읽기 방향, 두 페이지 보기를 바꿀 수 있어요.")
                    Text("· 상세 화면의 북마크 버튼으로 즐겨 보는 만화를 저장하고, 마이 탭에서 모아볼 수 있어요.")
                    Text("· 만화가 안 불러와지면 URL 설정(자동/수동)을 확인해주세요.")
                    Text("· 테마는 앱 전체 밝기 모드를 라이트/시스템/다크 중에서 고르는 설정이에요.")
                    Text("· 백업 내보내기/불러오기로 북마크와 읽기 기록을 파일로 저장하거나 복원할 수 있어요.")
                    Text("· 데이터 삭제는 북마크와 읽기 기록을 모두 지워요. 되돌릴 수 없으니 주의하세요.")
                    Text("· 이미지 캐시 삭제는 저장된 이미지 캐시를 정리해 저장공간을 확보해요.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("확인") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("데이터 삭제") },
            text = { Text("북마크와 읽기 기록을 모두 삭제할까요? 이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showDeleteConfirm = false
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}
