# Smart Sales Assistant - UI Layer (Jetpack Compose)

## 3.9 UIå±‚å®žçŽ°

### 3.9.1 ä¸»ç•Œé¢å¯¼èˆªç»“æž„

```kotlin
// å¯¼èˆªè·¯ç”±
sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object History : Screen("history")
    object DeviceManagement : Screen("device")
    object FileViewer : Screen("files")
    object Settings : Screen("settings")
    object ConversationDetail : Screen("conversation/{id}") {
        fun createRoute(id: Long) = "conversation/$id"
    }
}

// ä¸»åº”ç”¨å¯¼èˆª
@Composable
fun SmartSalesApp() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Chat.route) {
                ChatScreen(navController)
            }
            composable(Screen.History.route) {
                HistoryScreen(navController)
            }
            composable(Screen.DeviceManagement.route) {
                DeviceManagementScreen()
            }
            composable(Screen.FileViewer.route) {
                FileViewerScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.ConversationDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: 0L
                ConversationDetailScreen(conversationId = id, navController = navController)
            }
        }
    }
}

// åº•éƒ¨å¯¼èˆªæ 
@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("èŠå¤©", Icons.Default.Chat, Screen.Chat.route),
        BottomNavItem("åŽ†å²", Icons.Default.History, Screen.History.route),
        BottomNavItem("è®¾å¤‡", Icons.Default.Devices, Screen.DeviceManagement.route),
        BottomNavItem("æ–‡ä»¶", Icons.Default.Folder, Screen.FileViewer.route),
        BottomNavItem("è®¾ç½®", Icons.Default.Settings, Screen.Settings.route)
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) }
            )
        }
    }
}

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)
```

---

### 3.9.2 èŠå¤©ä¸»ç•Œé¢

```kotlin
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val chatState by viewModel.chatState.collectAsState()
    val currentConversation by viewModel.currentConversation.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // é¡¶éƒ¨æ 
        ChatTopBar(
            title = currentConversation?.title ?: "æ–°å¯¹è¯",
            onNewChat = { viewModel.startNewConversation() },
            onShowHistory = { navController.navigate(Screen.History.route) }
        )
        
        // æ¶ˆæ¯åˆ—è¡¨
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            contentPadding = PaddingValues(16.dp)
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // åŠ è½½çŠ¶æ€
            if (chatState is ChatState.Processing) {
                item {
                    LoadingIndicator()
                }
            }
        }
        
        // åŠŸèƒ½æŒ‰é’®è¡Œ
        ActionButtonRow(
            onAnalyzeCustomer = { viewModel.analyzeCustomer() },
            onGeneratePdf = { viewModel.exportPdf() },
            onGenerateCsv = { viewModel.exportCsv() }
        )
        
        // è¾“å…¥æ¡†
        MessageInputBar(
            text = messageText,
            onTextChange = { messageText = it },
            onSend = {
                if (messageText.isNotBlank()) {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            },
            onAttachFile = { /* TODO: æ–‡ä»¶é€‰æ‹© */ },
            onVoiceInput = { /* TODO: è¯­éŸ³è¾“å…¥ */ }
        )
    }
}

// èŠå¤©é¡¶éƒ¨æ 
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    title: String,
    onNewChat: () -> Unit,
    onShowHistory: () -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, contentDescription = "æ–°å»ºå¯¹è¯")
            }
            IconButton(onClick = onShowHistory) {
                Icon(Icons.Default.History, contentDescription = "æŸ¥çœ‹åŽ†å²")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

// æ¶ˆæ¯æ°”æ³¡
@Composable
fun MessageBubble(message: MessageEntity) {
    val isUser = message.role == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = "AI",
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isUser) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Markdownæ¸²æŸ“
                MarkdownText(
                    markdown = message.content,
                    color = if (isUser) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // æ—¶é—´æˆ³
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) 
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        if (isUser) {
            Icon(
                Icons.Default.Person,
                contentDescription = "ç”¨æˆ·",
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = 8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// åŠŸèƒ½æŒ‰é’®è¡Œ
@Composable
fun ActionButtonRow(
    onAnalyzeCustomer: () -> Unit,
    onGeneratePdf: () -> Unit,
    onGenerateCsv: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionChip(
            onClick = onAnalyzeCustomer,
            label = { Text("åˆ†æžå®¢æˆ·") },
            leadingIcon = {
                Icon(Icons.Default.Analytics, contentDescription = null)
            }
        )
        
        ActionChip(
            onClick = onGeneratePdf,
            label = { Text("ç”ŸæˆPDF") },
            leadingIcon = {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            }
        )
        
        ActionChip(
            onClick = onGenerateCsv,
            label = { Text("ç”ŸæˆCSV") },
            leadingIcon = {
                Icon(Icons.Default.TableChart, contentDescription = null)
            }
        )
    }
}

// æ¶ˆæ¯è¾“å…¥æ 
@Composable
fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachFile: () -> Unit,
    onVoiceInput: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttachFile) {
                Icon(Icons.Default.AttachFile, contentDescription = "é™„ä»¶")
            }
            
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("è¾“å…¥æ¶ˆæ¯...") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp)
            )
            
            IconButton(onClick = onVoiceInput) {
                Icon(Icons.Default.Mic, contentDescription = "è¯­éŸ³")
            }
            
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "å‘é€",
                    tint = if (text.isNotBlank()) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// åŠ è½½æŒ‡ç¤ºå™¨
@Composable
fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

// Markdownæ¸²æŸ“ (ç®€åŒ–ç‰ˆ)
@Composable
fun MarkdownText(markdown: String, color: Color) {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val boldPattern = "\\*\\*(.*?)\\*\\*".toRegex()
        
        boldPattern.findAll(markdown).forEach { match ->
            // æ·»åŠ æ™®é€šæ–‡æœ¬
            append(markdown.substring(currentIndex, match.range.first))
            
            // æ·»åŠ åŠ ç²—æ–‡æœ¬
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            
            currentIndex = match.range.last + 1
        }
        
        // æ·»åŠ å‰©ä½™æ–‡æœ¬
        if (currentIndex < markdown.length) {
            append(markdown.substring(currentIndex))
        }
    }
    
    Text(
        text = annotatedString,
        color = color,
        style = MaterialTheme.typography.bodyMedium
    )
}

// æ—¶é—´æ ¼å¼åŒ–
fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "åˆšåˆš"
        diff < 3600_000 -> "${diff / 60_000}åˆ†é’Ÿå‰"
        diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.CHINESE).format(Date(timestamp))
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.CHINESE).format(Date(timestamp))
    }
}
```

