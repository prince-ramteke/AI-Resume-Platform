import { useQuery } from "@tanstack/react-query";
import { getJobDescription } from "../api/jobDescriptions";

/**
 * Single JD detail. Disabled when `id` is not a valid number (e.g. a
 * malformed route param) so we never fire a request for `/job-descriptions/NaN`.
 */
export function useJobDescription(id: number) {
  return useQuery({
    queryKey: ["jobDescriptions", "detail", id],
    queryFn: () => getJobDescription(id),
    enabled: Number.isInteger(id) && id > 0,
  });
}
