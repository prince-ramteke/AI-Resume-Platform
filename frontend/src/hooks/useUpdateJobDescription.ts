import { useMutation, useQueryClient } from "@tanstack/react-query";
import { updateJobDescription } from "../api/jobDescriptions";
import type { JobDescriptionDetail } from "../types/jobDescription";

/**
 * Update a JD's title and rawText. On success writes the fresh detail into the
 * cache immediately (no extra roundtrip) and invalidates the list/recent/count
 * queries so metadata that depends on the update reflects it.
 */
export function useUpdateJobDescription(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { title: string; rawText: string }) =>
      updateJobDescription(id, input),
    onSuccess: (updated: JobDescriptionDetail) => {
      queryClient.setQueryData(["jobDescriptions", "detail", id], updated);
      queryClient.invalidateQueries({ queryKey: ["jobDescriptions"] });
    },
  });
}
