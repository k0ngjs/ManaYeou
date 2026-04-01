package com.fubuki.manarabbit.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    }
}