package com.fubuki.manarabbit.ui.my

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MyScreen(
    onRecentClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("마이", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRecentClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("최근 본 만화", style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Filled.ArrowForward, "이동")
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBookmarkClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("북마크", style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Filled.ArrowForward, "이동")
            }
        }
    }
}