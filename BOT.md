# BOT (Cognitive Orchestration System)

Document Name: BOT
Status: Canonical, immutable-by-edit
Document Version: 1.0.0
Created (Local Time - America/Monterrey): 2026-01-30

## 1) Purpose

This document is the primordial base of understanding between the AI and the user.
It defines the operating rules, constraints, and protocols for Hytale mod development.

## 2) Immutability and Governance (Append-Only)

- This document must never be deleted.
- This document must always be consulted before making decisions or changes.
- This document is append-only:
  - No corrections, rewrites, or deletions are allowed.
  - Any change in rules or direction must be recorded as a new log entry.
  - Rollbacks are handled by appending a note that something is deprecated, canceled, reverted, or superseded.

## 3) Non-Contamination Rule (Hytale Only)

- The project is 100% focused on Hytale.
- Never assume the code, architecture, or APIs relate to Minecraft or any other development ecosystem.
- Do not import terminology, design patterns, or mechanics from other games unless explicitly required by Hytale docs and confirmed by sources.

## 4) Language Contract

- User-facing explanations: Spanish.
- Programming artifacts (code, identifiers, filenames): English.
- Code comments: minimal, ASCII only, no emojis.

## 5) Development Focus

- Primary domain: Hytale mod development.
- Architecture: ECS (data-oriented), not OOP.
  - Entities are ids (e.g., int).
  - Components are data-only.
  - Systems contain logic and operate over component sets.

## 6) Java Runtime Baseline

- Java JDK: 25
- This must be used until:
  - The user modifies this document, OR
  - The Java section explicitly states otherwise via a new append-only log entry.

## 7) Sources and Verification (No Invention Policy)

- Sources must be researched before stating API behavior.
- Do not invent programming behaviors, API signatures, lifecycle hooks, or server behaviors.
- The authoritative API and documentation source is:
  <https://github.com/Henry-Bonikowsky/hytale-docs>
- If something is uncertain, it must be verified in sources or asked to the user (with concrete questions).

## 8) Repository and Publishing Policy

- No changes may be published to the cloud (GitHub push, PR, releases, remote sync) unless the user explicitly permits it.
- Default stance: local-only changes and guidance.

## 9) Build / Compilation Requirement

- After finishing any change (code or configuration), the project must be compiled/build-verified.
- The exact build command depends on the project toolchain (Gradle/Maven/other).
- If the build command is not known, it must be defined as part of the next change and recorded in the log.

## 10) Loop Avoidance and Action Log Consultation

- Before proposing or executing a step, consult the action log in this document.
- Objective:
  - Avoid repeating identical actions.
  - Avoid falling into loops.
  - Keep decisions consistent with prior recorded context.

## 11) Multimedia Inputs

- If any multimedia is required (images, audio, video, diagrams), the user must provide it.
- The assistant must request it with clear specs:
  - format (png/jpg/wav/etc),
  - dimensions/resolution,
  - duration (if media),
  - and any required content constraints.

## 12) Uncertainty Handling

- If there are doubts in any aspect:
  - Request additional information from the user, including links to similar repositories or concrete references to analyze.
  - Do not guess.

## 13) System Output Standard

- Provide code in fenced blocks.
- Keep explanations short and concrete unless the user requests expansion.
- Keep ECS alignment explicit (data-only components, logic in systems).

## 14) Git Exclusions and Pre-Compilation

- Non-relevant files (build artifacts, IDE configs, logs, etc.) must always be excluded via `.gitignore`.
- The `.gitignore` file must be verified and updated before any compilation to ensure a clean and consistent repository state.

---

# LOG (Append-Only)

All entries below are immutable. Add new entries at the end only.

## Log Entry Template

- Timestamp (America/Monterrey):
- Actor:
- Type: [rule-change | decision | implementation | rollback | deprecation | build | research]
- Summary:
- Details:
- Affected artifacts:
- Verification (build/compile):
- Status: [active | superseded | deprecated | canceled]

## 2026-01-30

- Timestamp (America/Monterrey): 2026-01-30
- Actor: user
- Type: rule-change
- Summary: Established BOT as canonical append-only contract for Hytale ECS mod development.
- Details:
  - BOT is never deletable and always consulted.
  - BOT is append-only (no edits/deletes); rollbacks recorded via deprecation/cancel notes.
  - Hytale-only; no Minecraft or other ecosystem assumptions.
  - Java baseline fixed to JDK 25 until BOT changes.
  - Sources must be researched; no invented API behavior; authoritative docs link provided.
  - No GitHub/cloud publishing without explicit user approval.
  - Always compile/build verify after changes.
  - Maintain version control expectations with future config changes respected.
  - Consult action log to avoid loops/repetition.
  - Multimedia must be requested from user with format/measure specs.
  - If doubts arise, request similar repos or specific repos for analysis.
  - Spanish explanations, English code.
