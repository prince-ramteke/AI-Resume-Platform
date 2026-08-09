/**
 * Human-readable byte size, e.g. 52340 -> "51.1 KB", 1_600_000 -> "1.5 MB".
 * Binary units (1024). Pure. Guards against negative/NaN input (renders "—").
 */
export function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return "—";
  if (bytes < 1024) return `${bytes} B`;

  const units = ["KB", "MB", "GB"];
  let value = bytes / 1024;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  // One decimal place, but drop a trailing ".0" for whole numbers.
  const rounded = Math.round(value * 10) / 10;
  return `${rounded} ${units[unitIndex]}`;
}
