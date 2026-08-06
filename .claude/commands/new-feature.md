# Command: /new-feature

Workflow to add a complete vertical feature slice.

**Usage:** `/new-feature <feature name>`

## Steps
1. Load `CLAUDE.md`, relevant `docs/*.md`, and every file in `.claude/rules/`.
2. Open `.claude/prompts/build-feature.md` and follow it end to end for `<feature name>`.
3. When implementation is done, run `.claude/prompts/write-tests.md` to fill any test gaps.
4. Run `.claude/prompts/review-code.md` on the diff.
5. Only then report completion, with verification evidence (`./mvnw verify` output summary).

Enforces: plan-first, docs-as-truth, tests + review before "done".
