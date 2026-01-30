# GUIA (Success Cases and Replicable Paths)

Document Name: GUIA
Status: Append-only by convention
Document Version: 1.0.0
Created (Local Time - America/Monterrey): 2026-01-30

## 1) Purpose

Record only:

- Success cases: what worked and how to replicate it reliably.
- Errors encountered: what failed and what was learned, specifically leading to a successful outcome.

This prevents repeated trial-and-error and supports deterministic iteration.

## 2) Rules

- Append-only by convention:
  - Do not rewrite prior entries.
  - If something becomes outdated, append a deprecation note in a new entry.
- Each success case must include:
  - Preconditions
  - Steps
  - Why it worked (minimal)
  - How to replicate
  - Verification method (build/compile, runtime check)
- Each error note must include:
  - Symptom
  - Cause (if confirmed)
  - Fix
  - Prevention

## 3) Entry Template (Success Case)

- Timestamp (America/Monterrey):
- Title:
- Goal:
- Context:
- Preconditions:
- Steps:
- Result:
- Replication Steps:
- Verification:
- Related BOT Log Reference (date/summary):
- Status: [active | superseded | deprecated]

## 4) Entry Template (Error -> Success)

- Timestamp (America/Monterrey):
- Title:
- Symptom:
- Root Cause (confirmed):
- Fix Applied:
- Why it fixed the issue:
- Prevention:
- Verification:
- Related Success Case:
- Status: [active | superseded | deprecated]

---

# ENTRIES (Append-Only)
