/**
 * Match-score → status tone. Shared by the dashboard row, the score gauge, and
 * anywhere else a score needs a consistent color meaning. Thresholds are
 * deliberately coarse (job seeker signal, not a fine gradient): a strong
 * match reads success, a mediocre match warning, a weak match danger.
 */
export type ScoreTone = "success" | "warning" | "danger";

export function toneForScore(score: number): ScoreTone {
  if (score >= 75) return "success";
  if (score >= 50) return "warning";
  return "danger";
}
