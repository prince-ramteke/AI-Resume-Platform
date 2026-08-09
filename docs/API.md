# API Contract
## AI Resume Intelligence Platform

> REST API reference. Every endpoint is documented in Swagger at runtime (`/swagger-ui.html`); this file is the design source of truth.

---

## 1. Conventions

- Base path: `/api`
- Format: JSON. Auth: `Authorization: Bearer <JWT>` on all routes except the public ones below.
- IDs are numeric (`Long`). Timestamps are ISO-8601 UTC.
- **Public (no auth):** `POST /api/auth/register`, `POST /api/auth/login`, `GET /actuator/health`, Swagger.
- Pagination: `?page=0&size=20&sort=createdAt,desc` → Spring `Page` response.

### Standard error envelope
```json
{
  "timestamp": "2026-08-05T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "file must be PDF or DOCX",
  "traceId": "a1b2c3d4"
}
```

| Status | When |
|---|---|
| 400 | Validation / bad input |
| 401 | Missing/invalid/expired JWT |
| 403 | Authenticated but not allowed (RBAC — admin routes only) |
| 404 | Resource not found |
| 409 | Conflict (e.g., email already registered) |
| 413 | File too large |
| 422 | LLM produced unusable output after repair retry |
| 429 | Per-user rate limit exceeded (`POST /api/analyses` only) — `Retry-After` header included |
| 500 | Unexpected — generic message, details logged only |

---

## 2. Auth

### POST /api/auth/register
```json
// request
{ "email": "prince@example.com", "password": "StrongPass!23" }
// 201
{ "id": 1, "email": "prince@example.com", "role": "USER" }
// 409 if email exists
```

### POST /api/auth/login
```json
// request
{ "email": "prince@example.com", "password": "StrongPass!23" }
// 200
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer", "expiresAt": "2026-08-05T11:15:30Z" }
// 401 on bad credentials
```

### GET /api/auth/me
Returns the current user. `200 { id, email, role }`.

---

## 3. Resumes

All resume endpoints require authentication. Ownership is enforced — non-owners receive `404` (not `403`) to prevent enumeration. Admins (`ROLE_ADMIN`) can access any resume.

### POST /api/resumes
`multipart/form-data`, field `file` (PDF/DOCX, ≤10 MB). Validates content-type, file extension, magic bytes, and size. Extracts text via Apache Tika and stores the file locally.
```json
// 201 UploadResumeResponse
{ "id": 12, "filename": "prince_resume.pdf", "contentType": "application/pdf",
  "fileSize": 52340, "createdAt": "2026-08-07T10:00:00Z" }
// 400 wrong type / empty / corrupted / password-protected
// 413 too large (>10 MB)
```

### GET /api/resumes
Paginated list of the caller's resumes (soft-deleted excluded). `200 Page<ResumeSummaryResponse>` where
`ResumeSummaryResponse = { id, filename, contentType, fileSize, createdAt }`.
Default sort: `createdAt`, default page size: `20`.

### GET /api/resumes/{id}
Full resume detail including extracted text. `200 ResumeResponse`:
```json
{ "id": 12, "filename": "prince_resume.pdf", "contentType": "application/pdf",
  "fileSize": 52340, "rawText": "Prince Ramteke\nSoftware Engineer...",
  "pageCount": 2, "language": "en",
  "createdAt": "2026-08-07T10:00:00Z", "updatedAt": null }
```
`404` if missing, soft-deleted, or not owner.

### PUT /api/resumes/{id}
`multipart/form-data`, field `file` (PDF/DOCX, ≤10 MB). Replaces the resume file, re-extracts text, deletes old file from storage.
```json
// 200 UploadResumeResponse
{ "id": 12, "filename": "updated_resume.pdf", "contentType": "application/pdf",
  "fileSize": 61024, "createdAt": "2026-08-07T10:00:00Z" }
// 400 / 413 same as POST · 404 if missing or not owner
```

### DELETE /api/resumes/{id}
Soft-delete. `204 No Content`. The resume is excluded from queries but remains in the database.
`404` if missing or not owner.

### GET /api/resumes/{id}/download
Downloads the original uploaded file. Returns the binary content with:
- `Content-Type`: the original file's MIME type
- `Content-Disposition: attachment; filename="<original_filename>"`
`404` if missing or not owner.

