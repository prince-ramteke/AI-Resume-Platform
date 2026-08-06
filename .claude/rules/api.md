# Rule: API

Always-on constraints for REST endpoints. See `docs/API.md` for the contract.

## Always
- Base path `/api`. JSON in/out. Auth via `Authorization: Bearer <JWT>` except public routes.
- Public routes only: `/api/auth/**`, `/actuator/health`, Swagger. Everything else is authenticated.
- Return the standard error envelope `{timestamp, status, error, message, traceId}` for all errors.
- Use correct status codes (400/401/403/404/409/413/422/500 — see API.md table).
- Validate request DTOs; return `400` with field messages on violation.
- Document every endpoint in springdoc/Swagger and keep `docs/API.md` current.
- Paginate list endpoints (`page`, `size`, `sort`).

## Never
- Never expose entities or internal fields (password hash, raw vectors) in responses.
- Never return raw, unvalidated LLM text as an API result.
- Never add an endpoint without auth unless it's explicitly whitelisted.
- Never break the response shape of `AnalysisResponse` without bumping/documenting it.

## New endpoint checklist
DTOs + validation → service method (ownership check) → controller → Swagger annotations → tests (happy + 400 + 401/403) → update `docs/API.md`.

## Work that belongs here
Endpoint design, request/response contracts, status codes, validation rules, pagination, error envelope, and Swagger/OpenAPI.

## Skills for this area
- **Auto-consult:** `engineering:system-design` (API/contract design). Always read `rules/security` (every endpoint is an attack surface) and `rules/backend`.
- **Verify before done:** `engineering:code-review`.
- **Ignore:** frontend/design and deployment skills. A contract change must update `docs/API.md` and Swagger — no skill substitutes for that.
