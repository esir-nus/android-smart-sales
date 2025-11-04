# Smart Sales Assistant - Testing Strategy

## ðŸ§ª Testing Overview

This document provides a comprehensive testing strategy for the Smart Sales Assistant application, covering unit tests, integration tests, UI tests, and best practices.

---

## ðŸ“Š Testing Pyramid

```
           /\
          /  \
         / UI \          10% - E2E/UI Tests (Slow, Brittle)
        /______\
       /        \
      /  Integ.  \      20% - Integration Tests (Medium)
     /____________\
    /              \
   /  Unit Tests    \   70% - Unit Tests (Fast, Reliable)
  /__________________\
```

**Target Coverage:**
- Unit Tests: 70%+ code coverage
- Integration Tests: Critical flows
- UI Tests: Key user journeys

---

## ðŸŽ¯ Test Dependencies

```kotlin
// build.gradle.kts (app module)
dependencies {
    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("com.google.truth:truth:1.1.5")
    
    // Integration Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    
    // UI Testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.7")
    
    // Mock Web Server
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    
    // Hilt Testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.48")
}
```

---

## ðŸ”¬ Unit Tests

### 1. Repository Tests

```kotlin
// ConversationRepositoryTest.kt
@RunWith(MockitoJUnitRunner::class)
class ConversationRepositoryTest {
    
    @Mock
    private lateinit var conversationDao: ConversationDao
    
    @Mock
    private lateinit var messageDao: MessageDao
    
    @Mock
    private lateinit var attachmentDao: AttachmentDao
    
    private lateinit var repository: ConversationRepository
    
    @Before
    fun setup() {
        repository = ConversationRepository(
            conversationDao,
            messageDao,
            attachmentDao
        )
    }
    
    @Test
    fun `createConversation inserts with current timestamp`() = runTest {
        // Given
        val title = "Test Conversation"
        val expectedId = 1L
        whenever(conversationDao.insertConversation(any())).thenReturn(expectedId)
        
        // When
        val result = repository.createConversation(title)
        
        // Then
        assertThat(result).isEqualTo(expectedId)
        verify(conversationDao).insertConversation(argThat { conversation ->
            conversation.title == title &&
            conversation.createdAt > 0 &&
            conversation.updatedAt > 0
        })
    }
    
    @Test
    fun `addMessage saves to database and updates conversation`() = runTest {
        // Given
        val conversationId = 1L
        val role = "user"
        val content = "Hello"
        val conversation = ConversationEntity(
            id = conversationId,
            title = "Test",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        whenever(conversationDao.getConversationById(conversationId))
            .thenReturn(conversation)
        whenever(messageDao.insertMessage(any())).thenReturn(1L)
        
        // When
        repository.addMessage(conversationId, role, content)
        
        // Then
        verify(messageDao).insertMessage(argThat { message ->
            message.conversationId == conversationId &&
            message.role == role &&
            message.content == content
        })
        verify(conversationDao).updateConversation(any())
    }
    
    @Test
    fun `getConversationsGroupedByTime groups correctly`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val conversations = listOf(
            ConversationEntity(1, "Recent", null, now - 3600000, now),
            ConversationEntity(2, "Week old", null, now - 7 * 86400000, now),
            ConversationEntity(3, "Old", null, now - 30 * 86400000, now)
        )
        
        whenever(conversationDao.getAllConversations())
            .thenReturn(flowOf(conversations))
        
        // When
        val result = repository.getConversationsGroupedByTime()
        
        // Then
        assertThat(result).containsKey("7å¤©å†…")
        assertThat(result).containsKey("30å¤©å†…")
        assertThat(result["7å¤©å†…"]).hasSize(1)
    }
}
```

### 2. ViewModel Tests

