/** Compact resume row from `GET /api/resumes` (ResumeSummaryResponse). */
export interface ResumeSummary {
  id: number;
  filename: string;
  contentType: string;
  fileSize: number;
  /** ISO-8601 UTC timestamp. */
  createdAt: string;
}

/**
 * Full resume detail from `GET /api/resumes/{id}` (ResumeResponse). Includes the
 * server-extracted text and document metadata. `pageCount`/`language` may be null
 * when extraction couldn't determine them; `updatedAt` is null until first replace.
 */
export interface ResumeDetail {
  id: number;
  filename: string;
  contentType: string;
  fileSize: number;
  rawText: string;
  pageCount: number | null;
  language: string | null;
  /** ISO-8601 UTC timestamp. */
  createdAt: string;
  /** ISO-8601 UTC timestamp; null until the file has been replaced. */
  updatedAt: string | null;
}

/**
 * Result of upload/replace (`POST /api/resumes`, `PUT /api/resumes/{id}` →
 * UploadResumeResponse). Carries the new id so callers can navigate to detail.
 */
export interface ResumeUploadResult {
  id: number;
  filename: string;
  contentType: string;
  fileSize: number;
  /** ISO-8601 UTC timestamp. */
  createdAt: string;
}
