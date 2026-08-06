# Command: /ship-milestone

Workflow to close out a roadmap milestone.

**Usage:** `/ship-milestone <M?>`

## Steps
1. Open `docs/ROADMAP.md`; read the milestone's Definition of Done.
2. Verify each DoD item with evidence (tests, running stack, screenshots).
3. Run `.claude/prompts/deploy.md` pre-flight (clean-clone `docker-compose up`, smoke the happy path).
4. Confirm CI green, coverage gate met, docs current.
5. Update the milestone status in `docs/ROADMAP.md`.
6. Summarize what shipped and what's next.

Do not mark a milestone done until every DoD item is proven, not assumed.
