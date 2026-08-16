import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAnalysis } from "../../hooks/useAnalysis";
import { PageHeader } from "../primitives/PageHeader";
import { Card } from "../primitives/Card";
import { Button } from "../primitives/Button";
import { Eyebrow } from "../primitives/Eyebrow";
import { ScoreDial } from "../primitives/ScoreDial";
import { Badge } from "../primitives/Badge";
import { Chip } from "../primitives/Chip";
import { ErrorState } from "../primitives/ErrorState";
import type { AnalysisEvidence, AnalysisSkill } from "../../types/analysis";
import { formatRelativeTime } from "../../lib/formatDate";

function verdictHeadline(score: number, matched: number, total: number): string {
  if (score >= 80) return `Strong match — ${matched} of ${total} requirements evidenced.`;
  if (score >= 60) return `Reasonable match — ${matched} of ${total} requirements evidenced.`;
  return `Weak match — ${matched} of ${total} requirements evidenced.`;
}

function SkillRow({ skill, onCite }: { skill: AnalysisSkill; onCite: (ref: string) => void }) {
  return (
    <li style={{ display: "flex", alignItems: "flex-start", gap: 12, padding: "10px 0", borderTop: "1px solid var(--line-1)" }}>
      <Chip tone="evidence" onClick={() => onCite(skill.evidenceRef)}>
        {skill.skill}
      </Chip>
      <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--fg-4)", marginLeft: "auto" }}>
        {skill.importance.toLowerCase()}
      </span>
    </li>
  );
}

