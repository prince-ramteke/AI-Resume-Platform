import { useQuery } from "@tanstack/react-query";
import { listResumes } from "../api/resumes";

/** Most recent resumes + total count. One request serves both. */
export function useRecentResumes() {
  return useQuery({
    queryKey: ["resumes", "recent"],
    queryFn: () => listResumes({ size: 5, sort: "createdAt,desc" }),
  });
}