```kotlin
// ChatViewModelTest.kt
@RunWith(MockitoJUnitRunner::class)
class ChatViewModelTest {
    
    @Mock
    private lateinit var aiRepository: AiRepository
    
    @Mock
    private lateinit var conversationRepository: ConversationRepository
    
    private lateinit var viewModel: ChatViewModel
    
    @Before
    fun setup() {
        viewModel = ChatViewModel(aiRepository, conversationRepository)
    }
    
    @Test
    fun `sendMessage updates state to Loading then Success`() = runTest {
        // Given
        val conversationId = 1L
        val message = "Hello"
        val response = "Hi there!"
        
        whenever(aiRepository.chatManager.chat(conversationId, message, true))
            .thenReturn(Result.success(response))
        
        // When
        viewModel.sendMessage(conversationId, message)
        
        // Then
        viewModel.chatState.test {
            assertThat(awaitItem()).isInstanceOf(ChatUiState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(ChatUiState.Success::class.java)
        }
    }
    
    @Test
    fun `sendMessage handles error correctly`() = runTest {
        // Given
        val conversationId = 1L
        val message = "Hello"
        val error = Exception("Network error")
        
        whenever(aiRepository.chatManager.chat(conversationId, message, true))
            .thenReturn(Result.failure(error))
        
        // When
        viewModel.sendMessage(conversationId, message)
        
        // Then
        viewModel.chatState.test {
            assertThat(awaitItem()).isInstanceOf(ChatUiState.Loading::class.java)
            val errorState = awaitItem() as ChatUiState.Error
            assertThat(errorState.message).contains("Network error")
        }
    }
    
    @Test
    fun `loadConversation fetches messages from repository`() = runTest {
        // Given
        val conversationId = 1L
        val messages = listOf(
            MessageEntity(1, conversationId, "user", "Hello", System.currentTimeMillis(), null, null),
            MessageEntity(2, conversationId, "assistant", "Hi!", System.currentTimeMillis(), null, null)
        )
        val conversation = ConversationWithMessages(
            ConversationEntity(conversationId, "Test", null, 0, 0),
            messages
        )
        
        whenever(conversationRepository.getConversationWithMessages(conversationId))
            .thenReturn(conversation)
        
        // When
        viewModel.loadConversation(conversationId)
        
        // Then
        viewModel.messages.test {
            val messageList = awaitItem()
            assertThat(messageList).hasSize(2)
            assertThat(messageList[0].content).isEqualTo("Hello")
        }
    }
}
```

### 3. AI Manager Tests

```kotlin
// AiChatManagerTest.kt
@RunWith(MockitoJUnitRunner::class)
class AiChatManagerTest {
    
    @Mock
    private lateinit var dashscopeApi: DashscopeApi
    
    @Mock
    private lateinit var conversationRepository: ConversationRepository
    
    private lateinit var manager: AiChatManager
    
    @Before
    fun setup() {
        manager = AiChatManager(dashscopeApi, conversationRepository)
    }
    
    @Test
    fun `chat with context includes conversation history`() = runTest {
        // Given
        val conversationId = 1L
        val userMessage = "What did we discuss?"
        val previousMessages = listOf(
            MessageEntity(1, conversationId, "user", "Hello", 0, null, null),
            MessageEntity(2, conversationId, "assistant", "Hi!", 0, null, null)
        )
        val conversation = ConversationWithMessages(
            ConversationEntity(conversationId, "Test", null, 0, 0),
            previousMessages
        )
        
        val mockResponse = ChatResponse(
            output = ChatOutput(
                text = null,
                finish_reason = "stop",
                choices = listOf(
                    ChatChoice(
                        finish_reason = "stop",
                        message = ChatMessage("assistant", "We discussed greetings.")
                    )
                )
            ),
            usage = Usage(10, 5, 15),
            request_id = "test"
        )
        
        whenever(conversationRepository.getConversationWithMessages(conversationId))
            .thenReturn(conversation)
        whenever(dashscopeApi.chat(any())).thenReturn(Response.success(mockResponse))
        
        // When
        val result = manager.chat(conversationId, userMessage, useContext = true)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        verify(dashscopeApi).chat(argThat { request ->
            request.input.messages.size >= 4 // system + 2 previous + new message
        })
    }
    
    @Test
    fun `extractCrmData returns structured data`() = runTest {
        // Given
        val transcript = "å®¢æˆ·å¼ ä¸‰ï¼Œæ¥è‡ªABCå…¬å¸ï¼Œç”µè¯13800138000"
        val jsonResponse = """
            {
                "customer_name": "å¼ ä¸‰",
                "company": "ABCå…¬å¸",
                "phone": "13800138000"
            }
        """.trimIndent()
        
        val mockResponse = ChatResponse(
            output = ChatOutput(
                text = null,
                finish_reason = "stop",
                choices = listOf(
                    ChatChoice(
                        finish_reason = "stop",
                        message = ChatMessage("assistant", jsonResponse)
                    )
                )
            ),
            usage = Usage(50, 20, 70),
            request_id = "test"
        )
        
        whenever(dashscopeApi.chat(any())).thenReturn(Response.success(mockResponse))
        
        // When
        val result = manager.extractCrmData(transcript)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        val crmData = result.getOrThrow()
        assertThat(crmData.customer_name).isEqualTo("å¼ ä¸‰")
        assertThat(crmData.company).isEqualTo("ABCå…¬å¸")
        assertThat(crmData.phone).isEqualTo("13800138000")
    }
}
```

