# Command: /design-api

**Purpose:** Design an endpoint or a group of endpoints (contracts) before implementing.
**Input:** The capability + rough request/response idea.
**Output:** Finalized request/response records, status codes, validation rules, and an updated `docs/API.md` + Swagger plan.
**Best skills:** `engineering:system-design` (API/contract design); verify with `engineering:code-review`.

## Steps
1. Read `.claude/rules/api.md`, `.claude/rules/security.md`, and `docs/API.md`.
2. Define DTOs (records) + validation, status codes (per API.md table), pagination, and the error envelope.
3. Confirm consistency with `AnalysisResponse` and existing conventions.
4. Update `docs/API.md`. Hand off to `/add-endpoint` (or `/new-feature`) to implement.

Design first, code second. Every endpoint is authenticated-by-default and documented.
