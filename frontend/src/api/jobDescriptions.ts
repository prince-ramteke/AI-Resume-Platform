import apiClient from "./client";
import type { Page } from "../types";
import type { JobDescriptionSummary } from "../types/jobDescription";

/**
 * GET /api/job-descriptions — the caller's job descriptions, paginated. The
 * dashboard reads `totalElements` for the count.
 */
export async function listJobDescriptions(params: {
  page?: number;
  size?: number;
  sort?: string;
}): Promise<Page<JobDescriptionSummary>> {
  const { data } = await apiClient.get<Page<JobDescriptionSummary>>(
    "/job-descriptions",
    { params }
  );
  return data;
}
