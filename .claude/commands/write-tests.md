# Command: /write-tests

**Purpose:** Add or backfill meaningful tests to the coverage bar.
**Input:** A target class/feature/endpoint (and current coverage if known).
**Output:** Unit + integration tests covering happy and error paths; `./mvnw verify` green with the gate met.
**Best skills:** `engineering:testing-strategy`, `superpowers:test-driven-development`, `verification-before-completion`.

## Steps
1. Read `.claude/rules/testing.md` and `docs/TESTING.md`.
2. Follow `.claude/prompts/write-tests.md`: map surface → unit (mock LLM/DB) → integration (Testcontainers) → run gate.
3. Report what's now covered and any remaining gaps.

No live network/LLM in tests. Deterministic fakes only.
