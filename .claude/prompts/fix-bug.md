# Prompt: Fix a bug

Use this to diagnose and fix a defect. Reproduce before fixing; add a regression test.

---

**Symptom:** <what happens>
**Expected:** <what should happen>
**Where:** <endpoint / class / screen, stack trace if any>

## Do this in order
1. **Reproduce.** Write a failing test (or a precise manual repro) that demonstrates the bug. Do not fix anything yet.
2. **Isolate.** Trace the failure through the layers. Identify the true root cause, not the symptom. State it explicitly.
3. **Fix at the root.** Make the smallest change that addresses the cause. Respect the layering and rules.
4. **Prove it.** The failing test now passes. Add/keep it as a regression test. Run `./mvnw verify`.
5. **Check for siblings.** Does the same bug pattern exist elsewhere (e.g., another feature missing the ownership check)? Fix or note them.
6. **Docs.** If behavior/contract was wrong in a doc, correct it.
7. **Summarize.** Root cause, fix, test added, any related risks.

## Constraints
No band-aid fixes that hide the symptom. No silent catch blocks. Keep the change focused. All `.claude/rules/*.md` still apply.

## Skills to use
- **Primary:** `engineering:debug` and/or `superpowers:systematic-debugging` (reproduce → isolate → fix).
- **Support:** the area rule for wherever the bug lives; `rules/testing` for the regression test.
- **Finish:** `superpowers:verification-before-completion`.
- **Skip:** design/frontend-aesthetic skills unless the bug is visual.
