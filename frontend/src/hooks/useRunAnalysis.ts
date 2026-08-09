import { useMutation, useQueryClient } from "@tanstack/react-query";
import { runAnalysis } from "../api/analyses";
import type { AnalysisDetail } from "../types/analysis";

interface RunInput {
  resumeId: number;
  jobDescriptionId: number;
  /**
   * Optional AbortSignal wired to the axios request. When aborted, only the
   * client-side wait stops — the backend request (a synchronous LLM call with
   * no cancel endpoint) keeps running and may still persist an Analysis row.
   * The UI must not present abort as a real cancellation.
   */
  signal?: AbortSignal;
}

/**
 * Run an analysis. On success, seeds the detail cache directly from the POST
 * response (no extra roundtrip) and invalidates every `analyses` query so the
 * history list, dashboard count, and recent feed refetch.
 *
 * The mutation is intentionally slow on local Ollama (~2 minutes measured);
 * the page owning this hook is responsible for the long-running UX, double-
 * submit prevention, and any nav-guard while `isPending`.
 */
export function useRunAnalysis() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ resumeId, jobDescriptionId, signal }: RunInput) =>
      runAnalysis({ resumeId, jobDescriptionId }, { signal }),
    onSuccess: (result: AnalysisDetail) => {
      queryClient.setQueryData(["analyses", "detail", result.id], result);
      queryClient.invalidateQueries({ queryKey: ["analyses"] });
    },
  });
}
