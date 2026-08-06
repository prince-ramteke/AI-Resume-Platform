# Command: /fix-bug

**Purpose:** Diagnose and fix a defect at its root, with a regression test.
**Input:** A symptom, expected behavior, and where it happens (stack trace helps).
**Output:** A minimal root-cause fix + a regression test + green `./mvnw verify`.
**Best skills:** `engineering:debug`, `superpowers:systematic-debugging`, `verification-before-completion`.

## Steps
1. Read `.claude/rules/` for the affected area and any relevant `docs/*.md`.
2. Follow `.claude/prompts/fix-bug.md` end to end (reproduce → isolate → fix → prove).
3. Add the regression test; run `.claude/prompts/write-tests.md` if coverage is thin.
4. Report root cause, fix, test added, and any sibling risks.

Never patch the symptom. Never mark done without the failing test now passing.
