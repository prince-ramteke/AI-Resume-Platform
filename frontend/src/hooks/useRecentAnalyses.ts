import { useQuery } from "@tanstack/react-query";
import { listAnalyses } from "../api/analyses";

/** Most recent analyses + total count. One request serves both. */
export function useRecentAnalyses() {
  return useQuery({
    queryKey: ["analyses", "recent"],
    queryFn: () => listAnalyses({ size: 5, sort: "createdAt,desc" }),
  });
}
