# Command: /add-endpoint

Workflow to add or change a single REST endpoint.

**Usage:** `/add-endpoint <METHOD> <path> — <purpose>`

## Steps
1. Read `.claude/rules/api.md`, `.claude/rules/security.md`, and `docs/API.md`.
2. Design the request/response records + validation; confirm status codes against API.md.
3. Implement: DTOs → service method (with ownership check) → controller → Swagger annotations.
4. Update `docs/API.md` with the endpoint and any schema change.
5. Tests: happy path + 400 (validation) + 401 (anon) + 403 (not owner/role). Integration via Testcontainers.
6. `./mvnw verify`; summarize.

Never ship an endpoint that is unauthenticated-by-default, undocumented, or untested.
