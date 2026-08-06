# Skill Routing Map
## AI Resume Intelligence Platform

> The single source of truth for **which skill to use for which task**. Claude consults this map (via `CLAUDE.md`) to auto-select the right guidance. Most of the installed skill catalog is irrelevant to this project and is listed in §5 as **do-not-use-by-default**.
>
> **Golden rule:** use the *fewest* skills that fit the task. When unsure, prefer this project's own `docs/` and `.claude/rules/` over any external skill.

---

## 1. How routing works

1. Claude detects the **area** of the task from the files/keywords (backend, frontend, database, api, security, testing, deployment, docs).
2. It reads the matching `.claude/rules/<area>.md` and relevant `docs/*.md`.
3. It consults **only** the skills mapped to that area below (Priority P1/P2).
4. Review/verification skills (P3) run before "done".
5. Everything in §5 is ignored unless the user explicitly asks for it.

Priority key: **P1** = consult first when working in this area · **P2** = task-specific, use when the trigger matches · **P3** = review/verify or manual only.

---

## 2. Core skill routing (relevant to this project)

| Skill | What it does | When to use | Applies to (files/tasks) | Do NOT use for | Trigger words | Priority |
|---|---|---|---|---|---|---|
| **superpowers:brainstorming** | Explores intent/requirements before building | Before ANY new feature or non-trivial change | New feature kickoff, ambiguous requests | Tiny fixes, typo/format changes | "build", "add feature", "let's create", "design a" | P1 |
| **superpowers:writing-plans** | Turns a spec into a phased plan | Multi-step work before touching code | Feature/milestone planning | Single-file trivial edits | "plan", "roadmap", "break this down" | P1 |
| **engineering:architecture** | Creates/evaluates an ADR (trade-offs) | Choosing tech or documenting a design decision | System-wide decisions, `docs/SYSTEM_ARCHITECTURE.md`, ADRs | Routine CRUD, styling | "Kafka vs", "should we use", "trade-off", "decision" | P2 |
| **engineering:system-design** | Designs APIs, data models, service boundaries | Designing an endpoint, schema, or module boundary | Backend, API, Database tasks | UI styling, docs prose | "design a system", "data model", "service boundary", "API design" | P1 (backend/api/db) |
| **engineering:debug** | Structured reproduce→isolate→fix | A defect/stack trace/unexpected behavior | Bug fixing across layers | Building new features | "bug", "error", "broken", "works in X not Y" | P1 (bugs) |
| **superpowers:systematic-debugging** | Disciplined debugging before proposing fixes | Any failing test or unexpected behavior | Bug fixing, flaky tests | New feature work | "failing test", "why is this", "unexpected" | P1 (bugs) |
| **engineering:testing-strategy** | Designs test plans & coverage | Deciding what/how to test | `TESTING.md`, new endpoints/services | Pure styling, docs | "test plan", "how to test", "coverage" | P1 (testing) |
| **superpowers:test-driven-development** | TDD: test before implementation | Implementing a feature/bugfix test-first | Service/RAG logic | Throwaway spikes | "TDD", "test first", "write the test" | P2 (testing) |
| **engineering:code-review** | Reviews for security/perf/correctness | Before merging any change | Any diff/PR, security review | Early drafting | "review", "is this safe", "before I merge" | P3 |
| **superpowers:verification-before-completion** | Forces evidence before "done" | Right before claiming a task complete | Every non-trivial change | N/A | "done", "complete", "passing" | P3 |
| **superpowers:requesting-code-review** | Structures a self-review pre-merge | Completing a feature/milestone | Feature completion | Trivial edits | "ready to merge", "review my work" | P3 |
| **engineering:tech-debt** | Identifies & prioritizes refactors | Planning cleanup/refactor | Refactoring tasks | New features, bug fixes | "tech debt", "refactor", "clean up", "code health" | P2 (refactor) |
| **engineering:deploy-checklist** | Pre-deploy verification checklist | Before shipping / verifying the stack | `docker-compose`, CI, release | Day-to-day coding | "deploy", "release", "ship", "go live" | P1 (deploy) |
| **engineering:documentation** | Writes technical docs/README/runbooks | Authoring or updating docs | `docs/`, `README.md`, runbooks | Code logic | "document", "write docs", "README", "runbook" | P1 (docs) |
| **frontend-design:frontend-design** | Distinctive, intentional UI direction | Building/reshaping UI look & feel | React pages/components, styling | Backend, DB, API | "UI", "design the page", "look and feel" | P1 (frontend) |
| **ui-styling** | shadcn/ui + Tailwind component styling | Implementing accessible styled components | `frontend/` components, theming | Backend, DB | "style this", "component", "tailwind", "dark mode" | P2 (frontend) |
| **impeccable** | Critiques/polishes a frontend interface | Improving an existing UI's UX/polish | Existing React UI, results screen | Backend logic | "polish", "improve the UI", "make it cleaner" | P2 (frontend) |
| **ui-ux-pro-max** | UI/UX pattern + palette + a11y database | Choosing layout/color/motion/a11y patterns | Frontend design decisions | Backend, DB, deploy | "color palette", "layout", "accessibility", "font pairing" | P3 (frontend, optional) |

