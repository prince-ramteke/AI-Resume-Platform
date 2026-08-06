# Command: /security-review

**Purpose:** Audit auth, access control, input/AI safety, and secret handling.
**Input:** A feature, endpoint, or the whole app before a release.
**Output:** A findings list (auth gaps, missing ownership checks, injection risks, secret leaks) with fixes, mapped to the threat model.
**Best skills:** `engineering:code-review` (checks injection/auth/error gaps). Rules-first: `docs/SECURITY.md`.

## Steps
1. Read `.claude/rules/security.md`, `.claude/rules/api.md`, and `docs/SECURITY.md`.
2. Walk the hardening checklist: anon rejected, USER can't reach others' data, admin route 403 for USER, no secret logging, prompt-injection resistance, file-type/size validation.
3. For each gap: severity + concrete fix + a security test to add.
4. Confirm the threat-model table still holds.

Never relax a security rule for convenience. Prompt-injection and grounding checks are mandatory.
