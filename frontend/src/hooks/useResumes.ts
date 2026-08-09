import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { listResumes } from "../api/resumes";

export interface ResumesQueryParams {
  /** Zero-based page index (Spring `Page`). */
  page: number;
  size: number;
  sort?: string;
}

/**
 * Paginated resume list. `keepPreviousData` holds the current page on screen
 * while the next one loads, so paging never flashes a skeleton or empty state
 * (only the very first load is `isPending`).
 */
export function useResumes(params: ResumesQueryParams) {
  return useQuery({
    queryKey: ["resumes", "list", params],
    queryFn: () => listResumes(params),
    placeholderData: keepPreviousData,
  });
}
