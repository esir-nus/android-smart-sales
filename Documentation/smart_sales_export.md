# Smart Sales Assistant - PDF/CSV Export Module

## 3.8 PDF/CSVå¯¼å‡ºæ¨¡å—

### 3.8.1 PDFç”Ÿæˆå™¨

```kotlin
class PdfGenerator(private val context: Context) {
    
    // ç”Ÿæˆå®¢æˆ·åˆ†æžæŠ¥å‘ŠPDF
    suspend fun generateCustomerAnalysisPdf(
        customerName: String,
        analysisContent: String,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4å°ºå¯¸
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }
            
            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }
            
            val headerPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 16f
                isFakeBoldText = true
                isAntiAlias = true
            }
            
            var yPosition = 50f
            val margin = 40f
            val pageWidth = pageInfo.pageWidth.toFloat()
            val pageHeight = pageInfo.pageHeight.toFloat()
            val contentWidth = pageWidth - 2 * margin
            
            // æ ‡é¢˜
            canvas.drawText("å®¢æˆ·åˆ†æžæŠ¥å‘Š", margin, yPosition, titlePaint)
            yPosition += 30f
            
            // å®¢æˆ·åç§°
            canvas.drawText("å®¢æˆ·: $customerName", margin, yPosition, headerPaint)
            yPosition += 25f
            
            // æ—¥æœŸ
            val dateFormat = SimpleDateFormat("yyyyå¹´MMæœˆddæ—¥ HH:mm", Locale.CHINESE)
            canvas.drawText("ç”Ÿæˆæ—¶é—´: ${dateFormat.format(Date())}", margin, yPosition, paint)
            yPosition += 30f
            
            // åˆ†éš”çº¿
            canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, paint)
            yPosition += 20f
            
            // è§£æžMarkdownå†…å®¹å¹¶æ¸²æŸ“
            val lines = parseMarkdownToPdfLines(analysisContent)
            
            for (line in lines) {
                // æ£€æŸ¥æ˜¯å¦éœ€è¦æ–°é¡µé¢
                if (yPosition > pageHeight - 80f) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }
                
                when (line.type) {
                    LineType.HEADING1 -> {
                        yPosition += 10f
                        canvas.drawText(line.text, margin, yPosition, headerPaint)
                        yPosition += 25f
                    }
                    LineType.HEADING2 -> {
                        yPosition += 8f
                        val h2Paint = Paint(headerPaint).apply { textSize = 14f }
                        canvas.drawText(line.text, margin, yPosition, h2Paint)
                        yPosition += 22f
                    }
                    LineType.BULLET -> {
                        canvas.drawCircle(margin + 5f, yPosition - 5f, 3f, paint)
                        drawMultilineText(canvas, line.text, margin + 20f, yPosition, contentWidth - 20f, paint)
                        yPosition += 18f
                    }
                    LineType.NORMAL -> {
                        drawMultilineText(canvas, line.text, margin, yPosition, contentWidth, paint)
                        yPosition += 16f
                    }
                    LineType.BOLD -> {
                        val boldPaint = Paint(paint).apply { isFakeBoldText = true }
                        drawMultilineText(canvas, line.text, margin, yPosition, contentWidth, boldPaint)
                        yPosition += 16f
                    }
                }
            }
            
            document.finishPage(page)
            
            // ä¿å­˜æ–‡ä»¶
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e("PdfGenerator", "Failed to generate PDF", e)
            Result.failure(e)
        }
    }
    
    // ç”Ÿæˆä¼šè®®çºªè¦PDF
    suspend fun generateMeetingMinutesPdf(
        meetingTitle: String,
        date: String,
        participants: String,
        content: String,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                isAntiAlias = true
            }
            
            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }
            
            var yPosition = 50f
            val margin = 40f
            val pageWidth = pageInfo.pageWidth.toFloat()
            val pageHeight = pageInfo.pageHeight.toFloat()
            
            // æ ‡é¢˜
            canvas.drawText("ä¼šè®®çºªè¦", margin, yPosition, titlePaint)
            yPosition += 35f
            
            // ä¼šè®®ä¿¡æ¯
            canvas.drawText("ä¼šè®®ä¸»é¢˜: $meetingTitle", margin, yPosition, paint)
            yPosition += 20f
            canvas.drawText("æ—¥æœŸ: $date", margin, yPosition, paint)
            yPosition += 20f
            canvas.drawText("å‚ä¸Žäºº: $participants", margin, yPosition, paint)
            yPosition += 30f
            
            // åˆ†éš”çº¿
            canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, paint)
            yPosition += 20f
            
            // å†…å®¹
            val lines = parseMarkdownToPdfLines(content)
            val contentWidth = pageWidth - 2 * margin
            
            for (line in lines) {
                if (yPosition > pageHeight - 80f) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }
                
                when (line.type) {
                    LineType.HEADING1, LineType.HEADING2 -> {
                        val headerPaint = Paint(paint).apply { 
                            textSize = if (line.type == LineType.HEADING1) 14f else 12f
                            isFakeBoldText = true 
                        }
                        yPosition += 10f
                        canvas.drawText(line.text, margin, yPosition, headerPaint)
                        yPosition += 20f
                    }
                    LineType.BULLET -> {
                        canvas.drawCircle(margin + 5f, yPosition - 5f, 2f, paint)
                        drawMultilineText(canvas, line.text, margin + 15f, yPosition, contentWidth - 15f, paint)
                        yPosition += 16f
                    }
                    else -> {
                        drawMultilineText(canvas, line.text, margin, yPosition, contentWidth, paint)
                        yPosition += 15f
                    }
                }
            }
            
            document.finishPage(page)
            
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e("PdfGenerator", "Failed to generate meeting minutes PDF", e)
            Result.failure(e)
        }
    }
    
    // ç»˜åˆ¶å¤šè¡Œæ–‡æœ¬
    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint
    ): Float {
        val words = text.split(" ")
        var currentLine = ""
        var currentY = y
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            
            if (width > maxWidth && currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, x, currentY, paint)
                currentY += paint.textSize + 4f
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
        }
        
        return currentY
    }
    
    // è§£æžMarkdownä¸ºPDFè¡Œ
    private fun parseMarkdownToPdfLines(markdown: String): List<PdfLine> {
        val lines = mutableListOf<PdfLine>()
        
        markdown.lines().forEach { line ->
            when {
                line.startsWith("# ") -> {
                    lines.add(PdfLine(line.substring(2), LineType.HEADING1))
                }
                line.startsWith("## ") -> {
                    lines.add(PdfLine(line.substring(3), LineType.HEADING2))
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    lines.add(PdfLine(line.substring(2), LineType.BULLET))
                }
                line.contains("**") -> {
                    // ç®€å•å¤„ç†åŠ ç²—
                    val cleanText = line.replace("**", "")
                    lines.add(PdfLine(cleanText, LineType.BOLD))
                }
                line.isNotBlank() -> {
                    lines.add(PdfLine(line, LineType.NORMAL))
                }
            }
        }
        
        return lines
    }
}

// PDFè¡Œæ•°æ®ç±»
data class PdfLine(
    val text: String,
    val type: LineType
)

enum class LineType {
    HEADING1,
    HEADING2,
    NORMAL,
    BOLD,
    BULLET
}
```