### 4. File Sync Tests

```kotlin
// FileSyncManagerTest.kt
@RunWith(MockitoJUnitRunner::class)
class FileSyncManagerTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var api: GadgetApi
    
    @Mock
    private lateinit var attachmentDao: AttachmentDao
    
    private lateinit var manager: FileSyncManager
    
    @Before
    fun setup() {
        val audioDir = File("/tmp/audio")
        val imageDir = File("/tmp/images")
        audioDir.mkdirs()
        imageDir.mkdirs()
        
        whenever(context.filesDir).thenReturn(File("/tmp"))
        
        manager = FileSyncManager(context, api, attachmentDao)
    }
    
    @Test
    fun `syncFileList returns files on success`() = runTest {
        // Given
        val mockFiles = listOf(
            GadgetFile("recording1.mp3", "audio", 1024, 0, "/path"),
            GadgetFile("image1.gif", "image", 512, 0, "/path")
        )
        val response = FileListResponse(success = true, files = mockFiles)
        
        whenever(api.getFileList()).thenReturn(Response.success(response))
        
        // When
        val result = manager.syncFileList()
        
        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).hasSize(2)
    }
    
    @Test
    fun `downloadFile saves to correct directory`() = runTest {
        // Given
        val gadgetFile = GadgetFile("test.mp3", "audio", 1024, 0, "/path")
        val mockBody = "test content".toByteArray()
        val responseBody = mockBody.toResponseBody("application/octet-stream".toMediaType())
        
        whenever(api.downloadFile("test.mp3"))
            .thenReturn(Response.success(responseBody))
        
        // When
        val result = manager.downloadFile(gadgetFile) {}
        
        // Then
        assertThat(result.isSuccess).isTrue()
        val file = result.getOrThrow()
        assertThat(file.name).isEqualTo("test.mp3")
        assertThat(file.parentFile?.name).isEqualTo("audio")
    }
    
    @Test
    fun `smartSync only downloads new files`() = runTest {
        // Given
        val remoteFiles = listOf(
            GadgetFile("new.mp3", "audio", 1024, 0, "/path"),
            GadgetFile("existing.mp3", "audio", 1024, 0, "/path")
        )
        
        // Create existing file
        File(context.filesDir, "audio/existing.mp3").apply {
            parentFile?.mkdirs()
            writeText("existing")
        }
        
        val response = FileListResponse(success = true, files = remoteFiles)
        whenever(api.getFileList()).thenReturn(Response.success(response))
        
        // When
        val result = manager.smartSync()
        
        // Then
        verify(api, times(1)).downloadFile("new.mp3")
        verify(api, never()).downloadFile("existing.mp3")
    }
}
```

### 5. Export Tests