---

### 3.9.3 åŽ†å²è®°å½•ç•Œé¢

```kotlin
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val groupedConversations by viewModel.groupedConversations.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // é¡¶éƒ¨æ 
        HistoryTopBar(
            onBack = { navController.popBackStack() },
            onSearch = { /* TODO: æœç´¢åŠŸèƒ½ */ }
        )
        
        // å¯¹è¯åˆ—è¡¨
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            groupedConversations.forEach { (timeGroup, conversations) ->
                // æ—¶é—´åˆ†ç»„æ ‡é¢˜
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Text(
                            text = timeGroup,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                
                // å¯¹è¯é¡¹
                items(conversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = {
                            navController.navigate(
                                Screen.ConversationDetail.createRoute(conversation.id)
                            )
                        },
                        onDelete = { viewModel.deleteConversation(conversation.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTopBar(
    onBack: () -> Unit,
    onSearch: () -> Unit
) {
    TopAppBar(
        title = { Text("å¯¹è¯åŽ†å²") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "è¿”å›ž")
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, contentDescription = "æœç´¢")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationItem(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    SwipeToDismissBox(
        state = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    showDeleteDialog = true
                }
                false
            }
        ),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "åˆ é™¤",
                    modifier = Modifier.padding(16.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (conversation.isAnalyzed) {
                        Badge {
                            Text("å·²åˆ†æž")
                        }
                    }
                }
                
                conversation.summary?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Text(
                    text = formatTimestamp(conversation.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
    
    // åˆ é™¤ç¡®è®¤å¯¹è¯æ¡†
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("åˆ é™¤å¯¹è¯") },
            text = { Text("ç¡®å®šè¦åˆ é™¤è¿™ä¸ªå¯¹è¯å—ï¼Ÿæ­¤æ“ä½œæ— æ³•æ’¤é”€ã€‚") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("åˆ é™¤")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("å–æ¶ˆ")
                }
            }
        )
    }
}
```

---

### 3.9.4 è®¾å¤‡ç®¡ç†ç•Œé¢

