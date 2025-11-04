# Product Requirements Document: Smart Sales Assistant

## 1. Project Overview

### 1.1 Product Vision
An AI-powered sales assistant system combining a hardware voice recorder gadget with an Android companion app, enabling sales professionals to capture, transcribe, analyze customer interactions, and generate actionable insights.

### 1.2 Target Users
- B2B/B2C sales representatives
- Account managers
- Business development professionals

### 1.3 Key Value Propositions
- Hands-free meeting recording via hardware gadget
- AI-powered transcription and customer analysis
- Structured output for CRM integration
- Visual feedback on gadget screen

---

## 2. System Architecture

### 2.1 Components
1. **Hardware Gadget**: Voice recorder with 80×80 display, WiFi, BLE
2. **Android App**: Companion app for sync, AI processing, export

### 2.2 Communication Protocols
- **BLE**: Initial pairing, WiFi credential transmission
- **WiFi**: File sync, remote control (same network required)
- **HTTP**: Gadget hosts server for file access

---

## 3. Functional Requirements

### 3.1 Initial Setup & Pairing

#### FR-1.1: BLE Device Discovery
- **Description**: App scans for nearby gadgets via BLE
- **Acceptance Criteria**:
  - Display list of discoverable gadgets
  - Show device name and signal strength
  - Timeout after 30 seconds

#### FR-1.2: WiFi Credential Transfer
- **Description**: Send WiFi SSID and password to gadget via BLE
- **Acceptance Criteria**:
  - Support multiple WiFi profiles storage
  - Encrypt credentials during transmission
  - Confirm successful connection on gadget
  - Handle connection failures gracefully

---

### 3.2 Main Chat Interface

#### FR-2.1: AI Conversation
- **Description**: Chat-style interface for interacting with Qwen AI
- **Acceptance Criteria**:
  - Real-time message streaming
  - Support text input
  - Support audio file upload (from gadget or local)
  - Support image upload (for context)
  - Display markdown-formatted responses

#### FR-2.2: Skill Buttons (Selection Mode)
- **Description**: Three action buttons for workflow selection
- **Buttons**:
  1. **分析客户 (Analyze Customer)**: Tag conversation for customer profiling
  2. **生成PDF (Generate PDF)**: Mark output for PDF export
  3. **生成CSV (Generate CSV)**: Mark data for CRM export
- **Acceptance Criteria**:
  - Buttons highlight when selected (multi-select allowed)
  - Affect AI prompt context (instruct AI on output format)
  - No immediate execution (selection only)

#### FR-2.3: File Upload
- **Description**: Upload audio/image files for AI analysis
- **Acceptance Criteria**:
  - Support .mp3, .wav, .m4a audio formats
  - Support .jpg, .png, .gif image formats
  - Display upload progress
  - Handle file size limits (max 100MB audio, 10MB image)

---

### 3.3 History Management

#### FR-3.1: Conversation History
- **Description**: List all past chat sessions with time grouping
- **Acceptance Criteria**:
  - Group by: "7天内" (7 days), "30天内" (30 days), "2025年9月" (monthly)
  - Time indicators sticky during scroll
  - Display conversation title/preview
  - Persist across app restarts

#### FR-3.2: Session Revisit
- **Description**: Click history item to reload conversation
- **Acceptance Criteria**:
  - Load full conversation context
  - Restore uploaded files (if available)
  - Maintain chronological message order

#### FR-3.3: Delete History
- **Description**: Long-press to delete conversation
- **Acceptance Criteria**:
  - Confirmation dialog before deletion
  - Remove from local database
  - Cannot undo deletion

---

### 3.4 File Viewer & Sync

#### FR-4.1: Gadget File Listing
- **Description**: View audio/image files stored on gadget
- **Acceptance Criteria**:
  - HTTP request to gadget server for file list
  - Display: filename, timestamp, location tag (optional)
  - Separate audio and image sections
  - Refresh on pull-down gesture

#### FR-4.2: Manual Sync
- **Description**: "同步" button to fetch latest file list
- **Acceptance Criteria**:
  - Only enabled when on same WiFi as gadget
  - Show sync progress indicator
  - Update file list after successful sync
  - Trigger auto-sync on app launch

