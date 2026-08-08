/** Compact analysis row from `GET /api/analyses` (AnalysisSummaryResponse). */
export interface AnalysisSummary {
  id: number;
  /** Match score, 0–100. */
  score: number;
  /** Denormalized job-description title for display. */
  jobTitle: string;
  /** ISO-8601 UTC timestamp. */
  createdAt: string;
}
