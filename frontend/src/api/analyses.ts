import apiClient from "./client";
import type { Page } from "../types";
import type { AnalysisDetail, AnalysisSummary } from "../types/analysis";

/**
 * GET /api/analyses — the caller's analysis history, paginated. The dashboard
 * reads both the recent rows (`content`) and the count (`totalElements`).
 */
export async function listAnalyses(params: {
  page?: number;
  size?: number;
  sort?: string;
}): Promise<Page<AnalysisSummary>> {
  const { data } = await apiClient.get<Page<AnalysisSummary>>("/analyses", {
    params,
  });
  return data;
}

/** GET /api/analyses/{id} — full analysis result. 404 if missing or not owned. */
export async function getAnalysis(id: number): Promise<AnalysisDetail> {
  const { data } = await apiClient.get<AnalysisDetail>(`/analyses/${id}`);
  return data;
}

/** How long we'll wait, per request, for the synchronous run to finish. */
const ANALYSIS_RUN_TIMEOUT_MS = 240_000;

/**
 * POST /api/analyses — synchronous run of the RAG+LLM pipeline against a
 * resume + job description the caller owns. Backend returns the full
 * `AnalysisResponse` inline (201, plus a `Location` header we don't need to
 * follow — the body is the result).
 *
 * Locally with Ollama + llama3.1:8b this takes ~2 minutes end-to-end, so the
 * per-request timeout is raised well above the shared client's default; we
 * never mutate the global default because other calls are fast. The optional
 * AbortSignal lets the UI's "Stop waiting" button cut the client-side wait
 * (the backend request keeps running server-side; documented in the hook).
 */
export async function runAnalysis(
  input: { resumeId: number; jobDescriptionId: number },
  opts: { signal?: AbortSignal } = {}
): Promise<AnalysisDetail> {
  const { data } = await apiClient.post<AnalysisDetail>("/analyses", input, {
    timeout: ANALYSIS_RUN_TIMEOUT_MS,
    signal: opts.signal,
  });
  return data;
}