```kotlin
// PdfGeneratorTest.kt
@RunWith(RobolectricTestRunner::class)
class PdfGeneratorTest {
    
    private lateinit var context: Context
    private lateinit var generator: PdfGenerator
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        generator = PdfGenerator(context)
    }
    
    @Test
    fun `generateFromMarkdown creates valid PDF`() = runTest {
        // Given
        val markdown = """
            # Test Report
            
            ## Section 1
            This is a test paragraph.
            
            - Item 1
            - Item 2
        """.trimIndent()
        
        // When
        val result = generator.generateFromMarkdown(
            markdownContent = markdown,
            title = "Test Report"
        )
        
        // Then
        assertThat(result.isSuccess).isTrue()
        val file = result.getOrThrow()
        assertThat(file.exists()).isTrue()
        assertThat(file.extension).isEqualTo("pdf")
        assertThat(file.length()).isGreaterThan(0)
    }
    
    @Test
    fun `generateCustomerAnalysisReport includes all sections`() = runTest {
        // Given
        val analysis = "# Customer Analysis\n\nTest content"
        val transcription = FormattedTranscription(
            fullText = "Test transcript",
            speakers = listOf(SpeakerInfo("1", "Speaker 1", 5000, 10)),
            summary = "Summary",
            totalDuration = 5000
        )
        val crmData = CrmData(
            customer_name = "å¼ ä¸‰",
            company = "ABCå…¬å¸",
            phone = "13800138000",
            email = null, wechat = null, position = null,
            industry = null, company_size = null, location = null,
            product_interest = null, budget_range = null,
            decision_timeline = null, pain_points = null,
            competitors = null, decision_makers = null,
            current_stage = null, next_follow_up = null, notes = null
        )
        
        // When
        val result = generator.generateCustomerAnalysisReport(
            analysis = analysis,
            transcription = transcription,
            crmData = crmData,
            customerName = "å¼ ä¸‰"
        )
        
        // Then
        assertThat(result.isSuccess).isTrue()
        val file = result.getOrThrow()
        assertThat(file.name).contains("å¼ ä¸‰")
        assertThat(file.length()).isGreaterThan(0)
    }
}

// CsvGeneratorTest.kt
@RunWith(RobolectricTestRunner::class)
class CsvGeneratorTest {
    
    private lateinit var context: Context
    private lateinit var generator: CsvGenerator
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        generator = CsvGenerator(context)
    }
    
    @Test
    fun `generateGenericCsv creates valid CSV`() = runTest {
        // Given
        val crmData = listOf(
            CrmData(
                customer_name = "å¼ ä¸‰",
                company = "ABCå…¬å¸",
                phone = "13800138000",
                email = "test@example.com",
                wechat = null, position = null, industry = null,
                company_size = null, location = null,
                product_interest = null, budget_range = null,
                decision_timeline = null, pain_points = listOf("æˆæœ¬é«˜", "æ•ˆçŽ‡ä½Ž"),
                competitors = null, decision_makers = null,
                current_stage = null, next_follow_up = null, notes = null
            )
        )
        
        // When
        val result = generator.generateGenericCsv(crmData)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        val file = result.getOrThrow()
        assertThat(file.exists()).isTrue()
        
        val content = file.readText()
        assertThat(content).contains("å¼ ä¸‰")
        assertThat(content).contains("ABCå…¬å¸")
        assertThat(content).contains("æˆæœ¬é«˜; æ•ˆçŽ‡ä½Ž")
    }
    
    @Test
    fun `generateSalesforceCsv maps stage correctly`() = runTest {
        // Given
        val crmData = listOf(
            CrmData(
                customer_name = "å¼ ä¸‰",
                company = "ABCå…¬å¸",
                phone = null, email = null, wechat = null,
                position = null, industry = null, company_size = null,
                location = null, product_interest = null,
                budget_range = null, decision_timeline = null,
                pain_points = null, competitors = null,
                decision_makers = null,
                current_stage = "éœ€æ±‚ç¡®è®¤",
                next_follow_up = null, notes = null
            )
        )
        
        // When
        val result = generator.generateSalesforceCsv(crmData)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        val file = result.getOrThrow()
        val content = file.readText()
        assertThat(content).contains("Qualification") // Mapped stage
    }
}
```

---

## ðŸ”— Integration Tests

### 1. Database Tests

