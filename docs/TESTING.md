# Testing Strategy
## AI Resume Intelligence Platform

> What we test, how, and to what bar. JUnit 5 + Mockito + Testcontainers.

---

## 1. Philosophy

Test behavior, not implementation. Every bug fixed gets a regression test. AI calls are **deterministic in tests** — no live model, no network. The suite must run green in CI on every PR and block merge on failure.

---

## 2. The test pyramid

```
        ┌───────────────┐
        │  E2E (few)     │  full flow via HTTP + real DB (Testcontainers)
        ├───────────────┤
        │ Integration    │  controllers + security + repositories + Flyway
        │ (some)         │
        ├───────────────┤
        │ Unit (many)    │  services, RAG steps, validation, mappers
        └───────────────┘
```

Most value sits in **unit tests of the service/RAG layer** and a solid band of **integration tests** for controllers, security, and persistence.

---

## 3. What each layer covers

### Unit (JUnit 5 + Mockito)
- Services: business rules, ownership checks, error paths. Mock repositories and `LlmClient`/`EmbeddingClient`.
- RAG steps: chunking (sizes/overlap/metadata), evidence-grounding validator (drops unsupported claims), JSON parsing + repair-retry logic.
- Mappers (entity↔DTO), the global exception handler mapping.

### Integration (`@SpringBootTest` + Testcontainers)
- **Postgres + pgvector** in a container (`pgvector/pgvector:pg16`). Flyway migrations run against it.
- Controllers via `MockMvc`/`WebTestClient`: status codes, validation `400`s, auth `401/403`.
- Security: public routes open; protected routes reject anon; USER blocked from admin + others' data.
- Repository queries incl. a smoke test of vector search returning nearest chunks.

### E2E (thin)
- Register → login → upload resume → add JD → run analysis → fetch history. One or two happy-path flows, LLM faked.

---

## 4. Faking the AI (critical pattern)

```java
// test double — no network, deterministic
class FakeLlmClient implements LlmClient {
    private final String cannedJson;
    FakeLlmClient(String cannedJson) { this.cannedJson = cannedJson; }
    public LlmResponse complete(LlmRequest r) { return new LlmResponse(cannedJson, Usage.zero()); }
    public String providerName() { return "fake"; }
}

class FakeEmbeddingClient implements EmbeddingClient {
    public float[] embed(String t) { return deterministicVector(t); } // e.g. hash→vector
    public int dimensions() { return 768; }
}
```

- Inject fakes via `@TestConfiguration` or constructor. Never hit Ollama/OpenAI in tests.
- Test the **repair path** by feeding malformed JSON and asserting a retry then success/`422`.
- Test **grounding** by returning a verdict with a dangling `evidenceRef` and asserting it's stripped.

---

## 5. Coverage targets

| Layer | Target |
|---|---|
| Service layer | ≥ 80% line |
| RAG core (chunk/validate/parse) | ≥ 85% line |
| Overall | ≥ 75% line |

Coverage is a floor, not the goal — a green suite with meaningful assertions matters more than a number. Enforce with JaCoCo in the Maven build; fail build under threshold.

---

## 6. Test data

- `src/test/resources/` holds a sample resume + JD (text) and canned LLM JSON verdicts.
- Builders/object-mothers for entities to keep tests readable.

---

## 7. Running

```bash
./mvnw test                    # unit + integration (Testcontainers needs Docker)
./mvnw verify                  # + JaCoCo coverage gate
./mvnw test -Dtest=AnalysisServiceTest   # single class
```

Frontend: `npm run test` (Vitest + React Testing Library) for the API layer and key components.

---

## 8. Definition of Done (testing slice)
- [ ] New logic has unit tests with real assertions (not just "no exception").
- [ ] Error/edge paths covered (invalid input, not owner, LLM malformed).
- [ ] Integration test added if an endpoint or query changed.
- [ ] `./mvnw verify` green locally before pushing.
- [ ] CI passes on the PR.