---

### 3.8.2 CSVç”Ÿæˆå™¨

```kotlin
class CsvGenerator {
    
    // ç”ŸæˆCRMå¯¼å…¥CSV
    suspend fun generateCrmCsv(
        customers: List<CustomerData>,
        outputFile: File,
        format: CrmFormat = CrmFormat.SALESFORCE
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val headers = getCrmHeaders(format)
            
            FileWriter(outputFile).use { writer ->
                // å†™å…¥BOMä»¥æ”¯æŒä¸­æ–‡Excel
                writer.write("\uFEFF")
                
                // å†™å…¥è¡¨å¤´
                writer.write(headers.joinToString(",") { escapeCSV(it) })
                writer.write("\n")
                
                // å†™å…¥æ•°æ®
                customers.forEach { customer ->
                    val row = mapCustomerToRow(customer, format)
                    writer.write(row.joinToString(",") { escapeCSV(it) })
                    writer.write("\n")
                }
            }
            
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e("CsvGenerator", "Failed to generate CSV", e)
            Result.failure(e)
        }
    }
    
    // ç”Ÿæˆå¯¹è¯è®°å½•CSV
    suspend fun generateConversationCsv(
        messages: List<MessageEntity>,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            FileWriter(outputFile).use { writer ->
                writer.write("\uFEFF")
                
                // è¡¨å¤´
                writer.write("æ—¶é—´,è§’è‰²,å†…å®¹\n")
                
                // æ•°æ®
                messages.forEach { message ->
                    val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE)
                        .format(Date(message.timestamp))
                    writer.write("${escapeCSV(time)},${escapeCSV(message.role)},${escapeCSV(message.content)}\n")
                }
            }
            
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e("CsvGenerator", "Failed to generate conversation CSV", e)
            Result.failure(e)
        }
    }
    
    // ç”Ÿæˆå®¢æˆ·åˆ—è¡¨CSV
    suspend fun generateCustomerListCsv(
        conversations: List<ConversationEntity>,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            FileWriter(outputFile).use { writer ->
                writer.write("\uFEFF")
                
                // è¡¨å¤´
                writer.write("å¯¹è¯æ ‡é¢˜,åˆ›å»ºæ—¶é—´,æ›´æ–°æ—¶é—´,æ˜¯å¦å·²åˆ†æž\n")
                
                // æ•°æ®
                conversations.forEach { conv ->
                    val created = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINESE)
                        .format(Date(conv.createdAt))
                    val updated = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINESE)
                        .format(Date(conv.updatedAt))
                    val analyzed = if (conv.isAnalyzed) "æ˜¯" else "å¦"
                    
                    writer.write("${escapeCSV(conv.title)},$created,$updated,$analyzed\n")
                }
            }
            
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e("CsvGenerator", "Failed to generate customer list CSV", e)
            Result.failure(e)
        }
    }
    
    // CSVè½¬ä¹‰
    private fun escapeCSV(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
    
    // èŽ·å–CRMè¡¨å¤´
    private fun getCrmHeaders(format: CrmFormat): List<String> {
        return when (format) {
            CrmFormat.SALESFORCE -> listOf(
                "å§“å", "å…¬å¸", "èŒä½", "ç”µè¯", "é‚®ç®±",
                "äº§å“å…´è¶£", "é¢„ç®—", "å†³ç­–æ—¶é—´", "é˜¶æ®µ",
                "ä¼˜å…ˆçº§", "æ ¸å¿ƒéœ€æ±‚", "ç—›ç‚¹", "å¤‡æ³¨"
            )
            CrmFormat.HUBSPOT -> listOf(
                "First Name", "Last Name", "Company", "Job Title",
                "Phone", "Email", "Product Interest", "Budget",
                "Decision Timeline", "Stage", "Priority", "Notes"
            )
            CrmFormat.GENERIC -> listOf(
                "å®¢æˆ·å§“å", "å…¬å¸åç§°", "è”ç³»ç”µè¯", "é‚®ç®±",
                "äº§å“å…´è¶£", "å½“å‰é˜¶æ®µ", "è·Ÿè¿›ä¼˜å…ˆçº§", "å¤‡æ³¨"
            )
        }
    }
    
    // æ˜ å°„å®¢æˆ·æ•°æ®åˆ°CSVè¡Œ
    private fun mapCustomerToRow(customer: CustomerData, format: CrmFormat): List<String> {
        return when (format) {
            CrmFormat.SALESFORCE -> listOf(
                customer.name,
                customer.company,
                customer.jobTitle,
                customer.phone,
                customer.email,
                customer.productInterest,
                customer.budget,
                customer.decisionTimeline,
                customer.stage,
                customer.priority,
                customer.coreNeeds,
                customer.painPoints,
                customer.notes
            )
            CrmFormat.HUBSPOT -> {
                val nameParts = customer.name.split(" ", limit = 2)
                listOf(
                    nameParts.getOrNull(0) ?: "",
                    nameParts.getOrNull(1) ?: "",
                    customer.company,
                    customer.jobTitle,
                    customer.phone,
                    customer.email,
                    customer.productInterest,
                    customer.budget,
                    customer.decisionTimeline,
                    customer.stage,
                    customer.priority,
                    customer.notes
                )
            }
            CrmFormat.GENERIC -> listOf(
                customer.name,
                customer.company,
                customer.phone,
                customer.email,
                customer.productInterest,
                customer.stage,
                customer.priority,
                customer.notes
            )
        }
    }
}

// å®¢æˆ·æ•°æ®ç±»
data class CustomerData(
    val name: String,
    val company: String,
    val jobTitle: String,
    val phone: String,
    val email: String,
    val productInterest: String,
    val budget: String,
    val decisionTimeline: String,
    val stage: String,
    val priority: String,
    val coreNeeds: String,
    val painPoints: String,
    val notes: String
) {
    companion object {
        // ä»ŽJSONå­—ç¬¦ä¸²è§£æž
        fun fromJson(json: String): CustomerData? {
            return try {
                val jsonObject = JSONObject(json)
                CustomerData(
                    name = jsonObject.optString("å®¢æˆ·å§“å", ""),
                    company = jsonObject.optString("å…¬å¸åç§°", ""),
                    jobTitle = jsonObject.optString("èŒä½", ""),
                    phone = jsonObject.optString("è”ç³»ç”µè¯", ""),
                    email = jsonObject.optString("é‚®ç®±", ""),
                    productInterest = jsonObject.optString("äº§å“å…´è¶£", ""),
                    budget = jsonObject.optString("é¢„ç®—èŒƒå›´", ""),
                    decisionTimeline = jsonObject.optString("å†³ç­–æ—¶é—´", ""),
                    stage = jsonObject.optString("å½“å‰é˜¶æ®µ", ""),
                    priority = jsonObject.optString("è·Ÿè¿›ä¼˜å…ˆçº§", ""),
                    coreNeeds = jsonObject.optString("æ ¸å¿ƒéœ€æ±‚", ""),
                    painPoints = jsonObject.optString("ç—›ç‚¹", ""),
                    notes = jsonObject.optString("å¤‡æ³¨", "")
                )
            } catch (e: Exception) {
                Log.e("CustomerData", "Failed to parse JSON", e)
                null
            }
        }
    }
}

// CRMæ ¼å¼
enum class CrmFormat {
    SALESFORCE,   // Salesforceæ ¼å¼
    HUBSPOT,      // HubSpotæ ¼å¼
    GENERIC       // é€šç”¨æ ¼å¼
}
```

