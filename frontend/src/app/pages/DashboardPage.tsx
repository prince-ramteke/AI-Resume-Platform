import { Link } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import { useRecentResumes } from "../../hooks/useRecentResumes";
import { useRecentJobDescriptions } from "../../hooks/useRecentJobDescriptions";
import { useRecentAnalyses } from "../../hooks/useRecentAnalyses";
import { PageHeader } from "../primitives/PageHeader";
import { Card } from "../primitives/Card";
import { Button } from "../primitives/Button";
import { Eyebrow } from "../primitives/Eyebrow";
import { Empty } from "../primitives/Empty";
import { ScoreDial } from "../primitives/ScoreDial";
import { PipelineTrack, PIPELINE_STAGES } from "../pipeline/PipelineTrack";
import { displayNameFromEmail, formatRelativeTime, greetingForHour } from "../../lib/formatDate";

function Kpi({ label, value }: { label: string; value: string | number }) {
  return (
    <Card>
      <Eyebrow>{label}</Eyebrow>
      <div style={{ marginTop: 12, fontFamily: "var(--font-display)", fontSize: 40, lineHeight: 1.06, color: "var(--fg-1)", fontVariantNumeric: "tabular-nums" }}>
        {value}
      </div>
    </Card>
  );
}

export function DashboardPage() {
  const { user } = useAuth();
  const resumes = useRecentResumes();
  const jobDescriptions = useRecentJobDescriptions();
  const analyses = useRecentAnalyses();

  const name = displayNameFromEmail(user?.email);
  const greeting = greetingForHour();
  const lastAnalysis = analyses.data?.content?.[0];

  const isFirstRun =
    resumes.isSuccess &&
    jobDescriptions.isSuccess &&
    analyses.isSuccess &&
    resumes.data.totalElements === 0 &&
    jobDescriptions.data.totalElements === 0 &&
    analyses.data.totalElements === 0;

  const avg = (() => {
    const rows = analyses.data?.content ?? [];
    if (rows.length === 0) return "—";
    const s = rows.reduce((acc, a) => acc + a.score, 0) / rows.length;
    return Math.round(s).toString();
  })();

  const idleSteps = PIPELINE_STAGES.map((label) => ({ label, state: "idle" as const }));

  return (
    <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: "48px 32px" }}>
      <PageHeader
        eyebrow="Dashboard"
        title={<>
          {greeting}
          {name ? <>, <span style={{ color: "var(--cyan-500)" }}>{name}</span></> : null}.
        </>}
        meta={
          lastAnalysis
            ? `last analysis · ${formatRelativeTime(lastAnalysis.createdAt)} · ${lastAnalysis.jobTitle}`
            : "no analyses yet"
        }
        actions={<Button to="/analyses/new" variant="primary">New analysis →</Button>}
      />

      {isFirstRun ? (
        <Card>
          <Empty
            eyebrow="Get started"
            title="Let's read your first resume."
            body="Upload a PDF or DOCX to see how a tracking system parses it, then match it against a job description."
            action={<Button to="/resumes" variant="primary">Upload resume →</Button>}
          />
        </Card>
      ) : (
        <>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(4, minmax(0, 1fr))", gap: 24, marginBottom: 48 }} className="app-kpi-row">
            <Kpi label="Resumes" value={resumes.data?.totalElements ?? "—"} />
            <Kpi label="Job descriptions" value={jobDescriptions.data?.totalElements ?? "—"} />
            <Kpi label="Analyses" value={analyses.data?.totalElements ?? "—"} />
            <Kpi label="Average match" value={avg} />
          </div>

          {analyses.data && analyses.data.content.length > 0 && (
            <section style={{ marginBottom: 48 }}>
              <Eyebrow>Continue where you left off</Eyebrow>
              <div style={{ marginTop: 20, display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: 24 }}>
                {analyses.data.content.slice(0, 3).map((a) => (
                  <Card key={a.id}>
                    <div style={{ display: "flex", gap: 20, alignItems: "center" }}>
                      <ScoreDial value={a.score} size={72} label="score" />
                      <div style={{ minWidth: 0, flex: 1 }}>
                        <h3 style={{ fontSize: "var(--app-fs-h3)", lineHeight: "var(--app-lh-h3)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{a.jobTitle}</h3>
                        <div style={{ marginTop: 6, fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--fg-4)", letterSpacing: ".02em" }}>
                          #{a.id} · {formatRelativeTime(a.createdAt)}
                        </div>
                        <div style={{ marginTop: 14 }}>
                          <Link to={`/analyses/${a.id}`} style={{ fontFamily: "var(--font-mono)", fontSize: 12, textTransform: "uppercase", letterSpacing: ".14em", color: "var(--cyan-500)" }}>
                            Open →
                          </Link>
                        </div>
                      </div>
                    </div>
                  </Card>
                ))}
              </div>
            </section>
          )}

          <Card tone="stage" style={{ overflow: "hidden" }}>
            <Eyebrow>Start a new analysis</Eyebrow>
            <h2 style={{ marginTop: 12, fontSize: "var(--app-fs-h2)", lineHeight: "var(--app-lh-h2)", maxWidth: "32ch" }}>
              Match a resume against a role.
            </h2>
            <div style={{ marginTop: 32 }}>
              <PipelineTrack steps={idleSteps} />
            </div>
            <div style={{ marginTop: 32 }}>
              <Button to="/analyses/new" variant="primary">New analysis →</Button>
            </div>
          </Card>
        </>
      )}

      <style>{`
        @media (max-width: 1023px){
          .app-kpi-row { grid-template-columns: repeat(2, minmax(0, 1fr)) !important; }
        }
        @media (max-width: 640px){
          .app-kpi-row { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}
