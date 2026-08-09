/**
 * Short, human label for a document's file type ("PDF" / "DOCX" / "TXT"),
 * derived from the backend content-type with a filename-extension fallback.
 * Presentation only.
 */
export function fileKindLabel(contentType: string, filename: string): string {
  const name = filename.toLowerCase();
  if (contentType === "application/pdf" || name.endsWith(".pdf")) return "PDF";
  if (
    contentType.includes("wordprocessingml") ||
    name.endsWith(".docx")
  ) {
    return "DOCX";
  }
  if (contentType === "text/plain" || name.endsWith(".txt")) return "TXT";
  return "Document";
}

/**
 * Kind label for a job description row. Text-paste JDs have a null
 * `contentType`/`fileSize`; render them as "Text" so users can tell the source
 * apart at a glance. File-based JDs delegate to `fileKindLabel` using the
 * title as the filename fallback.
 */
export function jobDescriptionKindLabel(
  contentType: string | null,
  title: string
): string {
  if (!contentType) return "Text";
  return fileKindLabel(contentType, title);
}
