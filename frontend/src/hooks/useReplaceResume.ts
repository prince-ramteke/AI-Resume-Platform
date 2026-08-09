import { useMutation, useQueryClient } from "@tanstack/react-query";
import { replaceResume } from "../api/resumes";

/**
 * Replace the file behind an existing resume. On success invalidates every
 * `resumes` query so the detail (re-extracted text, new size/date) and list
 * both refetch. The previous resume stays intact until the server confirms.
 */
export function useReplaceResume(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => replaceResume(id, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["resumes"] });
    },
  });
}
