/** HIGH|MEDIUM|LOW importance/impact tag from the LLM verdict. */
export type SkillImportance = "HIGH" | "MEDIUM" | "LOW";

/** RESUME|JD tag identifying which document an evidence chunk came from. */
export type EvidenceSourceType = "RESUME" | "JD";

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

/**
 * One skill claim from the LLM verdict. `evidenceRef` is a citation tag such as
 * `RESUME#2` / `JD#3` that is guaranteed by the backend to resolve to an entry
 * in the enclosing analysis's `evidence` array (unsupported claims are dropped
 * server-side).
 */
export interface AnalysisSkill {
  skill: string;
  importance: SkillImportance;
  evidenceRef: string;
}

/** Prioritized recommendation from the LLM verdict. */
export interface AnalysisRecommendation {
  text: string;
  impact: SkillImportance;
  reason: string;
}

/** A cited chunk. `ref` is what skill claims point at. */
export interface AnalysisEvidence {
  ref: string;
  sourceType: EvidenceSourceType;
  chunkIndex: number;
  snippet: string;
}

/**
 * Full analysis result — POST /api/analyses response body and GET /api/analyses/{id}.
 * Shape mirrors AnalysisResponse.java exactly; do not add fields the backend
 * doesn't return.
 */
export interface AnalysisDetail {
  id: number;
  /** Match score, 0–100 (integer). */
  score: number;
  /** One-sentence human summary of the verdict. */
  summary: string;
  matchedSkills: AnalysisSkill[];
  missingSkills: AnalysisSkill[];
  weakSkills: AnalysisSkill[];
  recommendations: AnalysisRecommendation[];
  /** Every `evidenceRef` in matched/missing/weak resolves to one of these. */
  evidence: AnalysisEvidence[];
  /** Which LLM produced the verdict ("ollama" | "openai"). */
  provider: string;
  /** LLM call latency in ms (not the full pipeline). */
  latencyMs: number;
  /** ISO-8601 UTC timestamp. */
  createdAt: string;
}
