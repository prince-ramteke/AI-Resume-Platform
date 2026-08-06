# Prompt: Refactor

Use this to improve code structure without changing behavior.

---

**Target:** <class / package / area>
**Goal:** <readability / remove duplication / fix layering / performance>

## Do this in order
1. **Safety net first.** Ensure tests cover the current behavior. If coverage is thin, add characterization tests BEFORE refactoring.
2. **Confirm green.** `./mvnw verify` passes before you touch anything.
3. **Refactor in small steps.** One structural change at a time (extract method, introduce interface, move to correct layer, remove duplication). Re-run tests after each step.
4. **No behavior change.** The public API and `AnalysisResponse` shape stay identical. If behavior must change, that's a feature, not a refactor — stop and switch prompts.
5. **Verify.** Tests still green, coverage not reduced. Diff is purely structural.
6. **Docs.** Update only if internal structure referenced in docs changed.
7. **Summarize.** What improved and why; confirm no behavior changed.

## Constraints
Respect all `.claude/rules/*.md`. Prefer many small commits. Don't mix refactor + feature + bugfix in one change.

## Skills to use
- **Plan:** `engineering:tech-debt` (identify + prioritize what to refactor).
- **Support:** the area rule for the code being refactored; `rules/testing` for the safety net.
- **Finish:** `engineering:code-review` + `superpowers:verification-before-completion` (prove no behavior changed).
- **Skip:** anything that would change behavior — that's a feature, not a refactor.
