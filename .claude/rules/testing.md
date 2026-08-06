# Rule: Testing

Always-on testing constraints. See `docs/TESTING.md` for strategy.

## Always
- Write JUnit 5 + Mockito unit tests for new/changed logic, with real assertions.
- Mock `LlmClient`/`EmbeddingClient` and repositories in unit tests — no network, deterministic.
- Use `FakeLlmClient`/`FakeEmbeddingClient` for RAG tests; cover the repair-retry and evidence-grounding paths.
- Add integration tests (`@SpringBootTest` + Testcontainers pgvector) for new endpoints/queries.
- Cover error/edge paths: invalid input (400), not owner (403), unauthenticated (401), malformed LLM output (422).
- Run `./mvnw verify` (tests + JaCoCo gate) green before pushing.

## Never
- Never call a live LLM or external network in tests.
- Never mark work done with failing/partial tests.
- Never write a test that only asserts "no exception thrown".
- Never drop coverage below the gate (service ≥80%, overall ≥75%).

## Naming
`XxxTest` (unit), `XxxIT` (integration). Methods: `method_condition_expected`. Arrange–Act–Assert.

## Work that belongs here
Unit tests, integration tests (Testcontainers), edge/error-path tests, regression tests, deterministic AI test doubles, and the coverage gate.

## Skills for this area
- **Auto-consult:** `engineering:testing-strategy` (what to test, coverage shape).
- **Task-specific:** `superpowers:test-driven-development` when implementing test-first; `superpowers:systematic-debugging` when a test is failing for an unknown reason.
- **Verify before done:** `superpowers:verification-before-completion` — never claim green without running `./mvnw verify`.
- **Ignore:** frontend/design and doc-format skills (unless testing the frontend, then pair with `rules/frontend`).
