import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { listJobDescriptions } from "../api/jobDescriptions";

export interface JobDescriptionsQueryParams {
  /** Zero-based page index (Spring `Page`). */
  page: number;
  size: number;
  sort?: string;
  /** Optional case-insensitive title contains-search; omit when blank. */
  search?: string;
}

/**
 * Paginated JD list. `keepPreviousData` holds the current page on screen while
 * the next one (or a re-typed search) loads, so paging never flashes a
 * skeleton or empty state — only the very first load is `isPending`.
 */
export function useJobDescriptions(params: JobDescriptionsQueryParams) {
  return useQuery({
    queryKey: ["jobDescriptions", "list", params],
    queryFn: () => listJobDescriptions(params),
    placeholderData: keepPreviousData,
  });
}
