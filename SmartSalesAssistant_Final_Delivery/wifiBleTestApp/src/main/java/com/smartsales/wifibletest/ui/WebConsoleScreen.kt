package com.smartsales.wifibletest.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebConsoleScreen(
    viewModel: WifiBleTestViewModel,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val webConsoleUrl = uiState.webConsoleUrl
    val webViewHolder = remember { mutableStateOf<WebView?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val context = LocalContext.current

    BackHandler { onClose() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = webConsoleUrl ?: "Web 控制台",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { webViewHolder.value?.reload() }, enabled = webConsoleUrl != null) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新页面")
                    }
                    IconButton(
                        onClick = {
                            webConsoleUrl?.let { url ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                        enabled = webConsoleUrl != null
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "使用浏览器打开")
                    }
                }
            )
        }
    ) { padding ->
        if (webConsoleUrl.isNullOrBlank()) {
            EmptyConsoleState(modifier = Modifier.padding(padding), onClose = onClose)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading.value = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading.value = false
                                }
                            }
                            loadUrl(webConsoleUrl)
                        }.also { webViewHolder.value = it }
                    },
                    update = { webView ->
                        webViewHolder.value = webView
                        if (webView.url != webConsoleUrl) {
                            webView.loadUrl(webConsoleUrl)
                        }
                    }
                )

                if (isLoading.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyConsoleState(modifier: Modifier = Modifier, onClose: () -> Unit) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "没有可用的 Web 控制台链接",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "请返回上一页并输入有效的 IP/端口后重新打开。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onClose) {
                Text("返回配置")
            }
        }
    }
}
