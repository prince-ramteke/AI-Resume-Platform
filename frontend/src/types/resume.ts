/** Compact resume row from `GET /api/resumes` (ResumeSummaryResponse). */
export interface ResumeSummary {
  id: number;
  filename: string;
  contentType: string;
  fileSize: number;
  /** ISO-8601 UTC timestamp. */
  createdAt: string;
}
