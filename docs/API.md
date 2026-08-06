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

### POST /api/resumes
`multipart/form-data`, field `file` (PDF/DOCX, ≤10 MB).
```json
// 201
{ "id": 12, "filename": "prince_resume.pdf", "createdAt": "..." }
// 400 wrong type · 413 too large
```

### GET /api/resumes
Paginated list of the caller's resumes. `200 Page<ResumeSummary>` where
`ResumeSummary = { id, filename, createdAt }`.

### GET /api/resumes/{id}
`200 { id, filename, rawText, createdAt }` · `404` if missing or not owner.

### DELETE /api/resumes/{id}
`204` · cascades to its chunks.

---

## 4. Job descriptions

### POST /api/job-descriptions
```json
// request (paste)
{ "title": "Java Backend Engineer", "rawText": "We are looking for..." }
// or multipart file upload (same as resumes)
// 201
{ "id": 7, "title": "Java Backend Engineer", "createdAt": "..." }
```

### GET /api/job-descriptions
Paginated list. `200 Page<{ id, title, createdAt }>`.

### GET /api/job-descriptions/{id} · DELETE /api/job-descriptions/{id}
As per resumes.

---

## 5. Analysis (core)

### POST /api/analyses
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
// 404 if caller doesn't own resume or JD · 422 if LLM output unusable
```

### GET /api/analyses
Paginated history. `200 Page<{ id, score, jobTitle, createdAt }>`.

### GET /api/analyses/{id}
Full `AnalysisResponse`. `404` if missing or not owner.

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
| uploaded file | PDF/DOCX, ≤10 MB (validated before parsing) |

All violations → `400` via the global handler with field-level messages.

---

## 8. Versioning

Prefix reserved for future breaking changes: `/api/v1/...` may be adopted at v2. v1 uses unversioned `/api` for simplicity; document any change here and in Swagger.