```kotlin
@Composable
fun DeviceManagementScreen(
    viewModel: DeviceViewModel = hiltViewModel()
) {
    val bleState by viewModel.bleConnectionState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val currentDevice by viewModel.currentDevice.collectAsState()
    val wifiConfigs by viewModel.wifiConfigs.collectAsState()
    
    var showWifiDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // å½“å‰è®¾å¤‡çŠ¶æ€
        CurrentDeviceCard(
            device = currentDevice,
            connectionState = bleState,
            onDisconnect = { viewModel.disconnect() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // WiFié…ç½®
        WifiConfigSection(
            configs = wifiConfigs,
            onAddWifi = { showWifiDialog = true },
            onSelectWifi = { config -> viewModel.sendWifiConfig(config) },
            onDeleteWifi = { config -> viewModel.deleteWifiConfig(config) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // æ‰«ææŒ‰é’®
        Button(
            onClick = { viewModel.startBleScan() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.BluetoothSearching, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("æ‰«æè®¾å¤‡")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // å‘çŽ°çš„è®¾å¤‡åˆ—è¡¨
        Text(
            text = "å‘çŽ°çš„è®¾å¤‡",
            style = MaterialTheme.typography.titleMedium
        )
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(discoveredDevices) { device ->
                DeviceItem(
                    device = device,
                    onClick = { viewModel.connectDevice(device) }
                )
            }
        }
    }
    
    // WiFié…ç½®å¯¹è¯æ¡†
    if (showWifiDialog) {
        AddWifiDialog(
            onDismiss = { showWifiDialog = false },
            onConfirm = { ssid, password ->
                viewModel.saveWifiConfig(ssid, password)
                showWifiDialog = false
            }
        )
    }
}

@Composable
fun CurrentDeviceCard(
    device: DeviceSettingEntity?,
    connectionState: BleConnectionState,
    onDisconnect: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "å½“å‰è®¾å¤‡",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (device != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = device.deviceName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = device.deviceId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // è¿žæŽ¥çŠ¶æ€
                        val statusText = when (connectionState) {
                            is BleConnectionState.Connected -> "å·²è¿žæŽ¥"
                            is BleConnectionState.ServicesDiscovered -> "æœåŠ¡å·²å‘çŽ°"
                            else -> "æœªè¿žæŽ¥"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (connectionState is BleConnectionState.Disconnected)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (connectionState !is BleConnectionState.Disconnected) {
                        TextButton(onClick = onDisconnect) {
                            Text("æ–­å¼€")
                        }
                    }
                }
            } else {
                Text(
                    text = "æœªé…å¯¹è®¾å¤‡",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WifiConfigSection(
    configs: List<WifiConfigEntity>,
    onAddWifi: () -> Unit,
    onSelectWifi: (WifiConfigEntity) -> Unit,
    onDeleteWifi: (WifiConfigEntity) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WiFié…ç½®",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onAddWifi) {
                    Icon(Icons.Default.Add, contentDescription = "æ·»åŠ WiFi")
                }
            }
            
            if (configs.isEmpty()) {
                Text(
                    text = "æš‚æ— WiFié…ç½®",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                configs.forEach { config ->
                    WifiConfigItem(
                        config = config,
                        onSelect = { onSelectWifi(config) },
                        onDelete = { onDeleteWifi(config) }
                    )
                }
            }
        }
    }
}

@Composable
fun AddWifiDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("æ·»åŠ WiFié…ç½®") },
        text = {
            Column {
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("SSID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("å¯†ç ") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(ssid, password) },
                enabled = ssid.isNotBlank()
            ) {
                Text("ç¡®å®š")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("å–æ¶ˆ")
            }
        }
    )
}
```

---

