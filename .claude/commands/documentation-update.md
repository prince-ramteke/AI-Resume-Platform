# Command: /documentation-update

**Purpose:** Keep docs truthful after a change (README, PRD, architecture, API, roadmap, changelog).
**Input:** What changed in the code/contract/config.
**Output:** The affected `docs/*.md`, `README.md`, and `.env.example` updated to match reality.
**Best skills:** `engineering:documentation`. Doc-format skills (`docx`/`pdf`/`pptx`/`xlsx`) only if exporting that file type.

## Steps
1. Read `.claude/rules/documentation.md`.
2. Map the change to docs: endpoint → `API.md`+Swagger; schema → `DATABASE.md`; dependency/decision → `TECH_STACK.md`; env var → `DEPLOYMENT.md`+`.env.example`; milestone → `ROADMAP.md`.
3. Update the docs; keep prose (no bloat); mark unbuilt features as planned.
4. Confirm no doc now contradicts the code.

In-repo docs stay `.md`. Never document a not-yet-built feature as if it exists.
