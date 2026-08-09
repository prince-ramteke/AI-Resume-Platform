import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createJobDescriptionFromText } from "../api/jobDescriptions";

/**
 * Create a JD from pasted text. On success invalidates every `jobDescriptions`
 * query (list, dashboard count, recent feed) so they refetch. Navigation to
 * the new detail page is the caller's responsibility (it has the returned id).
 */
export function useCreateJobDescriptionFromText() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { title: string; rawText: string }) =>
      createJobDescriptionFromText(input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jobDescriptions"] });
    },
  });
}
