import { useCallback, useRef, useState } from "react";
import { useParams, useSearchParams } from "react-router-dom";
import { PageHeader } from "../../components/layout/PageHeader";
import {
  Alert,
  Card,
  EmptyState,
  ErrorState,
  LinkButton,
  Skeleton,
} from "../../components/ui";
import { ScoreGauge } from "../../components/analysis/ScoreGauge";
import { SkillColumn } from "../../components/analysis/SkillColumn";
import { RecommendationCard } from "../../components/analysis/RecommendationCard";
import { EvidenceThread } from "../../components/analysis/EvidenceThread";
import { useAnalysis } from "../../hooks/useAnalysis";
import { parseApiError } from "../../api/errors";

const BACK = { to: "/analyses", label: "History" };
const HIGHLIGHT_MS = 2000;

function absoluteDateTime(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

function ResultSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <Card>
        <Skeleton className="h-6 w-64" />
        <div className="mt-4 grid gap-6 sm:grid-cols-[auto_1fr]">
          <Skeleton className="h-24 w-40 rounded-card" />
          <Skeleton className="h-24 w-full rounded-card" />
        </div>
      </Card>
      <Skeleton className="h-32 w-full rounded-card" />
      <Skeleton className="h-48 w-full rounded-card" />
    </div>
  );
}

/**
 * The signature result page. Shared by "just-analyzed" (`?fromRun=1`) and any
 * historical detail via `/analyses/:id`. The two halves — skill columns and
 * evidence thread — are wired two-ways via `highlightedRef`: clicking a chip
 * scrolls the matching evidence into view and briefly rings it, and each
 * evidence card lists the claims that cite it so the user can scroll back up.
 */