```kotlin
// ConversationDaoTest.kt
@RunWith(AndroidJUnit4::class)
class ConversationDaoTest {
    
    private lateinit var database: SmartSalesDatabase
    private lateinit var conversationDao: ConversationDao
    private lateinit var messageDao: MessageDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            SmartSalesDatabase::class.java
        ).allowMainThreadQueries().build()
        
        conversationDao = database.conversationDao()
        messageDao = database.messageDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveConversation() = runTest {
        // Given
        val conversation = ConversationEntity(
            id = 0,
            title = "Test Conversation",
            summary = "Test summary",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // When
        val id = conversationDao.insertConversation(conversation)
        val retrieved = conversationDao.getConversationById(id)
        
        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.title).isEqualTo("Test Conversation")
    }
    
    @Test
    fun deleteConversationCascadesMessages() = runTest {
        // Given
        val conversationId = conversationDao.insertConversation(
            ConversationEntity(0, "Test", null, 0, 0)
        )
        
        messageDao.insertMessage(
            MessageEntity(0, conversationId, "user", "Hello", 0, null, null)
        )
        
        // When
        conversationDao.deleteConversationById(conversationId)
        
        // Then
        val messages = messageDao.getMessagesByConversation(conversationId).first()
        assertThat(messages).isEmpty()
    }
    
    @Test
    fun getConversationsByTimeRangeFiltersCorrectly() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val oldTime = now - 10 * 86400000 // 10 days ago
        
        conversationDao.insertConversation(
            ConversationEntity(0, "Recent", null, now, now)
        )
        conversationDao.insertConversation(
            ConversationEntity(0, "Old", null, oldTime, oldTime)
        )
        
        // When
        val startTime = now - 7 * 86400000 // 7 days ago
        val result = conversationDao.getConversationsByTimeRange(startTime, now + 1000)
            .first()
        
        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Recent")
    }
}
```

### 2. API Integration Tests

```kotlin
// QwenApiIntegrationTest.kt
@RunWith(AndroidJUnit4::class)
class QwenApiIntegrationTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: DashscopeApi
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(DashscopeApi::class.java)
    }
    
    @After
    fun teardown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun chatRequest_returnsExpectedResponse() = runTest {
        // Given
        val mockResponse = """
            {
                "output": {
                    "text": null,
                    "finish_reason": "stop",
                    "choices": [{
                        "finish_reason": "stop",
                        "message": {
                            "role": "assistant",
                            "content": "Hello! How can I help?"
                        }
                    }]
                },
                "usage": {
                    "input_tokens": 10,
                    "output_tokens": 5,
                    "total_tokens": 15
                },
                "request_id": "test123"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
        )
        
        val request = ChatRequest(
            model = "qwen-max",
            input = ChatInput(
                messages = listOf(
                    ChatMessage("user", "Hello")
                )
            )
        )
        
        // When
        val response = api.chat(request)
        
        // Then
        assertThat(response.isSuccessful).isTrue()
        val body = response.body()!!
        assertThat(body.output.choices?.first()?.message?.content)
            .isEqualTo("Hello! How can I help?")
        assertThat(body.usage.total_tokens).isEqualTo(15)
    }
    
    @Test
    fun chatRequest_handlesError() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Unauthorized"}""")
        )
        
        val request = ChatRequest(
            model = "qwen-max",
            input = ChatInput(messages = listOf(ChatMessage("user", "Hello")))
        )
        
        // When
        val response = api.chat(request)
        
        // Then
        assertThat(response.isSuccessful).isFalse()
        assertThat(response.code()).isEqualTo(401)
    }
}
```

---

## ðŸŽ¨ UI Tests (Compose)

### 1. Chat Screen Tests