---

## 3. Document-format skills (output only, manual)

Use **only** when the deliverable is literally that file type — never for writing code or in-repo markdown.

| Skill | Use when | Trigger | Priority |
|---|---|---|---|
| **docx** | Producing a Word document | ".docx", "Word doc" | P3 manual |
| **pdf** | Producing or reading a PDF | ".pdf", "export PDF report" | P3 manual |
| **pptx** | Producing a slide deck | ".pptx", "presentation", "deck" | P3 manual |
| **xlsx** | Producing a spreadsheet | ".xlsx", "spreadsheet", "CSV report" | P3 manual |

> In-repo docs stay as `.md` (handled by `engineering:documentation`). These format skills are for exporting artifacts a stakeholder asked for.

---

## 4. Plugin review commands (manual, on request)

| Skill/Command | Use when | Priority |
|---|---|---|
| **code-review:code-review** | Reviewing a specific GitHub PR | P3 manual |
| **pr-review-toolkit:review-pr** | Deep multi-agent PR review | P3 manual |
| **feature-dev:feature-dev** | Guided feature dev walkthrough (alt to our /new-feature) | P3 manual |

Prefer this repo's own `.claude/commands/` and `.claude/prompts/` first; reach for these only for heavier, external PR-centric flows.

---

## 5. Do-NOT-use-by-default (ignored unless explicitly requested)

These are installed but **out of scope** for building this application. Never auto-apply them.

| Group | Skills | Why excluded here |
|---|---|---|
| Personal job-search | resume-diagnoser, resume-recruiter, resume-rewriter, resume-hiring-manager, the-diagnoser, the-recruiter, the-rewriter, the-hiring-manager | These improve *your* resume/interview prep. This project *builds software that analyzes resumes* — different thing. Use them for your own job hunt, not the codebase. |
| Brand/marketing design | brand, banner-design, design, slides, design-is | Marketing/visual-identity work, not product engineering. |
| Ops rituals | engineering:incident-response, engineering:standup | For live incidents / daily standups, not building v1. |
| Memory & plugin tooling | claude-mem:* (timeline, digests, version-bump, mode-creator, etc.) | Meta-tooling unrelated to app features. |
| Advanced/meta workflow | superpowers:writing-skills, using-superpowers, using-git-worktrees, dispatching-parallel-agents, subagent-driven-development, executing-plans | Powerful but manual; invoke deliberately, never automatically. |
| Utility | skill-creator, find-skills, setup-cowork, morning, schedule, explain-usage, cowork-plugin-management:*, claude-opus-4-5-migration | Environment/config utilities, not project work. |
| Design tokens (borderline) | design-system | Only if we formalize a token system later; optional, manual. |

---

## 6. Quick area → skills lookup

| If Claude is working on... | Auto-consult (P1/P2) | Verify with (P3) |
|---|---|---|
| **Backend** (controllers/services/repos/DTOs) | rules/backend, rules/api, rules/security, rules/database, rules/testing · engineering:system-design | engineering:code-review, verification-before-completion |
| **Frontend** (React/UI/forms/state) | rules/frontend, rules/api, rules/testing · frontend-design, ui-styling | impeccable, verification-before-completion |
| **Database** (schema/entities/migrations/indexes) | rules/database, rules/backend · engineering:system-design | engineering:code-review |
| **API** (endpoints/contracts/Swagger) | rules/api, rules/security, rules/backend · engineering:system-design | engineering:code-review |
| **Security** (auth/JWT/roles/secrets/CORS) | rules/security, rules/api · (rules-first) | engineering:code-review |
| **Testing** (unit/integration/edge/regression) | rules/testing · engineering:testing-strategy, TDD | verification-before-completion |
| **Deployment** (Docker/Compose/CI/CD) | rules/deployment, rules/security · engineering:deploy-checklist | verification-before-completion |
| **Docs** (README/PRD/arch/roadmap) | rules/documentation · engineering:documentation | (self-review) |
| **New feature (any)** | superpowers:brainstorming → writing-plans → area skills above | requesting-code-review, verification |
| **Bug** | engineering:debug / systematic-debugging → area skills | verification-before-completion |
| **Refactor** | engineering:tech-debt → area skills | engineering:code-review |

---

## 7. Maintenance
When a new skill is installed, add one row here and decide its priority + area before Claude uses it. If a skill is never triggered in practice, move it to §5. Keep this map short — routing that no one reads is worse than no routing.
