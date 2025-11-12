Perfect 👍 Here’s your **developer-facing Markdown specification**, clean, structured, and ready for AI or engineering use.
It combines the ASCII UI blueprints and behavior logic — everything self-contained.

---

# 🧩 Sales Assistant App – Interface Specification (Markdown Version)

## 1️⃣ Main Chat Interface (`/chat`)

```
┌──────────────────────────────────────────────┐
│ 16:30  新对话                              ○ │
│ ≡ (menu)                           ＋ (new) │
├──────────────────────────────────────────────┤
│                    LOGO )                    │
│                                              │
│ Hi XXX                                       │
│ 我是你的销售助手                             │
│                                              │
│ 我可以为你：                                 │
│  - 分析用户画像、渠道、痛点                   │
│  - 生成 PDF、CSV 文档、思维导图（占位）       │
│                                              │
│ 开始我们的任务吧                             │
│                                              │
│ （空白对话区，显示消息气泡）                 │
│                                              │
├──────────────────────────────────────────────┤
│ 上传本地文件并发送消息                       │
│ [分析客户] [生成PDF] [生成CSV] [+ 上传]       │
└──────────────────────────────────────────────┘
```

### **Functions**

| Element                | Description                                                                                         |
| ---------------------- | --------------------------------------------------------------------------------------------------- |
| `≡` (Hamburger)        | Opens sidebar menu (see Interface 2).                                                               |
| `＋ (New)`              | Starts a new session.                                                                               |
| `分析客户 / 生成PDF / 生成CSV` | Mode-selection buttons (do not execute instantly). The chosen mode sets context for next operation. |
| `＋ 上传`                 | Opens local file picker. Supports: images (.png, .jpg, .jpeg, .webp) and audio (.mp3).              |
| Chat area              | Displays user/AI messages and uploaded content as chat bubbles.                                     |

---

## 2️⃣ Sidebar Menu (`/menu`)

```
┌──────────────────────────────────────────────┐
│ ← tap right gray area to close               │
│▕██████████████████ (dimmed main view)        │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │  ≡  AI 助手                           │  │
│  │                                        │  │
│  │  历史记录  ›                           │  │
│  │  文件文件 (占位)                        │  │
│  │  编辑器   (占位)                        │  │
│  │                                        │  │
│  │                                   用户A │  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

### **Functions**

| Element                      | Description                               |
| ---------------------------- | ----------------------------------------- |
| `历史记录`                       | Opens the History Page (see Interface 4). |
| `文件文件`                       | Placeholder for File Manager module.      |
| `编辑器`                        | Placeholder for Markdown Editor module.   |
| `用户A`                        | User account and settings entry.          |
| Gray overlay / Hamburger tap | Closes the menu drawer.                   |

---

## 3️⃣ Structured Output Chat (`/chat/structured`)

```
┌──────────────────────────────────────────────┐
│ ≡  新对话                              ＋ │
├──────────────────────────────────────────────┤
│ [输入框气泡] 请分析该用户                     │
│                                              │
│ ── AI 回复 (结构化文本预览) ─────────────── │
│ # 🧑‍💼 客户「老王」个性化销售跟进方案          │
│ ## 一、客户画像速写                          │
│ 性格：开朗、幽默                              │
│ 兴趣：家庭、车                                │
│ 痛点：效率、成本                              │
│ ## 二、核心销售策略                          │
│ 1. 沟通风格定位：使用故事化、轻松语气          │
│ 2. 关键价值点：强调稳定性、售后支持            │
│ （更多结构化内容继续显示…）                   │
│ ──────────────────────────────────────────── │
├──────────────────────────────────────────────┤
│ 上传文件或发送消息                           │
│ [分析客户] [生成PDF] [生成CSV] [+ 上传]       │
└──────────────────────────────────────────────┘
```

### **Functions**

| Element         | Description                                                                |
| --------------- | -------------------------------------------------------------------------- |
| Structured text | AI outputs Markdown-formatted content (headings, bullets, emphasis).       |
| `生成PDF`         | Converts Markdown to printable PDF (A4 layout, titles, auto page breaks).  |
| `生成CSV`         | Extracts structured CRM fields (e.g., name, company, intent, plan) to CSV. |
| Images          | Ignored during export.                                                     |
| Code blocks     | Not supported / not generated.                                             |

---

## 4️⃣ History Page (`/history`)

```
┌──────────────────────────────────────────────┐
│ ≡  历史记录                                   │
├──────────────────────────────────────────────┤
│  7天内 (sticky)                              │
│  ─────────────────────────────────────────    │
│  2025-11-08  王总  询问新品                   │
│  2025-11-07  小A   询问底价                   │
│  2025-11-06  XX公司  采购询价                 │
│  2025-11-06  跟进刘总  需求                   │
│                                               │
│  30天内 (sticky)                              │
│  ─────────────────────────────────────────    │
│  2025-10-12  刘工  产品定制                   │
│  2025-10-03  公司例会  新品计划               │
│                                               │
│  2025年9月 (sticky)                           │
│  ─────────────────────────────────────────    │
│  2025-09-18  出差杭州  见XX公司客户            │
│  2025-09-02  参加XX经销商  培训                │
└──────────────────────────────────────────────┘
```

### **Functions**

| Element                         | Description                                     |
| ------------------------------- | ----------------------------------------------- |
| **Title format**                | `YYYY-MM-DD  <人物/组织>  <主题≤6字>`                  |
| **Chronological order**         | Strict descending order (latest first).         |
| **Sticky headers**              | “7天内 / 30天内 / 月份” stay visible while scrolling. |
| **Tap**                         | Reopens the corresponding conversation.         |
| **Long-press**                  | Option to delete the record.                    |
| **No search/filter/export all** | Simplicity prioritized.                         |

---

## ⚙️ Global Behavior Summary

| Category             | Details                                                                                |
| -------------------- | -------------------------------------------------------------------------------------- |
| **File upload**      | Supports `.mp3` audio and common image formats (`.png`, `.jpg`, `.jpeg`, `.webp`).     |
| **Skill buttons**    | Mode selectors; execution occurs upon next confirmation or AI prompt.                  |
| **Export formats**   | - **PDF:** From structured Markdown (A4, printable). <br> - **CSV:** CRM-style fields. |
| **No extra modules** | No state saving, accessibility layer, or i18n. English-only UI.                        |
| **Session model**    | Every conversation autosaves to History list (not editable in place).                  |

---


