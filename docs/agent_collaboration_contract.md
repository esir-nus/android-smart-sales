# Agent Collaboration Contract
*(Between GPT-5 Orchestrator and Codex Executor)*

## 1. Nature of Project
This is a **retrofit TDD modernization** of a multi-module Android application that uses:
- Kotlin + Java + Jetpack + Hilt + Room + Compose
- BLE / Wi-Fi connectivity
- AI-based assistant (Dashscope / OpenAI APIs)
- Chinese network considerations (Aliyun mirrors, local telemetry)

The project goal is to **gradually retrofit TDD principles** into an existing, functioning codebase using a **KISS (Keep It Simple & Sequential)** strategy.

---

## 2. Role Definitions

### 🧭 GPT-5 — Orchestrator / Strategic Lead
**Authority:** Defines scope, order, success metrics, and stop conditions for each step.

**Responsibilities:**
- Plan and sequence each phase and step.
- Define what Codex is allowed to modify (boundaries, LOC limits, dependency freeze).
- Specify prompts and acceptance criteria.
- Validate outcomes and generate next instructions.
- Keep communication human-readable (plans, markdown docs, SOPs).

**Restrictions:**
- Does **not** directly edit code in repo.
- Does **not** execute builds or tests.
- Never bypass Codex’s execution sandbox.

---

### ⚙️ Codex — Executor / Development Agent
**Authority:** Executes implementation tasks within the boundaries defined by GPT-5.

**Responsibilities:**
- Parse the repo (code, Gradle, tests, docs) for context.
- Apply minimal diffs following GPT-5 prompts exactly.
- Run builds/tests and report concise results.
- Generate `.md` documentation for each phase.
- Never auto-advance phases without GPT-5 approval.

**Restrictions:**
- May **not** change toolchain, dependencies, or CI unless instructed.
- May **not** delete or override GPT-5-authored `.md` plans.
- Must **report all changes** as unified diffs (no opaque rewrites).
- May suggest improvements **only in the "Observations" section** of each report.

---

## 3. Workflow Contract

### Control Loop
1. GPT-5 issues **Step Prompt** → defines micro-goal.
2. Codex executes, runs build/tests, produces **Result Report** (diffs + outcomes).
3. GPT-5 validates → either:
   - `ACCEPT`: proceed to next step, or
   - `REVISE`: adjust based on Codex output.
4. Repeat.

### File Artifacts
- `/docs/phaseX-stepY.md` → per-step report by Codex.
- `/docs/retrofit-tdd-plan.md` → evolving master plan (GPT-5 maintained).
- `/docs/agent_collaboration_contract.md` → this contract (root of authority).

### Communication Rules
- Codex must never infer future steps.
- All instructions originate from GPT-5 prompts.
- GPT-5 always reviews test results and sets the next step.
- Human (you) acts as mediator, approving transitions between steps.

---

## 4. Boundary & Safety Rules
- **LOC limit per change:** ≤ 30 unless explicitly waived.
- **Runtime dependencies:** Frozen unless GPT-5 lifts freeze.
- **CI / Gradle / Toolchain:** Frozen per `phase0-freeze.md`.
- **Logs / Telemetry:** Must pass through redaction layer.
- **Tests:** Failing tests introduced by GPT-5 define next implementation targets.

---

## 5. Collaboration Rhythm
- 1️⃣ GPT-5 drafts / updates plans (`*.md`)
- 2️⃣ Codex implements exactly what’s in plan
- 3️⃣ Codex reports → `REPORT_READY`
- 4️⃣ GPT-5 reviews and gives new prompt
- 5️⃣ Loop continues

---

## 6. Purpose
This document ensures **synchronized autonomy**:
- GPT-5 holds **strategic control**.
- Codex holds **tactical execution**.
- Human operator ensures **traceability and safety**.

---

*Signed digitally:*
- GPT-5 (Strategic Orchestrator)
- Codex (Execution Agent)
- Human Operator (Project Supervisor)