---

### 3.8.3 å¯¼å‡ºç®¡ç†å™¨

```kotlin
class ExportManager(
    private val context: Context,
    private val conversationRepository: ConversationRepository,
    private val exportRepository: ExportRepository,
    private val aiServiceManager: AiServiceManager
) {
    private val pdfGenerator = PdfGenerator(context)
    private val csvGenerator = CsvGenerator()
    
    // å¯¼å‡ºå®¢æˆ·åˆ†æžæŠ¥å‘Šä¸ºPDF
    suspend fun exportCustomerAnalysisPdf(
        conversationId: Long,
        customerName: String
    ): Result<File> {
        return try {
            val conversation = conversationRepository.getConversationWithMessages(conversationId)
                ?: return Result.failure(Exception("Conversation not found"))
            
            // æŸ¥æ‰¾åˆ†æžç»“æžœï¼ˆAIåŠ©æ‰‹çš„æœ€åŽå›žå¤ï¼‰
            val analysisMessage = conversation.messages
                .filter { it.role == "assistant" }
                .lastOrNull()
                ?: return Result.failure(Exception("No analysis found"))
            
            val fileName = "å®¢æˆ·åˆ†æž_${customerName}_${System.currentTimeMillis()}.pdf"
            val outputFile = File(getExportDirectory(), fileName)
            
            val result = pdfGenerator.generateCustomerAnalysisPdf(
                customerName = customerName,
                analysisContent = analysisMessage.content,
                outputFile = outputFile
            )
            
            if (result.isSuccess) {
                // è®°å½•å¯¼å‡º
                exportRepository.recordExport(
                    conversationId = conversationId,
                    exportType = "pdf",
                    filePath = outputFile.absolutePath,
                    status = "success"
                )
            }
            
            result
            
        } catch (e: Exception) {
            Log.e("ExportManager", "Failed to export PDF", e)
            Result.failure(e)
        }
    }
    
    // å¯¼å‡ºä¼šè®®çºªè¦ä¸ºPDF
    suspend fun exportMeetingMinutesPdf(
        conversationId: Long,
        meetingTitle: String,
        participants: String,
        transcript: String
    ): Result<File> {
        return try {
            // ä½¿ç”¨AIç”Ÿæˆä¼šè®®çºªè¦
            val summaryResult = aiServiceManager.sendChatMessage(
                conversationId = conversationId,
                userMessage = PromptTemplates.meetingSummaryPrompt(transcript)
            )
            
            if (summaryResult.isFailure) {
                return Result.failure(summaryResult.exceptionOrNull()!!)
            }
            
            val summary = summaryResult.getOrNull()!!
            val dateFormat = SimpleDateFormat("yyyyå¹´MMæœˆddæ—¥", Locale.CHINESE)
            val date = dateFormat.format(Date())
            
            val fileName = "ä¼šè®®çºªè¦_${meetingTitle}_${System.currentTimeMillis()}.pdf"
            val outputFile = File(getExportDirectory(), fileName)
            
            val result = pdfGenerator.generateMeetingMinutesPdf(
                meetingTitle = meetingTitle,
                date = date,
                participants = participants,
                content = summary,
                outputFile = outputFile
            )
            
            if (result.isSuccess) {
                exportRepository.recordExport(
                    conversationId = conversationId,
                    exportType = "pdf",
                    filePath = outputFile.absolutePath,
                    status = "success"
                )
            }
            
            result
            
        } catch (e: Exception) {
            Log.e("ExportManager", "Failed to export meeting minutes", e)
            Result.failure(e)
        }
    }
    
    // å¯¼å‡ºCRMæ•°æ®ä¸ºCSV
    suspend fun exportCrmDataCsv(
        conversationId: Long,
        format: CrmFormat = CrmFormat.GENERIC
    ): Result<File> {
        return try {
            val conversation = conversationRepository.getConversationWithMessages(conversationId)
                ?: return Result.failure(Exception("Conversation not found"))
            
            // åˆå¹¶æ‰€æœ‰å¯¹è¯å†…å®¹
            val transcript = conversation.messages
                .joinToString("\n\n") { "${it.role}: ${it.content}" }
            
            // ä½¿ç”¨AIæå–CRMæ•°æ®
            val extractionResult = aiServiceManager.extractCrmData(conversationId, transcript)
            
            if (extractionResult.isFailure) {
                return Result.failure(extractionResult.exceptionOrNull()!!)
            }
            
            val jsonData = extractionResult.getOrNull()!!
            val customerData = CustomerData.fromJson(jsonData)
                ?: return Result.failure(Exception("Failed to parse customer data"))
            
            val fileName = "CRMå¯¼å‡º_${System.currentTimeMillis()}.csv"
            val outputFile = File(getExportDirectory(), fileName)
            
            val result = csvGenerator.generateCrmCsv(
                customers = listOf(customerData),
                outputFile = outputFile,
                format = format
            )
            
            if (result.isSuccess) {
                exportRepository.recordExport(
                    conversationId = conversationId,
                    exportType = "csv",
                    filePath = outputFile.absolutePath,
                    status = "success"
                )
            }
            
            result
            
        } catch (e: Exception) {
            Log.e("ExportManager", "Failed to export CRM data", e)
            Result.failure(e)
        }
    }
    
    // æ‰¹é‡å¯¼å‡ºå¤šä¸ªå¯¹è¯çš„CRMæ•°æ®
    suspend fun exportBatchCrmData(
        conversationIds: List<Long>,
        format: CrmFormat = CrmFormat.GENERIC
    ): Result<File> {
        return try {
            val customerDataList = mutableListOf<CustomerData>()
            
            for (conversationId in conversationIds) {
                val conversation = conversationRepository.getConversationWithMessages(conversationId)
                    ?: continue
                
                val transcript = conversation.messages
                    .joinToString("\n\n") { "${it.role}: ${it.content}" }
                
                val extractionResult = aiServiceManager.extractCrmData(conversationId, transcript)
                
                if (extractionResult.isSuccess) {
                    val jsonData = extractionResult.getOrNull()!!
                    val customerData = CustomerData.fromJson(jsonData)
                    if (customerData != null) {
                        customerDataList.add(customerData)
                    }
                }
            }
            
            if (customerDataList.isEmpty()) {
                return Result.failure(Exception("No customer data extracted"))
            }
            
            val fileName = "CRMæ‰¹é‡å¯¼å‡º_${System.currentTimeMillis()}.csv"
            val outputFile = File(getExportDirectory(), fileName)
            
            csvGenerator.generateCrmCsv(
                customers = customerDataList,
                outputFile = outputFile,
                format = format
            )
            
        } catch (e: Exception) {
            Log.e("ExportManager", "Failed to batch export", e)
            Result.failure(e)
        }
    }
    
    // å¯¼å‡ºå¯¹è¯è®°å½•ä¸ºCSV
    suspend fun exportConversationCsv(conversationId: Long): Result<File> {
        return try {
            val conversation = conversationRepository.getConversationWithMessages(conversationId)
                ?: return Result.failure(Exception("Conversation not found"))
            
            val fileName = "å¯¹è¯è®°å½•_${conversation.conversation.title}_${System.currentTimeMillis()}.csv"
            val outputFile = File(getExportDirectory(), fileName)
            
            csvGenerator.generateConversationCsv(
                messages = conversation.messages,
                outputFile = outputFile
            )
            
        } catch (e: Exception) {
            Log.e("ExportManager", "Failed to export conversation", e)
            Result.failure(e)
        }
    }
    
    // èŽ·å–å¯¼å‡ºç›®å½•
    private fun getExportDirectory(): File {
        val dir = File(context.getExternalFilesDir(null), "exports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    // åˆ†äº«æ–‡ä»¶
    fun shareFile(file: File, mimeType: String = "application/*"): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
```

