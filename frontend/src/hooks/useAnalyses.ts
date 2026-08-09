import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { listAnalyses } from "../api/analyses";

export interface AnalysesQueryParams {
  /** Zero-based page index (Spring `Page`). */
  page: number;
  size: number;
  sort?: string;
}

/**
 * Paginated analysis history. `keepPreviousData` holds the current page while
 * the next one loads, matching the resume/JD list ergonomics.
 */
export function useAnalyses(params: AnalysesQueryParams) {
  return useQuery({
    queryKey: ["analyses", "list", params],
    queryFn: () => listAnalyses(params),
    placeholderData: keepPreviousData,
  });
}
