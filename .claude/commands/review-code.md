# Command: /review-code

**Purpose:** Review a change before merge for correctness, security, and quality.
**Input:** A diff, PR, or "review my recent work".
**Output:** Findings by severity (Blocker/Major/Minor/Nit) with file:line + concrete fixes, and a pass/fail against the Definition of Done.
**Best skills:** `engineering:code-review`; manual heavy: `pr-review-toolkit:review-pr`, `code-review:code-review`.

## Steps
1. Load `.claude/rules/` for the touched areas + `docs/API.md`/`SECURITY.md` if relevant.
2. Follow `.claude/prompts/review-code.md` checklist.
3. Output findings; approve only if it truly passes.

Review reads code; it does not add features or change behavior.
