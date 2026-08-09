import { Badge } from "../ui";
import { SkillChip } from "./SkillChip";
import type { AnalysisSkill } from "../../types/analysis";

interface SkillColumnProps {
  title: "Matched" | "Missing" | "Weak";
  tone: "success" | "warning" | "danger";
  skills: AnalysisSkill[];
  /** Called with the evidenceRef of the clicked chip; page scrolls to it. */
  onSelectEvidence: (evidenceRef: string) => void;
  /** Optional bucket description (one short sentence for the header). */
  description: string;
  /** The evidenceRef currently highlighted — chips citing it get a ring. */
  highlightedRef: string | null;
}

/**
 * One of the three skill buckets on the result page. Renders a titled card
 * with a tone-matching badge and a stack of chips; when empty, shows a quiet
 * "None flagged" line instead of a blank box.
 */
export function SkillColumn({
  title,
  tone,
  skills,
  onSelectEvidence,
  description,
  highlightedRef,
}: SkillColumnProps) {
  return (
    <section
      aria-labelledby={`skills-${title.toLowerCase()}-heading`}
      className="flex flex-col rounded-card border border-border bg-surface p-4"
    >
      <div className="mb-1 flex items-center gap-2">
        <h3
          id={`skills-${title.toLowerCase()}-heading`}
          className="font-display text-base text-ink"
        >
          {title}
        </h3>
        <Badge tone={tone}>{skills.length}</Badge>
      </div>
      <p className="mb-3 text-xs text-muted">{description}</p>
      {skills.length === 0 ? (
        <p className="text-sm text-muted">None flagged.</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {skills.map((s, i) => (
            <li key={`${s.skill}-${i}`}>
              <SkillChip
                skill={s}
                tone={tone}
                onSelect={onSelectEvidence}
                active={highlightedRef === s.evidenceRef}
              />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
