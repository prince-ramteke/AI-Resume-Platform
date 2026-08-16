import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAnalyses } from "../../hooks/useAnalyses";
import { PageHeader } from "../primitives/PageHeader";
import { Card } from "../primitives/Card";
import { Button } from "../primitives/Button";
import { Empty } from "../primitives/Empty";
import { ErrorState } from "../primitives/ErrorState";
import { Badge } from "../primitives/Badge";
import { Table } from "../primitives/Table";
import type { TableColumn } from "../primitives/Table";
import { ScoreDial } from "../primitives/ScoreDial";
import type { AnalysisSummary } from "../../types/analysis";
import { formatRelativeTime } from "../../lib/formatDate";

const PAGE_SIZE = 20;

function verdictKind(score: number): "pass" | "warn" | "fail" {
  if (score >= 80) return "pass";
  if (score >= 60) return "warn";
  return "fail";
}

export function AnalysisHistoryPage() {
  const [page, setPage] = useState(0);
  const navigate = useNavigate();
  const { data, isPending, isError, refetch, isPlaceholderData } = useAnalyses({
    page,
    size: PAGE_SIZE,
    sort: "createdAt,desc",
  });

  const columns: TableColumn<AnalysisSummary>[] = [
    { key: "id", header: "#", render: (r) => <span style={{ fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--fg-4)" }}>#{r.id}</span> },
    { key: "job", header: "Job", render: (r) => <span style={{ color: "var(--fg-1)" }}>{r.jobTitle}</span> },
    { key: "score", header: "Score", render: (r) => <ScoreDial value={r.score} size={44} label="" /> },
    { key: "verdict", header: "Verdict", render: (r) => <Badge kind={verdictKind(r.score)}>{verdictKind(r.score)}</Badge> },
    { key: "run", header: "Run", render: (r) => <span style={{ fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--fg-3)" }}>{formatRelativeTime(r.createdAt)}</span> },
    { key: "open", header: "", render: () => <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, letterSpacing: ".14em", textTransform: "uppercase", color: "var(--cyan-500)" }}>Open →</span> },
  ];

  return (
    <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: "48px 32px" }}>
      <PageHeader
        eyebrow="History"
        title="Analyses."
        actions={<Button to="/analyses/new" variant="primary">New analysis →</Button>}
      />

      {isPending ? (
        <Card><div style={{ color: "var(--fg-3)" }}>Loading analyses…</div></Card>
      ) : isError ? (
        <ErrorState code="fetch.analyses" onRetry={() => void refetch()} />
      ) : data && data.content.length === 0 ? (
        <Card>
          <Empty
            title="No analyses yet."
            body="Run your first match between a resume and a job description to see it here."
            action={<Button to="/analyses/new" variant="primary">Start your first analysis →</Button>}
          />
        </Card>
      ) : (
        <>
          <Card padding={0}>
            <Table
              columns={columns}
              rows={data!.content}
              rowKey={(r) => r.id}
              onRowClick={(r) => navigate(`/analyses/${r.id}`)}
              ariaLabel="Analysis history"
            />
          </Card>
          <div style={{ marginTop: 24, display: "flex", alignItems: "center", justifyContent: "space-between", fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--fg-4)" }}>
            <span>
              page {data!.number + 1} of {Math.max(1, data!.totalPages)} · {data!.totalElements} total
            </span>
            <div style={{ display: "flex", gap: 8 }}>
              <Button
                variant="ghost"
                size="sm"
                disabled={data!.first || isPlaceholderData}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Prev
              </Button>
              <Button
                variant="ghost"
                size="sm"
                disabled={data!.last || isPlaceholderData}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
