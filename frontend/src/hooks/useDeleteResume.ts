import { useMutation, useQueryClient } from "@tanstack/react-query";
import { deleteResume } from "../api/resumes";

/**
 * Soft-delete a resume. On success drops the now-stale detail query from cache
 * and invalidates the list/recent feeds. Navigation (e.g. back to /resumes when
 * viewing the deleted item) is the caller's responsibility.
 */
export function useDeleteResume() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteResume(id),
    onSuccess: (_data, id) => {
      queryClient.removeQueries({ queryKey: ["resumes", "detail", id] });
      queryClient.invalidateQueries({ queryKey: ["resumes"] });
    },
  });
}