#### FR-4.3: File Download
- **Description**: Click file to download to app storage
- **Acceptance Criteria**:
  - Download progress notification
  - Success toast: "Added to clipboard" (or similar)
  - Store in app-specific directory
  - Playable from app after download

---

### 3.5 Editor (Gadget Control)

#### FR-5.1: Image Upload to Gadget
- **Description**: Select and upload image for gadget display
- **Acceptance Criteria**:
  - Pick from local storage
  - Auto-resize to 80×80 pixels
  - Compress to <50KB
  - Preview before upload
  - Send via HTTP POST to gadget

#### FR-5.2: Text Display Control
- **Description**: Edit text shown on gadget screen
- **Acceptance Criteria**:
  - Text input field (10 character limit)
  - UTF-8 support (Chinese characters)
  - Real-time character count
  - Send to gadget via HTTP request

#### FR-5.3: Network Requirement
- **Description**: Editor only works on same WiFi
- **Acceptance Criteria**:
  - Disable features if not on same network
  - Display warning message
  - Re-enable when WiFi connection restored

---

### 3.6 AI Processing

#### FR-6.1: Audio Transcription (Qwen Tingwu)
- **Description**: Upload audio to Qwen Tingwu for processing
- **Acceptance Criteria**:
  - Support long audio files (up to 2 hours)
  - Output: full transcription with timestamps
  - Speaker diarization (identify different speakers)
  - Summarization (key points extraction)
  - Language detection (Chinese/English)

#### FR-6.2: Customer Analysis (Qwen Dashscope)
- **Description**: AI-powered customer profiling based on conversation
- **Acceptance Criteria**:
  - Analyze: customer needs, pain points, decision factors
  - Structured markdown output with sections:
    - 客户画像速写 (Customer Profile)
    - 核心销售策略 (Sales Strategy)
    - 沟通偏好 (Communication Style)
    - 决策阻力 (Decision Barriers)
  - Bold key terms for scannability

---

### 3.7 Export Features

#### FR-7.1: PDF Generation
- **Description**: Convert markdown output to printable PDF
- **Acceptance Criteria**:
  - Preserve markdown formatting (headers, lists, bold)
  - Include: logo, timestamp, conversation title
  - A4 page size
  - Save to Downloads folder
  - Share via Android share sheet

#### FR-7.2: CSV Export (CRM-ready)
- **Description**: Extract customer data to CSV format
- **Acceptance Criteria**:
  - Columns: Name, Company, Contact, Needs, Stage, Notes
  - UTF-8 BOM encoding (Excel compatibility)
  - Comma-separated values
  - Save to Downloads folder
  - Share via Android share sheet

---

## 4. Non-Functional Requirements

### 4.1 Performance
- App startup: <2 seconds on mid-range devices
- Chat response latency: <3 seconds for text, <10 seconds for audio transcription
- File sync: <5 seconds for file list, proportional download time

### 4.2 Reliability
- Offline mode: Cache last 100 conversations
- Auto-retry failed HTTP requests (max 3 attempts)
- Graceful degradation if AI service unavailable

### 4.3 Security
- Encrypt BLE communication (AES-128)
- HTTPS for all API calls
- Local database encryption (SQLCipher)
- No cloud storage of audio files (user privacy)

### 4.4 Usability
- Support Chinese (Simplified) and English
- Accessibility: TalkBack support, minimum touch target 48dp
- Dark mode support

### 4.5 Compatibility
- Android 8.0+ (API 26+)
- Screen sizes: 5" to 7" phones
- BLE 4.2+ required

---

## 5. User Stories

### US-1: Sales Meeting Recording
**As a** sales representative  
**I want to** record customer meetings hands-free  
**So that** I can focus on the conversation without taking notes

### US-2: Post-Meeting Analysis
**As a** sales manager  
**I want to** review AI-generated meeting summaries  
**So that** I can quickly understand customer needs and plan follow-ups

### US-3: CRM Integration
**As a** sales operations specialist  
**I want to** export customer data to CSV  
**So that** I can bulk-update our CRM system efficiently

