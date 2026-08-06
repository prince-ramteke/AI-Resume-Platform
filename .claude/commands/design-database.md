# Command: /design-database

**Purpose:** Design or evolve schema, entities, relations, and indexes.
**Input:** The data need (new entity, relation, index, or vector change).
**Output:** A Flyway migration + JPA entity plan + updated `docs/DATABASE.md`.
**Best skills:** `engineering:system-design` (data modeling); `engineering:architecture` only for a real storage-choice ADR.

## Steps
1. Read `.claude/rules/database.md`, `.claude/rules/backend.md`, and `docs/DATABASE.md`.
2. Define the table(s)/columns, FKs (`ON DELETE CASCADE`), indexes (incl. pgvector HNSW), and JSONB shapes.
3. Write a new Flyway migration (never edit an applied one); align the JPA entity.
4. Keep the vector dimension centralized. Update `docs/DATABASE.md`.

Paginate all list queries. Cache embeddings — never re-embed existing chunks.
