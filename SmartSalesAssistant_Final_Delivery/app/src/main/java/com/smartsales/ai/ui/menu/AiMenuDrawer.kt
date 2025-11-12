package com.smartsales.ai.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AiMenuDrawer(
    onHistoryClick: () -> Unit,
    onFilesPlaceholderClick: () -> Unit,
    onEditorPlaceholderClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxHeight()
                .fillMaxWidth(0.75f),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(text = "AI 助手", style = MaterialTheme.typography.titleMedium)

            DrawerItem(label = "历史记录", onClick = onHistoryClick)
            DrawerItem(label = "文件文件 (占位)", onClick = onFilesPlaceholderClick)
            DrawerItem(label = "编辑器 (占位)", onClick = onEditorPlaceholderClick)

            Column(modifier = Modifier.weight(1f)) {}

            DrawerItem(label = "用户A", onClick = onProfileClick)
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
    )
}
