import { useQuery } from "@tanstack/react-query";
import { getResume } from "../api/resumes";

/**
 * Single resume detail. Disabled when `id` is not a valid number (e.g. a
 * malformed route param) so we never fire a request for `/resumes/NaN`.
 */
export function useResume(id: number) {
  return useQuery({
    queryKey: ["resumes", "detail", id],
    queryFn: () => getResume(id),
    enabled: Number.isInteger(id) && id > 0,
  });
}
