# RAG Evaluation Fixtures (v1.2.M1)

Small synthetic labeled cases for the retrieval evaluation harness
(`RetrievalEvaluationHarnessIT`). **Not** real resumes and **not** production data.

## Rules for adding a case

- Synthetic only. No real names, employers, or personally identifiable information.
- Keep each case small (target 4–8 chunks) and human-readable in a single screen.
- The `notes` field is mandatory — briefly justify why each relevant chunk was labeled relevant.
- Chunk indexes must be unique within a case and start at 0.
- `relevantChunkIndexes` is a set (unordered).

## What the harness measures

For each case, the harness ingests the fixture into a per-run Testcontainers Postgres,
then compares vector-only vs. hybrid retrieval on the same case at `k=3, k=5, k=8`
using Recall@K and MRR. Numbers are for **relative comparison only**, not a
production go/no-go signal.

## Fixture format

```json
{
  "caseId": "unique-slug",
  "sourceType": "RESUME",
  "chunks": [
    { "index": 0, "content": "..." }
  ],
  "query": "job description text used as the retrieval query",
  "relevantChunkIndexes": [0, 3],
  "notes": "Why the labels are justified; distractors called out."
}
```