---

## 4. Job Descriptions

All job-description endpoints require authentication. Ownership is enforced — non-owners receive `404` (not `403`) to prevent enumeration. Admins (`ROLE_ADMIN`) can access any job description.

Two creation modes: **text-paste** (JSON body) and **file upload** (multipart, PDF/DOCX/TXT ≤10 MB).

### POST /api/job-descriptions
Create from pasted text (JSON body).
```json
// request
{ "title": "Java Backend Engineer", "rawText": "We are looking for..." }
// 201 JobDescriptionResponse
{ "id": 7, "title": "Java Backend Engineer", "rawText": "We are looking for...",
  "contentType": null, "fileSize": null, "pageCount": null, "language": null,
  "createdAt": "2026-08-07T10:00:00Z", "updatedAt": null }
// 400 blank title or rawText
```

### POST /api/job-descriptions/upload
Create from uploaded file (`multipart/form-data`). Fields: `file` (PDF/DOCX/TXT, ≤10 MB), `title` (string, ≤255). Validates extension, content-type, magic bytes (skipped for TXT), and size. TXT files are read directly as UTF-8; PDF/DOCX are extracted via Apache Tika.
```json
// 201 JobDescriptionResponse
{ "id": 8, "title": "Backend Role", "rawText": "extracted text...",
  "contentType": "application/pdf", "fileSize": 52340, "pageCount": 2, "language": "en",
  "createdAt": "2026-08-07T10:00:00Z", "updatedAt": null }
// 400 wrong type / empty / corrupted
// 413 too large (>10 MB)
```

### GET /api/job-descriptions
Paginated list of the caller's job descriptions (soft-deleted excluded). Optional `?search=` query filters by title (case-insensitive contains). `200 Page<JobDescriptionSummaryResponse>`:
`JobDescriptionSummaryResponse = { id, title, contentType, fileSize, createdAt }`.
Default sort: `createdAt`, default page size: `20`.

### GET /api/job-descriptions/{id}
Full detail including raw text. `200 JobDescriptionResponse`:
```json
{ "id": 7, "title": "Java Backend Engineer", "rawText": "We are looking for...",
  "contentType": null, "fileSize": null, "pageCount": null, "language": null,
  "createdAt": "2026-08-07T10:00:00Z", "updatedAt": null }
```
`404` if missing, soft-deleted, or not owner.

### PUT /api/job-descriptions/{id}
Update title and raw text (JSON body).
```json
// request
{ "title": "Updated Title", "rawText": "Updated description..." }
// 200 JobDescriptionResponse
{ "id": 7, "title": "Updated Title", "rawText": "Updated description...",
  "contentType": null, "fileSize": null, "pageCount": null, "language": null,
  "createdAt": "2026-08-07T10:00:00Z", "updatedAt": "2026-08-07T11:00:00Z" }
// 400 blank title or rawText · 404 if missing or not owner
```

### DELETE /api/job-descriptions/{id}
Soft-delete. `204 No Content`. `404` if missing or not owner.

### GET /api/job-descriptions/{id}/download
Downloads the original uploaded file (file-based JDs only). Returns binary content with:
- `Content-Type`: the original file's MIME type
- `Content-Disposition: attachment; filename="<title>"`
`404` if missing, not owner, or the JD was created via text-paste (no file to download).

---

## 5. Analysis (core)

All analysis endpoints require authentication. Ownership is enforced in the service — a resume, JD, or analysis the caller does not own returns `404` (not `403`) to prevent enumeration. Admins (`ROLE_ADMIN`) can read any analysis via `GET /api/analyses/{id}`.

### POST /api/analyses
Runs the RAG + scoring pipeline for a resume against a job description (both must be owned by the caller). Returns `201` with a `Location: /api/analyses/{id}` header.

**Result cache (v1.1):** if a prior successful analysis exists for the same `(userId, resumeId, jobDescriptionId)` and was recorded after the most recent modification of both underlying documents, the endpoint returns that cached `AnalysisResponse` without re-running the pipeline. The response shape is identical to a fresh run; the `Location` header points at the pre-existing analysis id. Editing/replacing either the resume or the JD invalidates the cache naturally via each entity's `updatedAt`.

