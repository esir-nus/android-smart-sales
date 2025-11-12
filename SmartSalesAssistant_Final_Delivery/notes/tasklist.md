## 🧩 `tasklist.md` — *Granular TDD Execution Rules*  *(≈ 470 words)*

**Purpose**
Provide a repeatable structure for breaking work into micro-tasks that reflect Vibe + TDD principles. Each task is one behavior proven by one test.

---

### 1. Task Format

```
- [ ] <Type>-<ID>  <Short intent>  → expected initial state = failing
      Test: <file/class>
      Impl: <target file>
      Done when: test passes & suite green
```

**Type codes:**
T = unit I = implementation R = refactor UI = Compose/UI DB = Room Perf = benchmark

---

### 2. Creation Rules

1. Start every feature or bug by listing 5–12 atomic behaviors.
2. Each task expresses an observable outcome, *not* an activity.
3. Write the corresponding **failing test first**; implementation follows.
4. If a task exceeds ~50 LOC or multiple modules → split.
5. End each day with 1–2 queued “next smallest failing tests”.

---

### 3. Execution Loop

1. **Select one unchecked task.**
2. **Write failing test.** Confirm suite shows red.
3. **Implement minimal code.**
4. **Run tests.** Expect green.
5. **Refactor lightly.**
6. **Commit:**

   ```
   feat(scope): reason [test:<ID>]
   ```
7. **Mark task complete.**

---

### 4. Example Segment

```
- [ ] T-1  Repo returns cached devices when BT off  → fail
- [ ] I-1  Implement cache-return logic  → green
- [ ] R-1  Extract DeviceMapper  → refactor green
- [ ] T-2  Scan stops after 10s  → fail
- [ ] I-2  Add timeout + lifecycle cancel  → green
```

---

### 5. Validation Gates

* Every completed task must:

  * Pass its own test and full suite.
  * Respect permissions, lifecycle, and privacy constraints.
  * Update documentation or comments only after green.
* No task closes with known failing tests or disabled checks.

---

### 6. Cross-Feature Templates

**Bluetooth:** bounded scan, secure connect, retry backoff.
**Wi-Fi:** join AP, captive-portal detect, retry schedule.
**AI:** model load, deterministic output, latency guard.
Each of these expands into unit + instrumented + UI + perf tasks.

---

### 7. PR / Merge Checklist

* [ ] All tasks complete & checked.
* [ ] All tests green (unit/instrumented/UI).
* [ ] Lint/Detekt clean.
* [ ] Commit history shows one behavior per commit.
* [ ] `tasklist.md` updated with final states.

---

### 8. Principle

> A task is complete only when the behavior is proven, not when the code looks finished.
> The checklist is the rhythm; the tests are the truth.

---

These two files together keep your **AI-assisted Android project** fast, test-anchored, and self-documenting—perfect for both human and agent workflows.
