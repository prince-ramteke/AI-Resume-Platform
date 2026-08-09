import { useMutation, useQueryClient } from "@tanstack/react-query";
import { deleteJobDescription } from "../api/jobDescriptions";

/**
 * Soft-delete a JD. On success drops the now-stale detail query from cache
 * and invalidates the list/recent feeds. Navigation (e.g. back to the list
 * when viewing the deleted JD) is the caller's responsibility.
 */
export function useDeleteJobDescription() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteJobDescription(id),
    onSuccess: (_data, id) => {
      queryClient.removeQueries({ queryKey: ["jobDescriptions", "detail", id] });
      queryClient.invalidateQueries({ queryKey: ["jobDescriptions"] });
    },
  });
}
