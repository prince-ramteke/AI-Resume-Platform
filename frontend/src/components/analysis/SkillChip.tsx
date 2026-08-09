import { cn } from "../../lib/cn";
import type { AnalysisSkill, SkillImportance } from "../../types/analysis";

interface SkillChipProps {
  skill: AnalysisSkill;
  /** Bucket tone — drives the ring color so Missing reads as danger, etc. */
  tone: "success" | "warning" | "danger";
  /** Called with the skill's evidenceRef when the chip is activated. */
  onSelect: (evidenceRef: string) => void;
  /** Marks this chip as the currently focused pair on the evidence side. */
  active?: boolean;
}

const TONE_RING: Record<SkillChipProps["tone"], string> = {
  success: "border-success/40 hover:border-success",
  warning: "border-warning/40 hover:border-warning",
  danger: "border-danger/40 hover:border-danger",
};

const TONE_DOT: Record<SkillChipProps["tone"], string> = {
  success: "bg-success",
  warning: "bg-warning",
  danger: "bg-danger",
};

const IMPORTANCE_LABEL: Record<SkillImportance, string> = {
  HIGH: "High importance",
  MEDIUM: "Medium importance",
  LOW: "Low importance",
};

/**
 * A single skill claim, presented as a button that scrolls to and highlights
 * its cited evidence. The importance dot is a three-state visual (filled /
 * half / hollow) so the level is visible even without color. `aria-label`
 * carries both the skill and its importance so screen-reader users get the
 * full claim without the dot's decorative dependency.
 */
export function SkillChip({ skill, tone, onSelect, active }: SkillChipProps) {
  return (
    <button
      type="button"
      onClick={() => onSelect(skill.evidenceRef)}
      aria-label={`${skill.skill}. ${IMPORTANCE_LABEL[skill.importance]}. Jump to supporting evidence.`}
      className={cn(
        "group flex w-full items-center justify-between gap-3 rounded-control border bg-surface px-3 py-2 text-left text-sm text-ink transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-accent",
        TONE_RING[tone],
        active && "ring-2 ring-accent"
      )}
    >
      <span className="min-w-0 truncate font-medium">{skill.skill}</span>
      <ImportanceDot importance={skill.importance} toneClass={TONE_DOT[tone]} />
    </button>
  );
}

interface ImportanceDotProps {
  importance: SkillImportance;
  toneClass: string;
}

function ImportanceDot({ importance, toneClass }: ImportanceDotProps) {
  // Three-state visual: HIGH = filled, MEDIUM = half, LOW = hollow ring.
  if (importance === "HIGH") {
    return (
      <span
        aria-hidden="true"
        className={cn("inline-block h-2.5 w-2.5 shrink-0 rounded-full", toneClass)}
      />
    );
  }
  if (importance === "MEDIUM") {
    return (
      <span
        aria-hidden="true"
        className={cn(
          "relative inline-block h-2.5 w-2.5 shrink-0 overflow-hidden rounded-full border",
          toneClass.replace("bg-", "border-")
        )}
      >
        <span className={cn("absolute inset-y-0 left-0 w-1/2", toneClass)} />
      </span>
    );
  }
  return (
    <span
      aria-hidden="true"
      className={cn(
        "inline-block h-2.5 w-2.5 shrink-0 rounded-full border",
        toneClass.replace("bg-", "border-")
      )}
    />
  );
}