```kotlin
// ChatScreenTest.kt
@RunWith(AndroidJUnit4::class)
class ChatScreenTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun chatScreen_displaysMessages() {
        // Given
        val messages = listOf(
            MessageUi(1, "user", "Hello", System.currentTimeMillis()),
            MessageUi(2, "assistant", "Hi there!", System.currentTimeMillis())
        )
        
        // When
        composeTestRule.setContent {
            SmartSalesTheme {
                ChatScreen(
                    conversationId = 1L,
                    onBackClick = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Hello").assertExists()
        composeTestRule.onNodeWithText("Hi there!").assertExists()
    }
    
    @Test
    fun chatScreen_sendButtonEnabled_whenTextNotEmpty() {
        // When
        composeTestRule.setContent {
            SmartSalesTheme {
                var text by remember { mutableStateOf("") }
                ChatInputBar(
                    text = text,
                    onTextChange = { text = it },
                    onSendClick = {},
                    onAttachClick = {},
                    onVoiceClick = {}
                )
            }
        }
        
        // Type text
        composeTestRule.onNodeWithText("è¾“å…¥æ¶ˆæ¯...").performTextInput("Test message")
        
        // Then
        composeTestRule.onNodeWithContentDescription("å‘é€").assertExists()
    }
    
    @Test
    fun chatScreen_showsEmptyState_whenNoMessages() {
        // When
        composeTestRule.setContent {
            SmartSalesTheme {
                ChatScreen(
                    conversationId = 1L,
                    onBackClick = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("å¼€å§‹å¯¹è¯").assertExists()
    }
}
```

### 2. Conversation List Tests

```kotlin
// ConversationListScreenTest.kt
@RunWith(AndroidJUnit4::class)
class ConversationListScreenTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun conversationList_displaysConversations() {
        // Given
        val conversations = mapOf(
            "7å¤©å†…" to listOf(
                ConversationUi(1, "å®¢æˆ·Aåˆ†æž", "å¼ ä¸‰", System.currentTimeMillis(), true)
            ),
            "30å¤©å†…" to listOf(
                ConversationUi(2, "ä¼šè®®è®°å½•", null, System.currentTimeMillis() - 10 * 86400000, false)
            )
        )
        
        // When
        composeTestRule.setContent {
            SmartSalesTheme {
                // Set up with test data
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("7å¤©å†…").assertExists()
        composeTestRule.onNodeWithText("å®¢æˆ·Aåˆ†æž").assertExists()
        composeTestRule.onNodeWithText("ä¼šè®®è®°å½•").assertExists()
    }
    
    @Test
    fun conversationList_fabClick_navigatesToNewChat() {
        // Given
        var navigatedToNewChat = false
        
        composeTestRule.setContent {
            SmartSalesTheme {
                ConversationListScreen(
                    onConversationClick = {},
                    onNewConversation = { navigatedToNewChat = true },
                    onDeviceClick = {},
                    onFileSyncClick = {},
                    onSettingsClick = {}
                )
            }
        }
        
        // When
        composeTestRule.onNodeWithContentDescription("æ–°å¯¹è¯").performClick()
        
        // Then
        assertThat(navigatedToNewChat).isTrue()
    }
}
```

### 3. Device Pairing Tests

```kotlin
// DevicePairingScreenTest.kt
@RunWith(AndroidJUnit4::class)
class DevicePairingScreenTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun devicePairing_startsScanning_whenButtonClicked() {
        // Given
        var scanStarted = false
        
        composeTestRule.setContent {
            SmartSalesTheme {
                Button(onClick = { scanStarted = true }) {
                    Text("å¼€å§‹æ‰«æ")
                }
            }
        }
        
        // When
        composeTestRule.onNodeWithText("å¼€å§‹æ‰«æ").performClick()
        
        // Then
        assertThat(scanStarted).isTrue()
    }
    
    @Test
    fun wifiConfigDialog_displaysCorrectly() {
        // Given
        val device = BluetoothDeviceUi("Test Device", "00:11:22:33:44:55")
        
        composeTestRule.setContent {
            SmartSalesTheme {
                WifiConfigDialog(
                    device = device,
                    onDismiss = {},
                    onConfirm = { _, _ -> }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("é…ç½®WiFi").assertExists()
        composeTestRule.onNodeWithText("WiFiåç§° (SSID)").assertExists()
        composeTestRule.onNodeWithText("WiFiå¯†ç ").assertExists()
    }
}
```

