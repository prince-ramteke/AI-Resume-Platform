import apiClient from "./client";
import type { Page } from "../types";
import type { AnalysisSummary } from "../types/analysis";

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
