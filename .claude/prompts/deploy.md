# Prompt: Deploy / release

Use this before shipping a release or verifying the stack runs clean. See `docs/DEPLOYMENT.md`.

---

**Release:** <version / milestone>

## Pre-flight checklist
1. **Green build.** `./mvnw verify` and frontend build/test pass locally; CI green on the branch.
2. **Clean-clone test.** From a fresh checkout: copy `.env.example`→`.env`, set secrets, run `docker-compose up --build`. Everything starts; `/actuator/health` green; Ollama models pulled.
3. **Smoke the happy path.** Register → login → upload resume → add JD → run analysis → see grounded result → history. Via UI and/or Swagger.
4. **Config check.** All new env vars in `.env.example` + `docs/DEPLOYMENT.md`. No secrets committed.
5. **Docs current.** API.md/DATABASE.md/ROADMAP.md reflect reality. README demo steps + screenshots up to date.
6. **Migrations.** Flyway applies cleanly on an empty DB.
7. **Rollback note.** Know how to revert (previous image/tag).

## Do NOT
- Ship with red CI, failing clean-clone startup, or a doc that lies.
- Add a manual step that breaks one-command startup.

## After
Tag the release, update `ROADMAP.md` milestone status, summarize what shipped.

## Skills to use
- **Primary:** `engineering:deploy-checklist` (pre-ship verification).
- **Support:** `rules/deployment`, `rules/security`; `superpowers:finishing-a-development-branch` for the merge/integration step.
- **Finish:** `superpowers:verification-before-completion` (clean-clone startup proven).
- **Skip:** feature/design skills — deployment ships what exists, it doesn't build new logic.
