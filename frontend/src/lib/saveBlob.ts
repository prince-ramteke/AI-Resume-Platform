/**
 * Trigger a browser "Save as…" for an in-memory Blob by creating a temporary
 * object URL and clicking a synthetic anchor. Kept out of pages/components so
 * the DOM side-effect lives in one place. The object URL is always revoked.
 *
 * Never logs or inspects the blob contents (resume files are sensitive).
 */
export function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  try {
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = filename || "download";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  } finally {
    URL.revokeObjectURL(url);
  }
}