---

## ðŸš€ CI/CD Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/android.yml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Run unit tests
      run: ./gradlew testDebugUnitTest
    
    - name: Run lint
      run: ./gradlew lintDebug
    
    - name: Generate test report
      run: ./gradlew jacocoTestReport
    
    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: test-results
        path: app/build/reports/tests/
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        files: app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
    
  instrumentation-test:
    runs-on: macos-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run instrumented tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 29
        script: ./gradlew connectedAndroidTest
```

---

## ðŸ“ˆ Code Coverage Configuration

```kotlin
// build.gradle.kts (app module)
plugins {
    id("jacoco")
}

android {
    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*\$Lambda$*.*",
        "**/*\$inlined$*.*"
    )
    
    val debugTree = fileTree("${project.buildDir}/intermediates/javac/debug") {
        exclude(fileFilter)
    }
    
    val mainSrc = "${project.projectDir}/src/main/java"
    
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.buildDir) {
        include("jacoco/testDebugUnitTest.exec")
    })
}
```

---

## ðŸŽ¯ Testing Best Practices

### 1. Test Naming Convention

```kotlin
// Pattern: methodName_stateUnderTest_expectedBehavior

@Test
fun sendMessage_withValidInput_updatesStateToSuccess()

@Test
fun downloadFile_whenNetworkError_returnsFailure()

@Test
fun generatePdf_withEmptyContent_throwsException()
```

### 2. AAA Pattern (Arrange-Act-Assert)

```kotlin
@Test
fun example() {
    // Arrange - Set up test data
    val input = "test"
    val expected = "TEST"
    
    // Act - Execute the code under test
    val result = input.uppercase()
    
    // Assert - Verify the result
    assertThat(result).isEqualTo(expected)
}
```

### 3. Test Doubles

```kotlin
// Use appropriate test doubles
@Mock // For dependencies you don't control
private lateinit var externalApi: ExternalApi

@Spy // For partial mocking
private lateinit var repository: Repository

@Captor // For capturing arguments
private lateinit var captor: ArgumentCaptor<Data>

// Fake for complex behavior
class FakeDatabase : Database {
    private val data = mutableListOf<Entity>()
    override fun insert(entity: Entity) = data.add(entity)
    override fun getAll() = data.toList()
}
```

### 4. Coroutine Testing

```kotlin
@Test
fun testCoroutine() = runTest {
    // Use runTest for coroutine tests
    val result = suspendingFunction()
    assertThat(result).isNotNull()
}

@Test
fun testFlow() = runTest {
    // Use turbine for Flow testing
    repository.dataFlow.test {
        assertThat(awaitItem()).isEqualTo(expected)
        cancelAndIgnoreRemainingEvents()
    }
}
```

### 5. Test Data Builders

```kotlin
// Use builders for complex test data
object TestDataFactory {
    fun createConversation(
        id: Long = 1L,
        title: String = "Test",
        createdAt: Long = System.currentTimeMillis()
    ) = ConversationEntity(id, title, null, createdAt, createdAt)
    
    fun createMessage(
        id: Long = 1L,
        conversationId: Long = 1L,
        role: String = "user",
        content: String = "Test message"
    ) = MessageEntity(id, conversationId, role, content, 0, null, null)
}
```

---

## ðŸ“Š Test Reports

### Generate Test Reports

```bash
# Unit tests with report
./gradlew testDebugUnitTest --info

# Coverage report
./gradlew jacocoTestReport

# Instrumentation tests
./gradlew connectedAndroidTest

# View reports
open app/build/reports/tests/testDebugUnitTest/index.html
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

---

## âœ… Testing Checklist

### Before Commit
- [ ] All unit tests pass
- [ ] No failing integration tests
- [ ] Code coverage > 70%
- [ ] No lint warnings
- [ ] UI tests for new features

### Before Release
- [ ] Full test suite passes
- [ ] Instrumentation tests on real devices
- [ ] Performance tests
- [ ] Security tests
- [ ] Manual QA completed

---

**Last Updated:** November 2025  
**Version:** 1.0.0
