# Prompt: Write tests

Use this to add or backfill tests. See `docs/TESTING.md`.

---

**Target:** <class / feature / endpoint>
**Current coverage:** <if known>

## Do this in order
1. **Map the surface.** List public methods/endpoints and their behaviors: happy path, each error/edge path.
2. **Unit tests (JUnit 5 + Mockito).** Mock repositories and `LlmClient`/`EmbeddingClient`. For the RAG core, use `FakeLlmClient`/`FakeEmbeddingClient`:
   - grounding: dangling `evidenceRef` gets stripped
   - repair: malformed JSON → one retry → success, else 422
   - chunking: sizes/overlap/metadata correct
   - ownership: non-owner rejected
3. **Integration tests.** `@SpringBootTest` + Testcontainers pgvector for endpoints/queries. Assert status codes: 200/400/401/403/404/422. Include a vector-search smoke test.
4. **Assertions with teeth.** Assert real values/shape, not just "no exception".
5. **Run the gate.** `./mvnw verify` — meet service ≥80%, overall ≥75%.
6. **Summarize.** What's now covered, remaining gaps.

## Constraints
No live network/LLM in tests. Deterministic fakes only. Naming: `XxxTest`/`XxxIT`, `method_condition_expected`.

## Skills to use
- **Primary:** `engineering:testing-strategy` (coverage design); `superpowers:test-driven-development` when writing test-first.
- **Support:** `rules/testing`; the area rule for the code under test.
- **Finish:** `superpowers:verification-before-completion` (`./mvnw verify` green + gate met).
- **Skip:** design/deployment skills.
