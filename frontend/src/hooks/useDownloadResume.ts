import { useMutation } from "@tanstack/react-query";
import { downloadResume } from "../api/resumes";
import { saveBlob } from "../lib/saveBlob";

/**
 * Download a resume's original file. Modeled as a mutation (not a query) because
 * it's an explicit user action with a one-shot side effect — the button gets
 * `isPending`/`isError` for free. `fallbackName` is used when the server's
 * Content-Disposition filename isn't exposed to the browser.
 */
export function useDownloadResume() {
  return useMutation({
    mutationFn: async ({
      id,
      fallbackName,
    }: {
      id: number;
      fallbackName: string;
    }) => {
      const { blob, filename } = await downloadResume(id);
      saveBlob(blob, filename ?? fallbackName);
    },
  });
}
