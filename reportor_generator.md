# Reportor Generator Workflow

This document captures the backup instructions for producing the Chinese status report whenever the command "invoke reportor" is issued.

## Phase 1: Information Gathering
1. Review relevant documentation (Documentation/*.md, testing guides, dependency policies) for new changes.
2. Inspect git status/commits and current backlog items to understand recently completed work and in-progress tasks.
3. Collect build/test metrics (or note if blocked by environment/dependencies):
   - `./gradlew assembleDebug`
   - `./gradlew :wifiBleTestApp:assembleDebug`
   - `./gradlew lint`
   - `./gradlew testDebugUnitTest`
   - Coverage percentage and lint warnings (if available)
4. Confirm WiFi/BLE child project regression status and AI module progress.
5. Note documentation edits, dependency/version updates (or explicitly state "no updates").
6. Capture today's actual work items, next-day plan, and major issues (mark as solved or in-progress).
7. Update milestone progress (completed, in-progress with percentage, upcoming).

## Phase 2: Report Template
Always output in Chinese, technician tone, concise, and with language accessible to non-Android stakeholders.

```
已完成进展：
- ...
本周计划进度：
- ✅ ...
- 🔄 ...
- ⚪ ...
今日任务：
- ...
次日计划：
- ...
- 问题播报（仅列重大问题）：
- ✔ 已解决 — 简述问题及解决措施/效果（只记录真正影响发布的项）
- 🔄 处理中 — 简述问题本身，不延伸下一步
- 🔄 处理中 — 问题描述
项目里程碑：
- [已完成] ...
- [进行中 xx%] ...
- [待启动] ...
```

## Usage
When "invoke reportor" is requested, run Phase 1 to gather up-to-date information, then populate the Phase 2 template with the findings.
