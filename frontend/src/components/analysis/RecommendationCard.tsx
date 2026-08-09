import { Badge } from "../ui";
import type {
  AnalysisRecommendation,
  SkillImportance,
} from "../../types/analysis";

interface RecommendationCardProps {
  recommendation: AnalysisRecommendation;
  /** 1-based index for the visible ordinal in the card header. */
  index: number;
}

const IMPACT_TONE: Record<SkillImportance, "success" | "warning" | "danger"> = {
  HIGH: "danger",
  MEDIUM: "warning",
  LOW: "success",
};

const IMPACT_LABEL: Record<SkillImportance, string> = {
  HIGH: "High impact",
  MEDIUM: "Medium impact",
  LOW: "Low impact",
};

/**
 * One recommendation. `text` is the actionable line; `reason` explains why
 * (kept smaller/muted so the action stays the visual focus). Impact drives
 * the badge tone so a high-impact fix reads as an attention item.
 */
export function RecommendationCard({
  recommendation,
  index,
}: RecommendationCardProps) {
  const tone = IMPACT_TONE[recommendation.impact];

  return (
    <article className="rounded-card border border-border bg-surface p-4">
      <div className="mb-2 flex items-center justify-between gap-3">
        <span className="text-xs font-medium uppercase tracking-wide text-muted">
          Suggestion {index}
        </span>
        <Badge tone={tone}>{IMPACT_LABEL[recommendation.impact]}</Badge>
      </div>
      <p className="text-sm font-medium text-ink">{recommendation.text}</p>
      {recommendation.reason && (
        <p className="mt-2 text-sm text-muted">
          <span className="font-medium text-ink">Why: </span>
          {recommendation.reason}
        </p>
      )}
    </article>
  );
}
