import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createJobDescriptionFromFile } from "../api/jobDescriptions";

/**
 * Create a JD from an uploaded PDF/DOCX/TXT (multipart). On success invalidates
 * every `jobDescriptions` query. Navigation to the new detail is left to the
 * caller.
 */
export function useCreateJobDescriptionFromFile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { title: string; file: File }) =>
      createJobDescriptionFromFile(input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jobDescriptions"] });
    },
  });
}
