/** Compact job-description row from `GET /api/job-descriptions` (JobDescriptionSummaryResponse). */
export interface JobDescriptionSummary {
  id: number;
  title: string;
  /** Null for text-paste JDs (no uploaded file). */
  contentType: string | null;
  /** Null for text-paste JDs. */
  fileSize: number | null;
  /** ISO-8601 UTC timestamp. */
  createdAt: string;
}

/**
 * Full JD detail from `GET /api/job-descriptions/{id}` and returned by create /
 * update endpoints (JobDescriptionResponse). Text-paste JDs leave `contentType`,
 * `fileSize`, `pageCount`, and `language` null; `updatedAt` is null until the
 * first edit.
 */
export interface JobDescriptionDetail {
  id: number;
  title: string;
  rawText: string;
  contentType: string | null;
  fileSize: number | null;
  pageCount: number | null;
  language: string | null;
  /** ISO-8601 UTC timestamp. */
  createdAt: string;
  /** ISO-8601 UTC timestamp; null until the JD has been edited. */
  updatedAt: string | null;
}
