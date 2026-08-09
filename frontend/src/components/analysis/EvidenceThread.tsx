import { useMemo, useRef } from "react";
import { EvidenceCard } from "./EvidenceCard";
import type {
  AnalysisEvidence,
  AnalysisSkill,
} from "../../types/analysis";

interface EvidenceThreadProps {
  evidence: AnalysisEvidence[];
  matchedSkills: AnalysisSkill[];
  missingSkills: AnalysisSkill[];
  weakSkills: AnalysisSkill[];
  /** The evidenceRef currently highlighted (from the last skill click). */
  highlightedRef: string | null;
  /** Called when a citing-claim chip is activated inside a card. */
  onSelectClaim: (evidenceRef: string) => void;
  /** Registers each card's ref-keyed DOM node so the parent can scroll to it. */
  registerCard: (ref: string, node: HTMLElement | null) => void;
}

/**
 * The evidence thread: passages ordered "what you have" (RESUME) then "what
 * the role wants" (JD). For each passage we compute the set of skill claims
 * that cite it, so users can hop between a skill and its source in either
 * direction. The parent owns the highlighted-ref state (so a click in one of
 * the skill columns can drive a scroll here).
 */
export function EvidenceThread({
  evidence,
  matchedSkills,
  missingSkills,
  weakSkills,
  highlightedRef,
  onSelectClaim,
  registerCard,
}: EvidenceThreadProps) {
  // Build a ref → list of citing claims once per verdict. Preserves each
  // bucket's original order, which keeps chip ordering stable across renders.
  const citationsByRef = useMemo(() => {
    const map = new Map<
      string,
      { claim: AnalysisSkill; bucket: "matched" | "missing" | "weak" }[]
    >();
    function push(bucket: "matched" | "missing" | "weak", list: AnalysisSkill[]) {
      list.forEach((claim) => {
        const arr = map.get(claim.evidenceRef) ?? [];
        arr.push({ claim, bucket });
        map.set(claim.evidenceRef, arr);
      });
    }
    push("matched", matchedSkills);
    push("missing", missingSkills);
    push("weak", weakSkills);
    return map;
  }, [matchedSkills, missingSkills, weakSkills]);

  const ordered = useMemo(() => {
    // RESUME entries first, then JD, preserving backend order within each group.
    return [
      ...evidence.filter((e) => e.sourceType === "RESUME"),
      ...evidence.filter((e) => e.sourceType === "JD"),
    ];
  }, [evidence]);

  // Keep the connector rail stable-sized regardless of card count.
  const railRef = useRef<HTMLDivElement>(null);

  if (ordered.length === 0) {
    return (
      <p className="rounded-card border border-border bg-surface p-6 text-sm text-muted">
        No supporting passages were cited for this analysis.
      </p>
    );
  }

  return (
    <div className="relative">
      {/* Vertical rail (decorative). Positioned behind the cards. */}
      <div
        ref={railRef}
        aria-hidden="true"
        className="absolute inset-y-0 left-4 w-px bg-border sm:left-5"
      />
      <ol className="relative flex flex-col gap-4">
        {ordered.map((e) => {
          const anchorId = `evidence-${e.ref.replace(/[^A-Za-z0-9]/g, "-")}`;
          return (
            <li key={e.ref} className="relative pl-6 sm:pl-8">
              {/* Rail node (decorative dot aligned with the card top). */}
              <span
                aria-hidden="true"
                className="absolute left-3 top-5 h-2.5 w-2.5 rounded-full bg-accent ring-4 ring-bg sm:left-4"
              />
              <EvidenceCard
                ref={(node) => registerCard(e.ref, node)}
                evidence={e}
                citingClaims={citationsByRef.get(e.ref) ?? []}
                highlighted={highlightedRef === e.ref}
                anchorId={anchorId}
                onSelectClaim={onSelectClaim}
              />
            </li>
          );
        })}
      </ol>
    </div>
  );
}
