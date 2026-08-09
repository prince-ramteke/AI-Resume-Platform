import { forwardRef } from "react";
import { ResumeIcon, JobIcon } from "../layout/icons";
import { cn } from "../../lib/cn";
import type {
  AnalysisEvidence,
  AnalysisSkill,
} from "../../types/analysis";

interface CitingClaim {
  claim: AnalysisSkill;
  bucket: "matched" | "missing" | "weak";
}

interface EvidenceCardProps {
  evidence: AnalysisEvidence;
  /** Claims (across all three buckets) that cite this evidence. */
  citingClaims: CitingClaim[];
  /** When true, wraps the card in a temporary highlight ring. */
  highlighted: boolean;
  /** DOM id used both for scroll target and the hash anchor. */
  anchorId: string;
  /** Called when a citing-claim chip is activated, to scroll up to it. */
  onSelectClaim: (evidenceRef: string) => void;
}

const BUCKET_LABEL: Record<CitingClaim["bucket"], string> = {
  matched: "Matched",
  missing: "Missing",
  weak: "Weak",
};

const BUCKET_TONE_CLASS: Record<CitingClaim["bucket"], string> = {
  matched: "border-success/40 text-success",
  missing: "border-danger/40 text-danger",
  weak: "border-warning/40 text-warning",
};

/**
 * One passage in the evidence thread. Human phrasing on the outside ("From
 * your resume · passage #2"), with the raw citation tag kept as a small mono
 * label for verifiability. The snippet is a plain block-quote — no
 * character-level highlighting (out of scope). The citing-claims footer
 * closes the loop back to the skill columns above.
 *
 * Forwarded ref lets the thread scroll to a specific card without a query
 * selector.
 */
export const EvidenceCard = forwardRef<HTMLElement, EvidenceCardProps>(
  function EvidenceCard(
    { evidence, citingClaims, highlighted, anchorId, onSelectClaim },
    ref
  ) {
    const isResume = evidence.sourceType === "RESUME";
    const sourceLabel = isResume ? "From your resume" : "From the job description";
    const Icon = isResume ? ResumeIcon : JobIcon;
    const headingId = `${anchorId}-heading`;

    return (
      <article
        ref={ref}
        id={anchorId}
        aria-labelledby={headingId}
        className={cn(
          "relative rounded-card border border-border bg-surface p-4 transition-all",
          highlighted && "ring-2 ring-accent"
        )}
      >
        <div className="flex items-center gap-2">
          <span className="shrink-0 text-accent" aria-hidden="true">
            <Icon />
          </span>
          <h4 id={headingId} className="text-sm font-semibold text-ink">
            {sourceLabel} · passage #{evidence.chunkIndex}
          </h4>
          <span
            className="ml-auto rounded-control border border-border bg-surface-sunken px-2 py-0.5 font-mono text-[11px] text-muted"
            aria-label={`Citation tag ${evidence.ref}`}
          >
            {evidence.ref}
          </span>
        </div>

        <blockquote className="mt-3 whitespace-pre-wrap break-words rounded-control border-l-2 border-border bg-surface-sunken px-3 py-2 text-sm leading-relaxed text-ink">
          {evidence.snippet}
        </blockquote>

        {citingClaims.length > 0 && (
          <nav aria-label="Claims citing this passage" className="mt-3">
            <p className="mb-1.5 text-xs font-medium uppercase tracking-wide text-muted">
              Cited by
            </p>
            <ul className="flex flex-wrap gap-1.5">
              {citingClaims.map(({ claim, bucket }, i) => (
                <li key={`${claim.skill}-${i}`}>
                  <button
                    type="button"
                    onClick={() => onSelectClaim(claim.evidenceRef)}
                    className={cn(
                      "rounded-full border bg-surface px-2.5 py-0.5 text-xs transition-colors hover:bg-surface-sunken focus:outline-none focus-visible:ring-2 focus-visible:ring-accent",
                      BUCKET_TONE_CLASS[bucket]
                    )}
                    aria-label={`${BUCKET_LABEL[bucket]} skill: ${claim.skill}. Jump back to it.`}
                  >
                    {BUCKET_LABEL[bucket]} · {claim.skill}
                  </button>
                </li>
              ))}
            </ul>
          </nav>
        )}
      </article>
    );
  }
);