**Rate limit (v1.1):** per-user token bucket — capacity 5, refilling 5 tokens every 15 minutes (in-memory, Bucket4j). Only this endpoint is limited; `GET /api/analyses` and `GET /api/analyses/{id}` are unaffected. Exceeding the limit returns `429` with a `Retry-After: <seconds>` header and the standard error envelope:
```json
// 429
{
  "timestamp": "2026-08-09T10:20:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "You've hit the analysis rate limit. Try again in 42s.",
  "traceId": "unknown"
}
```
```json
// request
{ "resumeId": 12, "jobDescriptionId": 7 }
```
```json
// 201 AnalysisResponse
{
  "id": 55,
  "score": 78,
  "summary": "Strong backend match; missing cloud + observability depth.",
  "matchedSkills": [
    { "skill": "Spring Boot", "importance": "HIGH", "evidenceRef": "RESUME#2" },
    { "skill": "Kafka", "importance": "MEDIUM", "evidenceRef": "RESUME#5" }
  ],
  "missingSkills": [
    { "skill": "AWS", "importance": "HIGH", "evidenceRef": "JD#3" },
    { "skill": "Kubernetes", "importance": "MEDIUM", "evidenceRef": "JD#4" }
  ],
  "weakSkills": [
    { "skill": "Testing", "importance": "MEDIUM", "evidenceRef": "RESUME#6" }
  ],
  "recommendations": [
    { "text": "Add a bullet showing cloud deployment (even Docker→Render).",
      "impact": "HIGH", "reason": "JD lists AWS as required." },
    { "text": "Quantify test coverage on your API work.",
      "impact": "MEDIUM", "reason": "JD stresses reliability." }
  ],
  "evidence": [
    { "ref": "RESUME#2", "sourceType": "RESUME", "chunkIndex": 2, "snippet": "Built 10+ secured REST endpoints..." },
    { "ref": "JD#3", "sourceType": "JD", "chunkIndex": 3, "snippet": "Experience with AWS required..." }
  ],
  "provider": "ollama",
  "latencyMs": 4120,
  "createdAt": "2026-08-05T10:20:00Z"
}
// 201 with Location: /api/analyses/{id}
// 400 invalid body (resumeId/jobDescriptionId null or non-positive)
// 401 unauthenticated
// 404 if caller doesn't own resume or JD
// 422 if the LLM output is unusable after one repair retry
// 429 rate limit exceeded (5 requests / 15 min per user) — Retry-After header included
```
Every `evidenceRef` in `matchedSkills`/`missingSkills`/`weakSkills` resolves to an entry in `evidence`; unsupported claims are dropped during validation, so a returned claim is always grounded.

### GET /api/analyses
Paginated history of the caller's analyses. `200 Page<AnalysisSummaryResponse>` where
`AnalysisSummaryResponse = { id, score, jobTitle, createdAt }`. Default sort: `createdAt`, default page size: `20`. `401` if unauthenticated.

### GET /api/analyses/{id}
Full `AnalysisResponse`. `404` if missing or not owner; an admin (`ROLE_ADMIN`) may read any analysis. `401` if unauthenticated.

---

## 6. Admin / ops

### GET /api/admin/metrics  (ROLE_ADMIN)
Aggregate counts, avg latency, provider usage. `403` for non-admins.

### GET /actuator/health
Public liveness/readiness (DB + Ollama reachability).

---

## 7. Request DTO validation rules

| Field | Rule |
|---|---|
| email | `@Email`, not blank |
| password | ≥8 chars, at least 1 letter + 1 digit |
| resumeId / jobDescriptionId | not null, positive |
| JD title | not blank, ≤255 |
| JD rawText | not blank when no file, ≤50k chars |
| uploaded file (resume) | PDF/DOCX, ≤10 MB (validated before parsing) |
| uploaded file (JD)     | PDF/DOCX/TXT, ≤10 MB (validated before parsing) |

All violations → `400` via the global handler with field-level messages.

---

## 8. Versioning

Prefix reserved for future breaking changes: `/api/v1/...` may be adopted at v2. v1 uses unversioned `/api` for simplicity; document any change here and in Swagger.
