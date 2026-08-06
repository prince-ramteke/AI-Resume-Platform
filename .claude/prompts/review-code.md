# Prompt: Review code

Use this to review a change (a diff, a PR, or recent work) before merging.

---

**Scope:** <files / PR / `git diff`>

## Review against
1. **Correctness** — does it do what the story/AC requires? Edge cases handled?
2. **Layering** — Controller→Service→Repository respected? DTOs at boundaries? No entity leaks?
3. **Security** — auth on the endpoint? ownership check? input validated? secrets safe? LLM output validated? prompt-injection safe?
4. **Database** — migration present + correct? queries paginated? vector dim centralized? no N+1?
5. **Errors** — go through the global handler? no swallowed exceptions? no leaked stack traces?
6. **Tests** — meaningful unit + integration tests? error paths covered? LLM mocked? coverage gate met?
7. **Docs** — API.md/DATABASE.md/.env.example updated if contract/config changed?
8. **Style** — conventions in CODING_STANDARDS.md? naming? logging (no secrets)?

## Output
List findings by severity: **Blocker / Major / Minor / Nit**. For each: file:line, the problem, and a concrete fix. Confirm whether the Definition of Done (CLAUDE.md) is met. Be specific; approve only if it truly passes.

## Skills to use
- **Primary:** `engineering:code-review` (security/perf/correctness).
- **For heavier PRs (manual):** `pr-review-toolkit:review-pr` or `code-review:code-review`.
- **Receiving feedback:** `superpowers:receiving-code-review` (verify before implementing suggestions).
- **Skip:** build/design skills — review reads code, it doesn't add features.
