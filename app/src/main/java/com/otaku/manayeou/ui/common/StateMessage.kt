package com.otaku.manayeou.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// 로딩 완료 후 콘텐츠가 없거나 에러가 난 화면에서 공용으로 쓰는 중앙 메시지 (+선택적 재시도 버튼)
@Composable
fun CenteredMessage(
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    retryLabel: String? = null,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            message,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (retryLabel != null && onRetry != null) {
            Button(onClick = onRetry) { Text(retryLabel) }
        }
    }
}
