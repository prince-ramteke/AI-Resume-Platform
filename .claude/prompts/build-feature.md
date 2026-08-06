# Prompt: Build a feature

Use this when implementing a new feature. Fill in the blanks, then follow the steps in order.

---

**Feature:** <name / what the user can do>
**Related docs:** PRD user story <ID>, API.md endpoint(s), DATABASE.md tables
**Milestone:** <M?>

## Do this in order
1. **Read first.** Load `CLAUDE.md`, the relevant `docs/*.md`, and the matching `.claude/rules/*.md`. Read a sibling feature package to match patterns.
2. **Plan.** List the files you'll create/modify (controller, service, repository, DTOs, mapper, migration, tests, frontend). State the approach. Do NOT code yet — confirm the plan is consistent with the docs.
3. **Data layer.** Add/adjust the Flyway migration and JPA entity (per DATABASE.md). Keep dimensions/constants centralized.
4. **Service.** Business logic + ownership checks + `@Transactional`. LLM/embeddings via the abstraction only.
5. **API.** Request/response records with Bean Validation → controller → Swagger annotations. Match API.md; update it if the contract changes.
6. **Tests.** Unit (mock repos + LLM) covering happy + error paths; integration test for the endpoint (Testcontainers). Cover 400/401/403 and any 422.
7. **Frontend** (if user-facing): typed API function in `src/api/`, hook, component with loading/empty/error states.
8. **Verify.** Run `./mvnw verify` (green + coverage gate). Update docs touched.
9. **Summarize.** What changed, how to verify, any follow-ups.

## Constraints
Obey all `.claude/rules/*.md`. Controller→Service→Repository. DTOs at boundaries. Validate input. Validate LLM output. No secrets. Definition of Done in CLAUDE.md must be fully met before claiming completion.

## Skills to use
- **Start:** `superpowers:brainstorming` → `superpowers:writing-plans`.
- **During:** `engineering:system-design` for the design; the area rules for the code (backend/api/security/database/frontend/testing per what you touch).
- **Finish:** `superpowers:requesting-code-review` + `engineering:code-review`, then `superpowers:verification-before-completion`.
- **Skip:** anything in `docs/SKILL_ROUTING_MAP.md` §5.
