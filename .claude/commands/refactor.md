# Command: /refactor

**Purpose:** Improve code structure with zero behavior change.
**Input:** A target (class/package/area) and a goal (readability, dedupe, layering, perf).
**Output:** A structural-only diff, tests still green, coverage not reduced.
**Best skills:** `engineering:tech-debt` (prioritize), `engineering:code-review`, `verification-before-completion`.

## Steps
1. Ensure a test safety net exists (add characterization tests first if thin).
2. Follow `.claude/prompts/refactor.md` in small, verified steps.
3. Prove no behavior changed (`AnalysisResponse` and public APIs unchanged).

If behavior must change, stop — that's `/new-feature`, not a refactor.