export function AnalysisResultPage() {
  const { id } = useParams<{ id: string }>();
  const numericId = Number(id);
  const navigate = useNavigate();
  const { data, isPending, isError, refetch } = useAnalysis(numericId);
  const [activeRef, setActiveRef] = useState<string | null>(null);

  if (!Number.isInteger(numericId) || numericId <= 0) {
    return (
      <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: 48 }}>
        <ErrorState code="route.analysis" onRetry={() => navigate("/analyses")} retryLabel="Back to history" />
      </div>
    );
  }
  if (isError) {
    return (
      <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: 48 }}>
        <ErrorState code="fetch.analysis" onRetry={() => void refetch()} />
      </div>
    );
  }
  if (isPending || !data) {
    return (
      <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: 48 }}>
        <Card><div style={{ color: "var(--fg-3)" }}>Loading analysis…</div></Card>
      </div>
    );
  }

  const matched = data.matchedSkills.length;
  const missing = data.missingSkills.length;
  const weak = data.weakSkills.length;
  const totalReq = matched + missing + weak;

  const evidenceByRef = new Map<string, AnalysisEvidence>();
  data.evidence.forEach((e) => evidenceByRef.set(e.ref, e));

  return (
    <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: "48px 32px" }}>
      <PageHeader
        eyebrow={`Analysis · #${data.id} · complete`}
        title={verdictHeadline(data.score, matched, totalReq)}
        meta={`${data.provider} · ${data.latencyMs}ms · ${formatRelativeTime(data.createdAt)}`}
        actions={
          <div style={{ display: "flex", gap: 12 }}>
            <ScoreDial value={data.score} size={140} label="match score" />
          </div>
        }
      />

      <div style={{ display: "flex", gap: 8, marginBottom: 32, flexWrap: "wrap" }}>
        <Badge kind="evidence">{matched} matched</Badge>
        <Badge kind="warn">{weak} weak</Badge>
        <Badge kind="fail">{missing} missing</Badge>
        <Badge>{data.evidence.length} evidence chunks</Badge>
      </div>

      <div className="app-result-grid" style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 32 }}>
        <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
          <Card>
            <Eyebrow>Matched requirements</Eyebrow>
            <ul style={{ listStyle: "none", padding: 0, margin: "16px 0 0" }}>
              {data.matchedSkills.map((s, i) => <SkillRow key={i} skill={s} onCite={setActiveRef} />)}
              {data.matchedSkills.length === 0 && <li style={{ color: "var(--fg-3)", fontSize: 13 }}>None recorded.</li>}
            </ul>
          </Card>
          <Card>
            <Eyebrow>Weak signal</Eyebrow>
            <ul style={{ listStyle: "none", padding: 0, margin: "16px 0 0" }}>
              {data.weakSkills.map((s, i) => <SkillRow key={i} skill={s} onCite={setActiveRef} />)}
              {data.weakSkills.length === 0 && <li style={{ color: "var(--fg-3)", fontSize: 13 }}>None recorded.</li>}
            </ul>
          </Card>
          <Card>
            <Eyebrow>Missing</Eyebrow>
            <ul style={{ listStyle: "none", padding: 0, margin: "16px 0 0" }}>
              {data.missingSkills.map((s, i) => <SkillRow key={i} skill={s} onCite={setActiveRef} />)}
              {data.missingSkills.length === 0 && <li style={{ color: "var(--fg-3)", fontSize: 13 }}>None recorded.</li>}
            </ul>
          </Card>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
          <Card>
            <Eyebrow>Evidence · cited chunks</Eyebrow>
            <div style={{ marginTop: 16, display: "flex", flexDirection: "column", gap: 10 }}>
              {data.evidence.map((e) => {
                const on = e.ref === activeRef;
                return (
                  <div
                    key={e.ref}
                    onMouseEnter={() => setActiveRef(e.ref)}
                    onFocus={() => setActiveRef(e.ref)}
                    tabIndex={0}
                    style={{
                      padding: "14px 16px",
                      borderRadius: "var(--r-control)",
                      background: on ? "rgba(var(--accent-rgb),.06)" : "transparent",
                      border: `1px solid ${on ? "rgba(var(--accent-rgb),.34)" : "var(--line-1)"}`,
                      transition: "var(--t-surface)",
                    }}
                  >
                    <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
                      <span style={{ fontFamily: "var(--font-mono)", fontSize: 12, color: on ? "var(--cyan-500)" : "var(--fg-4)" }}>{e.ref}</span>
                      <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, letterSpacing: ".02em", color: "var(--fg-5)" }}>
                        {e.sourceType} · chunk {e.chunkIndex}
                      </span>
                    </div>
                    <p style={{ marginTop: 8, fontFamily: "var(--font-display)", fontStyle: "italic", fontSize: 14, lineHeight: 1.6, color: on ? "var(--fg-1)" : "var(--fg-3)" }}>
                      "{e.snippet}"
                    </p>
                  </div>
                );
              })}
              {data.evidence.length === 0 && <div style={{ color: "var(--fg-3)", fontSize: 13 }}>No evidence retrieved.</div>}
            </div>
          </Card>
        </div>
      </div>

      <Card style={{ marginTop: 32 }}>
        <Eyebrow>Summary</Eyebrow>
        <p style={{ marginTop: 16, maxWidth: "66ch", fontSize: 17, lineHeight: 1.65, color: "var(--fg-2)" }}>
          {data.summary}
        </p>
        {data.recommendations.length > 0 && (
          <>
            <div style={{ marginTop: 24 }}><Eyebrow>Recommendations</Eyebrow></div>
            <ol style={{ marginTop: 16, paddingLeft: 20, display: "flex", flexDirection: "column", gap: 14 }}>
              {data.recommendations.map((r, i) => (
                <li key={i} style={{ fontSize: 15, lineHeight: 1.6, color: "var(--fg-2)" }}>
                  <div>{r.text}</div>
                  <div style={{ marginTop: 4, fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--fg-4)", letterSpacing: ".02em" }}>
                    {r.impact.toLowerCase()} impact · {r.reason}
                  </div>
                </li>
              ))}
            </ol>
          </>
        )}
        <div style={{ marginTop: 32, display: "flex", gap: 12 }}>
          <Button to="/analyses/new" variant="primary">Run another analysis →</Button>
          <Button to="/analyses" variant="ghost">Back to history</Button>
        </div>
      </Card>

      <style>{`
        @media (max-width: 900px){
          .app-result-grid { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}
