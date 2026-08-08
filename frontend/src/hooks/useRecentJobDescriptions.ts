import { useQuery } from "@tanstack/react-query";
import { listJobDescriptions } from "../api/jobDescriptions";

/** Most recent job descriptions + total count. One request serves both. */
export function useRecentJobDescriptions() {
  return useQuery({
    queryKey: ["jobDescriptions", "recent"],
    queryFn: () => listJobDescriptions({ size: 5, sort: "createdAt,desc" }),
  });
}