export function AnalysisResultPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const idNum = Number(idParam);
  const validId = Number.isInteger(idNum) && idNum > 0;
  const [searchParams] = useSearchParams();
  const fromRun = searchParams.get("fromRun") === "1";

  const query = useAnalysis(idNum);

  // Two-way link state + a map from evidenceRef → the card's DOM node.
  const [highlightedRef, setHighlightedRef] = useState<string | null>(null);
  const cardsRef = useRef(new Map<string, HTMLElement | null>());
  const highlightTimeoutRef = useRef<number | null>(null);

  const registerCard = useCallback((ref: string, node: HTMLElement | null) => {
    if (node === null) cardsRef.current.delete(ref);
    else cardsRef.current.set(ref, node);
  }, []);

  const selectEvidence = useCallback((evidenceRef: string) => {
    const node = cardsRef.current.get(evidenceRef);
    if (node) {
      node.scrollIntoView({ behavior: "smooth", block: "start" });
    }
    setHighlightedRef(evidenceRef);
    if (highlightTimeoutRef.current !== null) {
      window.clearTimeout(highlightTimeoutRef.current);
    }
    highlightTimeoutRef.current = window.setTimeout(() => {
      setHighlightedRef(null);
      highlightTimeoutRef.current = null;
    }, HIGHLIGHT_MS);
    // Also update the URL hash for shareable scroll positions.
    const anchorId = `evidence-${evidenceRef.replace(/[^A-Za-z0-9]/g, "-")}`;
    if (window.location.hash !== `#${anchorId}`) {
      history.replaceState(null, "", `#${anchorId}`);
    }
  }, []);

  const selectClaim = useCallback((evidenceRef: string) => {
    // Same behavior as selecting evidence — the "cited by" chip just closes
    // the loop back to the skill column, which lives higher on the page.
    // We reuse the evidence highlight to also acknowledge the claim visually.
    selectEvidence(evidenceRef);
  }, [selectEvidence]);

  // Invalid route param → same neutral not-found as a real 404.
  if (!validId) {
    return (
      <div>
        <PageHeader title="Analysis" back={BACK} />
        <EmptyState
          title="Analysis not found"
          description="This analysis isn't available. It may have been deleted."
          action={<LinkButton to="/analyses">Back to history</LinkButton>}
        />
      </div>
    );
  }

  if (query.isPending) {
    return (
      <div>
        <PageHeader title="Analysis" back={BACK} />
        <ResultSkeleton />
      </div>
    );
  }

  if (query.isError) {
    const parsed = parseApiError(query.error);
    if (parsed.status === 404) {
      return (
        <div>
          <PageHeader title="Analysis" back={BACK} />
          <EmptyState
            title="Analysis not found"
            description="This analysis isn't available. It may have been deleted."
            action={<LinkButton to="/analyses">Back to history</LinkButton>}
          />
        </div>
      );
    }
    return (
      <div>
        <PageHeader title="Analysis" back={BACK} />
        <ErrorState
          title="Couldn't load this analysis"
          message={parsed.message}
          onRetry={() => query.refetch()}
        />
      </div>
    );
  }

  const analysis = query.data;
  const totalSkillClaims =
    analysis.matchedSkills.length +
    analysis.missingSkills.length +
    analysis.weakSkills.length;
  const isDefensiveEmpty =
    totalSkillClaims === 0 && analysis.recommendations.length === 0;

  return (
    <div>
      <PageHeader title="Analysis" back={BACK} />

      {fromRun && (
        <div className="mb-6">
          <Alert tone="success">Analysis complete.</Alert>
        </div>
      )}

      {/* Score + summary hero */}
      <Card>
        <div className="grid gap-6 sm:grid-cols-[auto_1fr] sm:items-center">
          <ScoreGauge score={analysis.score} />
          <div className="min-w-0">
            <h2 className="font-display text-xl text-ink">Summary</h2>
            <p className="mt-2 text-sm leading-relaxed text-ink">
              {analysis.summary || "No summary was produced for this analysis."}
            </p>
            <p className="mt-4 text-xs text-muted">
              Ran {absoluteDateTime(analysis.createdAt)} · Provider:{" "}
              <span className="font-mono">{analysis.provider}</span>
              {fromRun && analysis.latencyMs > 0 && (
                <>
                  {" "}· LLM latency:{" "}
                  <span className="font-mono">
                    {(analysis.latencyMs / 1000).toFixed(1)}s
                  </span>
                </>
              )}
            </p>
          </div>
        </div>
      </Card>

      {isDefensiveEmpty ? (
        <div className="mt-8">
          <EmptyState
            title="This analysis produced no detailed output"
            description="The score was recorded, but no skills or recommendations were returned. You can run a new analysis to try again."
            action={<LinkButton to="/analyses/new">Run a new analysis</LinkButton>}
          />
        </div>
      ) : (
        <>
          {/* Three skill columns */}
          <section aria-labelledby="skills-heading" className="mt-8">
            <h2
              id="skills-heading"
              className="mb-3 font-display text-lg text-ink"
            >
              Skills at a glance
            </h2>
            <div className="grid gap-4 md:grid-cols-3">
              <SkillColumn
                title="Matched"
                tone="success"
                skills={analysis.matchedSkills}
                onSelectEvidence={selectEvidence}
                description="Skills the resume already shows for this role."
                highlightedRef={highlightedRef}
              />
              <SkillColumn
                title="Missing"
                tone="danger"
                skills={analysis.missingSkills}
                onSelectEvidence={selectEvidence}
                description="What the role asks for that the resume doesn't cover."
                highlightedRef={highlightedRef}
              />
              <SkillColumn
                title="Weak"
                tone="warning"
                skills={analysis.weakSkills}
                onSelectEvidence={selectEvidence}
                description="Present on the resume but light on evidence."
                highlightedRef={highlightedRef}
              />
            </div>
          </section>

          {/* Recommendations */}
          {analysis.recommendations.length > 0 && (
            <section aria-labelledby="recs-heading" className="mt-8">
              <h2
                id="recs-heading"
                className="mb-3 font-display text-lg text-ink"
              >
                Recommendations
              </h2>
              <ol className="flex flex-col gap-3">
                {analysis.recommendations.map((r, i) => (
                  <li key={i}>
                    <RecommendationCard recommendation={r} index={i + 1} />
                  </li>
                ))}
              </ol>
            </section>
          )}

          {/* Evidence thread */}
          <section aria-labelledby="evidence-heading" className="mt-8">
            <h2
              id="evidence-heading"
              className="mb-3 font-display text-lg text-ink"
            >
              Evidence
            </h2>
            <p className="mb-4 text-sm text-muted">
              Every skill above is grounded in one of these passages from your
              resume or the job description. Click a skill to jump to its
              source; click a passage's tag to jump back.
            </p>
            <EvidenceThread
              evidence={analysis.evidence}
              matchedSkills={analysis.matchedSkills}
              missingSkills={analysis.missingSkills}
              weakSkills={analysis.weakSkills}
              highlightedRef={highlightedRef}
              onSelectClaim={selectClaim}
              registerCard={registerCard}
            />
          </section>
        </>
      )}
    </div>
  );
}
