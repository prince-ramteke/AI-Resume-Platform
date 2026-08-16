import { useNavigate, useParams } from "react-router-dom";
import { useResume } from "../../hooks/useResume";
import { useDeleteResume } from "../../hooks/useDeleteResume";
import { useDownloadResume } from "../../hooks/useDownloadResume";
import { PageHeader } from "../primitives/PageHeader";
import { Card } from "../primitives/Card";
import { Button } from "../primitives/Button";
import { Eyebrow } from "../primitives/Eyebrow";
import { DataList } from "../primitives/DataList";
import { ErrorState } from "../primitives/ErrorState";
import { formatFileSize } from "../../lib/formatFileSize";
import { formatRelativeTime } from "../../lib/formatDate";

export function ResumeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const numericId = Number(id);
  const navigate = useNavigate();
  const { data, isPending, isError, refetch } = useResume(numericId);
  const del = useDeleteResume();
  const download = useDownloadResume();

  if (!Number.isInteger(numericId) || numericId <= 0) {
    return (
      <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: 48 }}>
        <ErrorState code="route.resume" onRetry={() => navigate("/resumes")} retryLabel="Back to resumes" />
      </div>
    );
  }

  if (isError) {
    return (
      <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: 48 }}>
        <ErrorState code="fetch.resume" onRetry={() => void refetch()} />
      </div>
    );
  }

  if (isPending || !data) {
    return (
      <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: 48 }}>
        <Card><div style={{ color: "var(--fg-3)" }}>Loading resume…</div></Card>
      </div>
    );
  }

  const excerpt = data.rawText.slice(0, 2400);

  return (
    <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: "48px 32px" }}>
      <PageHeader
        eyebrow={`Resume · #${data.id}`}
        title={data.filename}
        meta={`uploaded ${formatRelativeTime(data.createdAt)}${data.updatedAt ? ` · replaced ${formatRelativeTime(data.updatedAt)}` : ""}`}
        actions={
          <>
            <Button variant="secondary" onClick={() => download.mutate({ id: data.id, fallbackName: data.filename })}>
              Download
            </Button>
            <Button
              variant="danger"
              onClick={() => {
                if (!window.confirm("Delete this resume? This cannot be undone.")) return;
                del.mutate(data.id, { onSuccess: () => navigate("/resumes", { replace: true }) });
              }}
            >
              Delete
            </Button>
          </>
        }
      />

      <div className="app-detail-grid" style={{ display: "grid", gridTemplateColumns: "minmax(0, 2fr) minmax(0, 1fr)", gap: 32 }}>
        <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
          <Card kind="paper" padding={40}>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: 10.5, letterSpacing: ".16em", textTransform: "uppercase", color: "var(--paper-ink-2)", marginBottom: 20 }}>
              {data.filename}
            </div>
            <pre style={{
              margin: 0,
              whiteSpace: "pre-wrap",
              wordBreak: "break-word",
              fontFamily: "var(--font-sans)",
              fontSize: 14,
              lineHeight: 1.7,
              color: "var(--paper-ink)",
              maxHeight: 480,
              overflow: "auto",
            }}>{excerpt}{data.rawText.length > excerpt.length ? "\n…" : ""}</pre>
          </Card>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
          <Card>
            <Eyebrow>Next action</Eyebrow>
            <div style={{ marginTop: 16 }}>
              <Button to={`/analyses/new?resume=${data.id}`} variant="primary" full>
                Analyze against role →
              </Button>
            </div>
          </Card>

          <Card>
            <Eyebrow>Metadata</Eyebrow>
            <div style={{ marginTop: 16 }}>
              <DataList
                items={[
                  { label: "File size", value: formatFileSize(data.fileSize) },
                  { label: "Type", value: data.contentType },
                  { label: "Pages", value: data.pageCount ?? "—" },
                  { label: "Language", value: data.language ?? "—" },
                  { label: "Uploaded", value: formatRelativeTime(data.createdAt) },
                  { label: "Replaced", value: data.updatedAt ? formatRelativeTime(data.updatedAt) : "—" },
                ]}
              />
            </div>
          </Card>
        </div>
      </div>

      <style>{`
        @media (max-width: 900px){
          .app-detail-grid { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}
