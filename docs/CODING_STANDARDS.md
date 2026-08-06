# Coding Standards
## AI Resume Intelligence Platform

> Conventions the codebase (and the AI assistant) must follow. Consistency > personal preference.

---

## 1. Java general
- Target **Java 21**. Use records for DTOs and immutable value objects; `var` for obvious local types only.
- **Constructor injection only.** No field `@Autowired`. Final fields.
- Prefer immutability: `final` fields, unmodifiable collections at boundaries.
- Fail fast: validate inputs at the edge; throw domain exceptions, don't return nulls for errors.
- `Optional` at repository return boundaries, not as method parameters or fields.
- No business logic in controllers; no persistence in controllers.

## 2. Package & naming
- **Package-by-feature**: `com.princeramteke.resumeai.<feature>` owning controller/service/repository/dto.
- Classes: `XxxController`, `XxxService`, `XxxRepository`, `XxxRequest`, `XxxResponse`, `XxxMapper`, `XxxException`.
- Methods: verbs (`analyze`, `extractText`). Booleans: `isX`/`hasX`. Constants: `UPPER_SNAKE`.
- One top-level class per file.

## 3. Layering rules
```
Controller  → validates request DTO, delegates, returns response DTO
Service     → business logic, transactions (@Transactional), ownership checks
Repository  → data access only (Spring Data interfaces / native vector queries)
Mapper      → entity ↔ DTO conversion
```
- Entities never leave the service layer. Controllers speak DTOs only.

## 4. DTOs
```java
public record AnalyzeRequest(@NotNull @Positive Long resumeId,
                             @NotNull @Positive Long jobDescriptionId) {}
```
- Request DTOs carry Bean Validation annotations.
- Response DTOs are records; never expose internal fields (password hash, raw embeddings).

## 5. Exceptions & errors
- Throw specific domain exceptions (`ResumeNotFoundException extends RuntimeException`).
- Map to HTTP centrally in `@RestControllerAdvice` → the standard error envelope (see `API.md`).
- Never `catch (Exception e) {}` silently. Log with context, rethrow or handle deliberately.
- Never leak stack traces or internal messages to clients.

## 6. Logging
- SLF4J. Levels: ERROR (action needed), WARN (recoverable), INFO (lifecycle/business events), DEBUG (dev detail).
- Include a trace id per request (MDC). Parameterized logging: `log.info("analysis {} for user {}", id, userId)`.
- **Never log** secrets, JWTs, passwords, or full document text.

## 7. Transactions
- `@Transactional` on service methods that write. Read-only queries: `@Transactional(readOnly = true)`.
- Keep external LLM/embedding calls **outside** DB transactions where possible (don't hold a connection during a slow model call).

## 8. AI-layer specifics
- All model access via `LlmClient`/`EmbeddingClient`. No feature imports Ollama/OpenAI SDK directly.
- Prompts live in a dedicated, reviewable place (constants/templates), not inline scattered strings.
- Always validate model output before use. Never trust free text.

## 9. Testing conventions
- Test class: `XxxTest` (unit), `XxxIT` (integration). Method names: `methodName_condition_expectedResult`.
- Arrange–Act–Assert. One logical assertion focus per test. Mock external deps.

## 10. Frontend (React/TS)
- Functional components + hooks. No class components.
- API calls only in `src/api/` (typed). Components consume hooks, not Axios directly.
- Types in `src/types/`; no `any`. Props typed explicitly.
- Keep components small; lift shared logic into hooks.

## 11. Git & commits
- Small, focused commits. Conventional style: `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`.
- One feature/bugfix per PR. PR description states what + how to verify.
- Never commit `.env`, secrets, or generated artifacts.

## 12. Formatting
- Backend: Google Java Format or Spring's formatter; enforce via Spotless in the build.
- Frontend: Prettier + ESLint. CI fails on lint errors.
