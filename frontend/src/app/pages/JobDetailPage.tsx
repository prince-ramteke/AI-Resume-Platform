import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useJobDescription } from "../../hooks/useJobDescription";
import { useDeleteJobDescription } from "../../hooks/useDeleteJobDescription";
import { useUpdateJobDescription } from "../../hooks/useUpdateJobDescription";
import { PageHeader } from "../primitives/PageHeader";
import { Card } from "../primitives/Card";
import { Button } from "../primitives/Button";
import { Eyebrow } from "../primitives/Eyebrow";
import { DataList } from "../primitives/DataList";
import { ErrorState } from "../primitives/ErrorState";
import { Input } from "../primitives/Input";
import { Textarea } from "../primitives/Textarea";
import { validateJdRawText, validateJdTitle } from "../../lib/validators";
import { formatRelativeTime } from "../../lib/formatDate";

export function JobDetailPage() {
  const { id } = useParams<{ id: string }>();
  const numericId = Number(id);
  const navigate = useNavigate();
  const { data, isPending, isError, refetch } = useJobDescription(numericId);
  const del = useDeleteJobDescription();
  const update = useUpdateJobDescription(numericId);

  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState("");
  const [text, setText] = useState("");
  const [errs, setErrs] = useState<{ title?: string; text?: string; api?: string }>({});

  if (!Number.isInteger(numericId) || numericId <= 0) {
    return (
      <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: 48 }}>
        <ErrorState code="route.jobDescription" onRetry={() => navigate("/job-descriptions")} retryLabel="Back to job descriptions" />
      </div>
    );
  }
  if (isError) {
    return (
      <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: 48 }}>
        <ErrorState code="fetch.jobDescription" onRetry={() => void refetch()} />
      </div>
    );
  }
  if (isPending || !data) {
    return (
      <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: 48 }}>
        <Card><div style={{ color: "var(--fg-3)" }}>Loading job description…</div></Card>
      </div>
    );
  }

  function beginEdit() {
    if (!data) return;
    setTitle(data.title);
    setText(data.rawText);
    setErrs({});
    setEditing(true);
  }

  function saveEdit(e: React.FormEvent) {
    e.preventDefault();
    const t = validateJdTitle(title);
    const b = validateJdRawText(text);
    setErrs({ title: t, text: b });
    if (t || b) return;
    update.mutate(
      { title: title.trim(), rawText: text.trim() },
      {
        onSuccess: () => setEditing(false),
        onError: (err) => setErrs((prev) => ({ ...prev, api: err instanceof Error ? err.message : "Save failed." })),
      }
    );
  }

  return (
    <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: "48px 32px" }}>
      <PageHeader
        eyebrow={`Job description · #${data.id}`}
        title={data.title}
        meta={`added ${formatRelativeTime(data.createdAt)}${data.updatedAt ? ` · edited ${formatRelativeTime(data.updatedAt)}` : ""}`}
        actions={
          <>
            {!editing && <Button variant="secondary" onClick={beginEdit}>Edit</Button>}
            <Button
              variant="danger"
              onClick={() => {
                if (!window.confirm("Delete this job description? This cannot be undone.")) return;
                del.mutate(data.id, { onSuccess: () => navigate("/job-descriptions", { replace: true }) });
              }}
            >
              Delete
            </Button>
          </>
        }
      />

      <div className="app-detail-grid" style={{ display: "grid", gridTemplateColumns: "minmax(0, 2fr) minmax(0, 1fr)", gap: 32 }}>
        <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
          {editing ? (
            <Card>
              <form onSubmit={saveEdit} style={{ display: "flex", flexDirection: "column", gap: 20 }}>
                <Input label="Title" value={title} onChange={(e) => setTitle(e.target.value)} error={errs.title} required />
                <Textarea label="Description" value={text} onChange={(e) => setText(e.target.value)} error={errs.text} rows={18} required />
                {errs.api && <div style={{ color: "var(--signal-fail)", fontFamily: "var(--font-mono)", fontSize: 12 }}>{errs.api}</div>}
                <div style={{ display: "flex", gap: 12, justifyContent: "flex-end" }}>
                  <Button type="button" variant="ghost" onClick={() => setEditing(false)}>Cancel</Button>
                  <Button type="submit" variant="primary" disabled={update.isPending}>
                    {update.isPending ? "Saving…" : "Save →"}
                  </Button>
                </div>
              </form>
            </Card>
          ) : (
            <Card>
              <Eyebrow>Description</Eyebrow>
              <pre style={{
                marginTop: 16,
                whiteSpace: "pre-wrap",
                wordBreak: "break-word",
                fontFamily: "var(--font-mono)",
                fontSize: 13,
                lineHeight: 1.65,
                color: "var(--fg-2)",
                borderLeft: "1px solid var(--line-2)",
                paddingLeft: 16,
                maxHeight: 560,
                overflow: "auto",
              }}>{data.rawText}</pre>
            </Card>
          )}
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
          <Card>
            <Eyebrow>Next action</Eyebrow>
            <div style={{ marginTop: 16 }}>
              <Button to={`/analyses/new?job=${data.id}`} variant="primary" full>
                Analyze a resume →
              </Button>
            </div>
          </Card>
          <Card>
            <Eyebrow>Metadata</Eyebrow>
            <div style={{ marginTop: 16 }}>
              <DataList
                items={[
                  { label: "Length", value: `${data.rawText.length.toLocaleString()} chars` },
                  { label: "Type", value: data.contentType ?? "text" },
                  { label: "Pages", value: data.pageCount ?? "—" },
                  { label: "Language", value: data.language ?? "—" },
                  { label: "Added", value: formatRelativeTime(data.createdAt) },
                  { label: "Edited", value: data.updatedAt ? formatRelativeTime(data.updatedAt) : "—" },
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
