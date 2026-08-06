# Rule: Deployment

Always-on constraints for containerization & CI. See `docs/DEPLOYMENT.md`.

## Always
- The whole stack must start with `docker-compose up --build` (postgres/pgvector, ollama, backend, frontend).
- Read all config from env vars; keep `.env.example` current when config changes.
- Use `depends_on` + healthchecks so backend waits for a healthy Postgres.
- Keep CI (GitHub Actions) green on every PR: backend `mvn verify` + frontend build/test.
- Use multi-stage Docker builds; run the backend as a non-root user on a JRE image.
- Document any new env var in `.env.example` and `docs/DEPLOYMENT.md`.

## Never
- Never commit `.env` or real secrets.
- Never add a required manual setup step that breaks one-command startup.
- Never merge with red CI. Branch protection requires passing checks.

## Profiles
`dev` (local, no Docker), `docker` (compose), `test` (Testcontainers). Never hardcode hostnames — use profile config.

## Work that belongs here
Dockerfiles, `docker-compose.yml`, environment/config, healthchecks, GitHub Actions CI/CD, and release/hosting steps.

## Skills for this area
- **Auto-consult:** `engineering:deploy-checklist` (pre-ship verification). Always read `rules/security` — deployment is where secrets and exposure mistakes happen.
- **Task-specific:** `superpowers:finishing-a-development-branch` when integrating/merging a completed branch.
- **Verify before done:** clean-clone `docker-compose up` must work; `superpowers:verification-before-completion`.
- **Ignore:** frontend/backend build-logic skills and design skills. Update `docs/DEPLOYMENT.md` + `.env.example` for any config change.
