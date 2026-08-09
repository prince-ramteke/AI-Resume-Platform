/**
 * Short, human label for a resume's file type ("PDF" / "DOCX"), derived from the
 * backend content-type with a filename-extension fallback. Presentation only.
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
  return "Document";
}
