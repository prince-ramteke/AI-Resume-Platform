/** Compact job-description row from `GET /api/job-descriptions` (JobDescriptionSummaryResponse). */
export interface JobDescriptionSummary {
  id: number;
  title: string;
  contentType: string;
  /** Null for text-paste JDs (no uploaded file). */
  fileSize: number | null;
  /** ISO-8601 UTC timestamp. */
  createdAt: string;
}
