package com.smartsales.wifibletest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartsales.business.bluetooth.BleTransportDirection

@Composable
fun NetworkToolsSection(
    gadgetWifiName: String?,
    phoneWifiName: String?,
    networkMatch: Boolean?,
    autoDeviceIp: String,
    isQuerying: Boolean,
    onQueryNetwork: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "网络状态查询",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = buildString {
                    append("HTTP IP (自动): ")
                    append(if (autoDeviceIp.isBlank()) "未知" else autoDeviceIp)
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "WiFi 名称 (设备): ${gadgetWifiName ?: "未知"}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "WiFi 名称 (手机): ${phoneWifiName ?: "未知"}",
                style = MaterialTheme.typography.bodyMedium
            )

            val matchLabel = when (networkMatch) {
                true -> "已在同一网络"
                false -> "网络不一致，请重新发送 WiFi 信息"
                null -> "等待设备回传名称"
            }
            val matchColor = when (networkMatch) {
                true -> MaterialTheme.colorScheme.primary
                false -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = "匹配状态: $matchLabel",
                color = matchColor,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onQueryNetwork,
                    enabled = !isQuerying
                ) {
                    Text(if (isQuerying) "查询中..." else "查询设备网络")
                }
            }

            Text(
                text = "提示: 查询结果会自动更新 HTTP IP，并附带 WiFi 名称 (wifi#address#ip#name)。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun BleTrafficLogSection(
    entries: List<BleTransportLogEntry>
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "BLE 数据窗口",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            val border = "+" + "-".repeat(46) + "+"
            val bottom = border

            Text(border, style = MaterialTheme.typography.bodySmall)
            val lines = entries.take(8)
            if (lines.isEmpty()) {
                val emptyLine = "| " + "无数据".padEnd(44, ' ') + "|"
                Text(emptyLine, style = MaterialTheme.typography.bodySmall)
            } else {
                lines.forEach { entry ->
                    val directionLabel = if (entry.direction == BleTransportDirection.SENT) {
                        "App->Gadget"
                    } else {
                        "Gadget->App"
                    }
                    val content = buildString {
                        append(directionLabel)
                        append(": ")
                        append(entry.textPreview.ifBlank { entry.hexPreview })
                    }.take(44)
                    val padded = content.padEnd(44, ' ')
                    Text(
                        text = "| $padded |",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text(bottom, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Divider()
            Text(
                text = "原始 HEX 会在文本不可见时显示。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
