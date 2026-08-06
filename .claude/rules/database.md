# Rule: Database

Always-on constraints for persistence. See `docs/DATABASE.md` for full DDL.

## Always
- Change schema only via a new Flyway migration in `db/migration/`. Never edit an applied migration.
- Keep JPA entities in sync with the DDL and the docs.
- Use `TIMESTAMPTZ` → `Instant`/`OffsetDateTime`.
- Enforce ownership FKs with `ON DELETE CASCADE` at DB level and cascade/orphanRemoval in JPA.
- Paginate all list queries; cap top-k on vector search (k ≤ 8).
- Store the embedding as `vector(768)` (match the embed model dimension via one config constant).
- Build/keep the HNSW index on `document_chunks.embedding`.

## Never
- Never use `findAll()` without pagination.
- Never build SQL with string concatenation — parameterized/JPA only.
- Never re-embed a document whose chunks already exist (check first; cache).
- Never hardcode the vector dimension in multiple places.

## Vector queries
Filter by `source_type` alongside the ANN search. Use cosine distance (`vector_cosine_ops`). Prefer Spring AI's `VectorStore` over hand-written SQL where practical.

## Work that belongs here
Schema/DDL, JPA entities and relations, Flyway migrations, indexes (incl. pgvector HNSW), query design, and vector-store setup.

## Skills for this area
- **Auto-consult:** `engineering:system-design` (data modeling, relations). Read `rules/backend` alongside — entities and repositories move together.
- **Task-specific:** `engineering:architecture` only when choosing a storage approach worth an ADR (e.g., pgvector vs. a dedicated vector DB).
- **Verify before done:** `engineering:code-review` (check for N+1, unbounded fetches, missing indexes).
- **Ignore:** frontend/design, deployment, and doc-format skills.