- Affected artifacts: BOT.md (this document)
- Verification (build/compile): not applicable (documentation-only)
- Verification (build/compile): N/A
- Status: active

## 2026-01-30 (Entry 3)

- Timestamp (America/Monterrey): 2026-01-30 14:52
- Actor: AI (Antigravity)
- Type: implementation
- Summary: Corrected README.md formula and JSON key consistency.
- Details:
  - Fixed incomplete parentheses in the hunger depletion formula.
  - Aligned the `ItemHungerRestoration` and `ItemMaxHungerSaturation` keys in the example to match the documentation table and `HHMFoodValuesConfig.java` current implementation.
- Affected artifacts: README.md
- Verification (build/compile): Manual verification of code-to-doc alignment. Build verification pending (requires local 'libs' dependencies).
- Status: active

## 2026-01-30 (Entry 4)

- Timestamp (America/Monterrey): 2026-01-30 14:55
- Actor: AI (Antigravity)
- Type: research
- Summary: Prepared libs folder and waiting for HytaleServer.jar.
- Details:
  - Created `libs` directory for external dependencies.
  - Acknowledged that `HytaleServer.jar` will be placed manually by the user.
  - Attempted to fetch dependencies from suggested repos; determined they contain source/docs, not pre-compiled JARs.
- Affected artifacts: /libs/
- Verification (build/compile): PENDING (Waiting for HytaleServer.jar)
- Status: active

## 2026-01-30 (Entry 5)

- Timestamp (America/Monterrey): 2026-01-30 15:05
- Actor: AI (Antigravity)
- Type: build
- Summary: Successful project compilation with manual dependencies.
- Details:
  - Verified presence of `HytaleServer.jar` and `MultipleHUD-1.0.4.jar` in `libs/`.
  - Executed `clean build` using Gradle and Java JDK 25.
  - Build confirmed as **SUCCESSFUL**.
- Affected artifacts: Project binaries (build/ folder)
- Verification (build/compile): SUCCESS (Gradle build)
- Status: active

## 2026-01-30 (Entry 6)

- Timestamp (America/Monterrey): 2026-01-30 15:10
- Actor: AI (Antigravity)
- Type: implementation
- Summary: Renamed project from HytaleHungerMod to Aqua-Thirst-hunger.
- Details:
  - Updated `settings.gradle.kts` (rootProject.name).
  - Renamed main class file to `AquaThirstHunger.java`.
  - Refactored all class references from `HytaleHungerMod` to `AquaThirstHunger`.
  - Updated `build.gradle.kts` manifest generation to point to the new main class.
- Affected artifacts: settings.gradle.kts, build.gradle.kts, src/main/java/**/*
- Verification (build/compile): SUCCESS (Gradle build passing)
- Status: active

## 2026-01-30 (Entry 7)

- Timestamp (America/Monterrey): 2026-01-30 15:15
- Actor: AI (Antigravity)
- Type: rollback
- Summary: Reverted project renaming from Entry 6.
- Details:
  - Reverted `settings.gradle.kts` rootProject.name to `HytaleHungerMod`.
  - Renamed `AquaThirstHunger.java` back to `HytaleHungerMod.java`.
  - Refactored all class references back to `HytaleHungerMod`.
  - Reverted `build.gradle.kts` manifest main class to `es.xcm.hunger.HytaleHungerMod`.
  - Entry 6 is now considered SUPERSEDED/CANCELED.
- Affected artifacts: settings.gradle.kts, build.gradle.kts, src/main/java/**/*
- Verification (build/compile): SUCCESS (Gradle build passing)
- Status: active

## 2026-01-30 (Entry 8)

- Timestamp (America/Monterrey): 2026-01-30 15:20
- Actor: AI (Antigravity)
- Type: implementation
- Summary: Created 'forkok' save point and reapplied Aqua-Thirst-hunger renaming.
- Details:
  - Created Git tag `forkok` as a stable recovery point.
  - Re-implemented renaming from `HytaleHungerMod` to `Aqua-Thirst-hunger`.
  - Process: (1) Bulk string replacement across all files, (2) Renamed main class file, (3) Updated build scripts.
  - Refactored all internal references to use `AquaThirstHunger`.
  - Updated `build.gradle.kts` manifest website and main class.
- Affected artifacts: Global source, settings.gradle.kts, build.gradle.kts
- Verification (build/compile): SUCCESS (Gradle build passing cleanly)
- Status: active
