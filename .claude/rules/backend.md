# Rule: Backend

Always-on constraints for Spring Boot code. Non-negotiable.

## Always
- Use Java 21 + Spring Boot 3.x. Constructor injection with `final` fields.
- Structure code package-by-feature under `com.princeramteke.resumeai.<feature>`.
- Keep the flow Controller → Service → Repository. Business logic lives in services.
- Return DTOs (records) from controllers; map entities → DTOs in a mapper.
- Validate every request DTO with Bean Validation (`@Valid`).
- Route all errors through the global `@RestControllerAdvice` handler.
- Put `@Transactional` on writes; `readOnly = true` on reads.
- Access LLM/embeddings only via `LlmClient` / `EmbeddingClient`.

## Never
- Never inject with field `@Autowired`.
- Never return or accept JPA entities at the API boundary.
- Never put business logic or repository calls in a controller.
- Never call Ollama/OpenAI SDKs directly from a feature package.
- Never hold a DB transaction open across a slow LLM call.
- Never swallow exceptions silently or leak stack traces to clients.

## When adding a new feature package
Mirror an existing one (e.g., `resume/`): controller, service, repository, dto/, mapper, exceptions. Add tests in the parallel `src/test` path. Update Swagger and `docs/API.md`.

## Work that belongs here
Spring Boot controllers, services, repositories, DTOs, mappers, bean validation, exception handling, transactions, and the LLM/embedding abstraction wiring.

## Skills for this area
- **Auto-consult:** `engineering:system-design` (service boundaries, module design). Also read `rules/api`, `rules/security`, `rules/database`, `rules/testing` — backend work almost always touches them.
- **Verify before done:** `engineering:code-review`, `superpowers:verification-before-completion`.
- **Ignore:** frontend/design skills, doc-format skills (docx/pdf/…), and everything in `SKILL_ROUTING_MAP.md` §5. Don't pull in `engineering:architecture` unless you're making a genuine tech-choice decision (ADR-worthy).