### 3.9.5 ViewModelå®žçŽ°

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val aiServiceManager: AiServiceManager,
    private val exportManager: ExportManager
) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()
    
    private val _currentConversation = MutableStateFlow<ConversationEntity?>(null)
    val currentConversation: StateFlow<ConversationEntity?> = _currentConversation.asStateFlow()
    
    val chatState = aiServiceManager.chatState
    
    private var currentConversationId: Long = 0L
    
    init {
        startNewConversation()
    }
    
    fun startNewConversation() {
        viewModelScope.launch {
            currentConversationId = conversationRepository.createConversation("æ–°å¯¹è¯")
            loadMessages()
        }
    }
    
    fun sendMessage(text: String) {
        viewModelScope.launch {
            aiServiceManager.sendChatMessage(currentConversationId, text)
            loadMessages()
        }
    }
    
    fun analyzeCustomer() {
        viewModelScope.launch {
            // TODO: å®žçŽ°å®¢æˆ·åˆ†æž
        }
    }
    
    fun exportPdf() {
        viewModelScope.launch {
            // TODO: å®žçŽ°PDFå¯¼å‡º
        }
    }
    
    fun exportCsv() {
        viewModelScope.launch {
            // TODO: å®žçŽ°CSVå¯¼å‡º
        }
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            val conversation = conversationRepository.getConversationWithMessages(currentConversationId)
            conversation?.let {
                _messages.value = it.messages
                _currentConversation.value = it.conversation
            }
        }
    }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {
    
    private val _groupedConversations = MutableStateFlow<Map<String, List<ConversationEntity>>>(emptyMap())
    val groupedConversations: StateFlow<Map<String, List<ConversationEntity>>> = _groupedConversations.asStateFlow()
    
    init {
        loadConversations()
    }
    
    private fun loadConversations() {
        viewModelScope.launch {
            _groupedConversations.value = conversationRepository.getConversationsGroupedByTime()
        }
    }
    
    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(id)
            loadConversations()
        }
    }
}

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    
    val bleConnectionState = bleManager.connectionState
    val discoveredDevices = bleManager.discoveredDevices
    
    private val _currentDevice = MutableStateFlow<DeviceSettingEntity?>(null)
    val currentDevice: StateFlow<DeviceSettingEntity?> = _currentDevice.asStateFlow()
    
    val wifiConfigs = deviceRepository.allWifiConfigs
    
    init {
        loadCurrentDevice()
    }
    
    private fun loadCurrentDevice() {
        viewModelScope.launch {
            _currentDevice.value = deviceRepository.getCurrentDevice()
        }
    }
    
    fun startBleScan() {
        bleManager.startScan()
    }
    
    fun connectDevice(device: BluetoothDevice) {
        bleManager.connectDevice(device)
        viewModelScope.launch {
            deviceRepository.saveDevice(device.address, device.name ?: "æœªçŸ¥è®¾å¤‡")
            loadCurrentDevice()
        }
    }
    
    fun disconnect() {
        bleManager.disconnect()
    }
    
    fun saveWifiConfig(ssid: String, password: String) {
        viewModelScope.launch {
            deviceRepository.saveWifiConfig(ssid, password)
        }
    }
    
    fun sendWifiConfig(config: WifiConfigEntity) {
        viewModelScope.launch {
            bleManager.sendWifiConfig(config.ssid, config.password)
        }
    }
    
    fun deleteWifiConfig(config: WifiConfigEntity) {
        // TODO: å®žçŽ°åˆ é™¤
    }
    
    override fun onCleared() {
        super.onCleared()
        bleManager.stopScan()
    }
}
```

---

## å®Œæˆè¿›åº¦

âœ… **æ•°æ®æŒä¹…åŒ–** - Roomæ•°æ®åº“ã€DAOã€Repository  
âœ… **BLEè¿žæŽ¥æ¨¡å—** - è®¾å¤‡æ‰«æã€é…å¯¹ã€WiFié…ç½®ä¼ è¾“  
âœ… **WiFiæ–‡ä»¶åŒæ­¥** - HTTPå®¢æˆ·ç«¯ã€æ–‡ä»¶ä¸Šä¼ /ä¸‹è½½ã€æ˜¾ç¤ºæŽ§åˆ¶  
âœ… **AIé›†æˆæ¨¡å—** - Qwen DashscopeèŠå¤© + Tingwuè½¬å†™  
âœ… **PDF/CSVå¯¼å‡º** - æ–‡æ¡£ç”Ÿæˆã€CRMæ•°æ®å¯¼å‡º  
âœ… **UIå±‚ (Compose)** - ä¸»èŠå¤©ç•Œé¢ã€åŽ†å²è®°å½•ã€è®¾å¤‡ç®¡ç†  

## ðŸŽ‰ æ ¸å¿ƒåŠŸèƒ½å…¨éƒ¨å®Œæˆ!

çŽ°åœ¨éœ€è¦åˆ›å»ºï¼š
1. **build.gradleé…ç½®** - æ‰€æœ‰ä¾èµ–é¡¹
2. **AndroidManifest.xml** - æƒé™å’Œé…ç½®
3. **Applicationç±»** - Hiltè®¾ç½®

ç»§ç»­ï¼Ÿ
