import { useQuery } from "@tanstack/react-query";
import { getAnalysis } from "../api/analyses";

/**
 * Single analysis result. Disabled when `id` is not a valid number (malformed
 * route param) so we never fire a request for `/analyses/NaN`.
 */
export function useAnalysis(id: number) {
  return useQuery({
    queryKey: ["analyses", "detail", id],
    queryFn: () => getAnalysis(id),
    enabled: Number.isInteger(id) && id > 0,
  });
}
