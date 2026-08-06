# Rule: Documentation

Always-on constraints for keeping docs truthful.

## Always
- Treat `docs/*.md` as the source of truth. If code and a doc disagree, fix one — deliberately — in the same change.
- Update the relevant doc when behavior or contract changes:
  - New/changed endpoint → `docs/API.md` (+ Swagger).
  - Schema change → `docs/DATABASE.md` (+ Flyway migration).
  - New dependency/decision → `docs/TECH_STACK.md`.
  - New env var → `docs/DEPLOYMENT.md` + `.env.example`.
  - Milestone progress → `docs/ROADMAP.md`.
- Keep the top-level `README.md` demo-ready: setup, one-command run, screenshots, architecture diagram, trade-offs.
- Write comments that explain *why*, not *what*. Delete stale comments.

## Never
- Never ship a behavior change that leaves a doc lying.
- Never document a feature that doesn't exist yet as if it does (mark as planned/stretch).
- Never leave `AnalysisResponse` (the core contract) undocumented after a change.

## Work that belongs here
README, PRD, architecture, roadmap, changelog, runbooks, and keeping `docs/*.md` truthful.

## Skills for this area
- **Auto-consult:** `engineering:documentation` (structure and clarity for technical docs).
- **Manual only:** the doc-format skills — `docx`, `pdf`, `pptx`, `xlsx` — used **only** when a stakeholder asks for that exact file type as an export. In-repo docs stay `.md`.
- **Ignore:** all engineering/design build skills. Docs describe the system; they don't build it.