---

### 3.8.4 ä¾èµ–é…ç½® (AndroidManifest.xml)

```xml
<!-- FileProvideré…ç½® -->
<application>
    ...
    <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="${applicationId}.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_paths" />
    </provider>
</application>
```

**res/xml/file_paths.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path
        name="exports"
        path="exports/" />
    <files-path
        name="files"
        path="." />
    <cache-path
        name="cache"
        path="." />
</paths>
```

---

## å®Œæˆè¿›åº¦

âœ… **æ•°æ®æŒä¹…åŒ–** - Roomæ•°æ®åº“ã€DAOã€Repository  
âœ… **BLEè¿žæŽ¥æ¨¡å—** - è®¾å¤‡æ‰«æã€é…å¯¹ã€WiFié…ç½®ä¼ è¾“  
âœ… **WiFiæ–‡ä»¶åŒæ­¥** - HTTPå®¢æˆ·ç«¯ã€æ–‡ä»¶ä¸Šä¼ /ä¸‹è½½ã€æ˜¾ç¤ºæŽ§åˆ¶  
âœ… **AIé›†æˆæ¨¡å—** - Qwen DashscopeèŠå¤© + Tingwuè½¬å†™  
âœ… **PDF/CSVå¯¼å‡º** - æ–‡æ¡£ç”Ÿæˆã€CRMæ•°æ®å¯¼å‡º  

## ä¸‹ä¸€æ­¥

æœ€åŽä¸€ä¸ªæ ¸å¿ƒæ¨¡å—ï¼š
**UIå±‚ (Jetpack Compose)** - ä¸»èŠå¤©ç•Œé¢ã€åŽ†å²è®°å½•ã€è®¾å¤‡é…å¯¹ã€æ–‡ä»¶ç®¡ç†

ç»§ç»­æž„å»ºUIï¼Ÿ
