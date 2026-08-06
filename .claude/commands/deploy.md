# Command: /deploy

**Purpose:** Verify and ship a release (or prove the stack runs clean).
**Input:** The release/milestone to ship.
**Output:** A passed pre-flight checklist, green CI, working clean-clone `docker-compose up`, and an updated roadmap.
**Best skills:** `engineering:deploy-checklist`; `superpowers:finishing-a-development-branch`, `verification-before-completion`.

## Steps
1. Read `.claude/rules/deployment.md`, `.claude/rules/security.md`, and `docs/DEPLOYMENT.md`.
2. Follow `.claude/prompts/deploy.md` pre-flight (clean-clone startup, smoke the happy path, config/secrets check).
3. Confirm no doc lies; update `.env.example` for new config.
4. Tag the release; update `docs/ROADMAP.md`.

Never ship with red CI or a broken one-command startup.
