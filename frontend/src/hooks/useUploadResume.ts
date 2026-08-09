import { useMutation, useQueryClient } from "@tanstack/react-query";
import { uploadResume } from "../api/resumes";

/**
 * Upload a new resume. On success invalidates every `resumes` query (list,
 * recent dashboard feed, counts) so they refetch. Navigation to the new detail
 * page is left to the caller, which has the returned id.
 */
export function useUploadResume() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => uploadResume(file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["resumes"] });
    },
  });
}