### US-4: Device Personalization
**As a** user  
**I want to** customize the gadget's display  
**So that** it shows my logo during client meetings

---

## 6. Technical Specifications

### 6.1 Gadget HTTP API (Assumptions)

#### Endpoints:
```
GET  /api/files/list          → JSON array of files
GET  /api/files/download?name=xxx.mp3 → File download
POST /api/display/image       → Upload image (multipart/form-data)
POST /api/display/text        → Update text (JSON body)
GET  /api/status              → Gadget health check
```

#### File List Response Format:
```json
{
  "audio": [
    {"name": "audio_20251030154100.mp3", "size": 1024000, "timestamp": "2025-10-30T15:41:00Z", "location": "福田"}
  ],
  "images": [
    {"name": "logo.gif", "size": 45000, "timestamp": "2025-10-25T10:00:00Z"}
  ]
}
```

### 6.2 BLE Service Specification

#### Service UUID: 
`0000fff0-0000-1000-8000-00805f9b34fb` (example)

#### Characteristics:
- **WiFi Config (Write)**: `0000fff1-...`  
  Format: `SSID:PASSWORD` (UTF-8 string)
- **Status (Notify)**: `0000fff2-...`  
  Values: `0x00` (idle), `0x01` (connecting), `0x02` (connected), `0xFF` (error)

### 6.3 Qwen API Integration

#### Dashscope Chat API:
```kotlin
POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
Headers: Authorization: Bearer YOUR_API_KEY
Body: {
  "model": "qwen-max",
  "input": {"messages": [...]},
  "parameters": {"result_format": "message"}
}
```

#### Tingwu Audio API:
```kotlin
POST https://tingwu.aliyuncs.com/api/v1/audio/transcribe
Headers: X-Tingwu-Key: YOUR_API_KEY
Body: multipart/form-data (audio file)
```

---

## 7. Development Phases

### Phase 1: Core Infrastructure (Week 1-2)
- Project setup (Compose, Hilt, Room)
- BLE communication module
- HTTP client for gadget API
- Local database schema

### Phase 2: Main Features (Week 3-5)
- Chat interface (UI + Qwen integration)
- History management
- File viewer + sync logic
- Editor (image/text upload)

### Phase 3: AI Integration (Week 6-7)
- Qwen Tingwu transcription
- Customer analysis prompts
- Markdown rendering

### Phase 4: Export & Polish (Week 8-9)
- PDF generation
- CSV export
- Error handling
- UI/UX refinement

### Phase 5: Testing & Launch (Week 10)
- Unit tests (80% coverage)
- Integration tests (BLE + HTTP)
- Beta testing with 10 users
- Production release

---

## 8. Success Metrics

### KPIs:
- User retention: >70% after 30 days
- Average sessions per day: >3
- Transcription accuracy: >90% (Chinese)
- Crash-free rate: >99%
- App rating: >4.5 stars

---

## 9. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Gadget BLE unreliable | High | Implement retry logic, fallback to manual WiFi setup |
| Qwen API rate limits | Medium | Implement request queuing, consider caching |
| File sync failures | Medium | Local caching, manual retry option |
| Large audio processing time | Low | Show progress, allow background processing |

---

## 10. Open Questions

1. **Gadget API Documentation**: Need full API spec from hardware team
2. **Qwen API Keys**: Obtain production keys and understand rate limits
3. **CRM CSV Format**: Define standard fields for major CRM systems
4. **Monetization**: Free tier vs. premium features?
5. **Multi-gadget Support**: Can one app pair with multiple gadgets?

---

## 11. Appendix

### A. Wireframe References
- Image 1: Main interface with chat and skill buttons
- Image 2: Side menu with history navigation
- Image 3: Structured output example
- Image 4: History page with time grouping
- Image 5: Editor interface
- Image 6: File viewer with sync button

### B. Dependencies
- Minimum Android Studio: Flamingo (2022.2.1)
- Kotlin: 1.9+
- Compose BOM: 2024.02.00+
- Target libraries: See Tech Stack (Step 1)
