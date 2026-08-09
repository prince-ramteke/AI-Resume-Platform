import { useMutation } from "@tanstack/react-query";
import { downloadJobDescription } from "../api/jobDescriptions";
import { saveBlob } from "../lib/saveBlob";

/**
 * Download a JD's original file. Modeled as a mutation (not a query) because
 * it's an explicit user action with a one-shot side effect. Only file-based
 * JDs have a file; text-paste JDs return 404 by design — callers gate the
 * button off in that case rather than firing this and swallowing the error.
 */
export function useDownloadJobDescription() {
  return useMutation({
    mutationFn: async ({
      id,
      fallbackName,
    }: {
      id: number;
      fallbackName: string;
    }) => {
      const { blob, filename } = await downloadJobDescription(id);
      saveBlob(blob, filename ?? fallbackName);
    },
  });
}
