import apiClient from "./client";
import type { Page } from "../types";
import type { ResumeSummary } from "../types/resume";

/**
 * GET /api/resumes — the caller's resumes, paginated. Spring `Page` carries
 * `totalElements`, which the dashboard uses for the resume count.
 */
export async function listResumes(params: {
  page?: number;
  size?: number;
  sort?: string;
}): Promise<Page<ResumeSummary>> {
  const { data } = await apiClient.get<Page<ResumeSummary>>("/resumes", {
    params,
  });
  return data;
}
