import { useState } from "react";
import { Link } from "react-router-dom";
import { useJobDescriptions } from "../../hooks/useJobDescriptions";
import { useCreateJobDescriptionFromText } from "../../hooks/useCreateJobDescriptionFromText";
import { PageHeader } from "../primitives/PageHeader";
import { Card } from "../primitives/Card";
import { Button } from "../primitives/Button";
import { Empty } from "../primitives/Empty";
import { ErrorState } from "../primitives/ErrorState";
import { Input } from "../primitives/Input";
import { Textarea } from "../primitives/Textarea";
import { formatRelativeTime } from "../../lib/formatDate";
import { validateJdRawText, validateJdTitle } from "../../lib/validators";

export function JobLibraryPage() {
  const [addOpen, setAddOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [text, setText] = useState("");
  const [errors, setErrors] = useState<{ title?: string; text?: string; api?: string }>({});
  const create = useCreateJobDescriptionFromText();
  const { data, isPending, isError, refetch } = useJobDescriptions({ page: 0, size: 24, sort: "createdAt,desc" });

  function submit(e: React.FormEvent) {
    e.preventDefault();
    const t = validateJdTitle(title);
    const b = validateJdRawText(text);
    setErrors({ title: t, text: b });
    if (t || b) return;
    create.mutate(
      { title: title.trim(), rawText: text.trim() },
      {
        onSuccess: () => { setAddOpen(false); setTitle(""); setText(""); setErrors({}); },
        onError: (err) => setErrors({ api: err instanceof Error ? err.message : "Save failed." }),
      }
    );
  }

  return (
    <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: "48px 32px" }}>
      <PageHeader
        eyebrow="Job descriptions"
        title="Your job descriptions."
        actions={<Button variant="primary" onClick={() => setAddOpen((v) => !v)}>Add JD →</Button>}
      />

      {addOpen && (
        <Card style={{ marginBottom: 32 }}>
          <form onSubmit={submit} style={{ display: "flex", flexDirection: "column", gap: 20 }}>
            <Input label="Title" value={title} onChange={(e) => setTitle(e.target.value)} error={errors.title} placeholder="Senior ML Engineer @ Company" required />
            <Textarea label="Description" value={text} onChange={(e) => setText(e.target.value)} error={errors.text} placeholder="Paste the full job description here…" rows={10} required />
            {errors.api && <div style={{ color: "var(--signal-fail)", fontFamily: "var(--font-mono)", fontSize: 12 }}>{errors.api}</div>}
            <div style={{ display: "flex", gap: 12, justifyContent: "flex-end" }}>
              <Button type="button" variant="ghost" onClick={() => setAddOpen(false)}>Cancel</Button>
              <Button type="submit" variant="primary" disabled={create.isPending}>
                {create.isPending ? "Saving…" : "Save JD →"}
              </Button>
            </div>
          </form>
        </Card>
      )}

      {isPending ? (
        <Card><div style={{ color: "var(--fg-3)" }}>Loading job descriptions…</div></Card>
      ) : isError ? (
        <ErrorState code="fetch.jobDescriptions" onRetry={() => void refetch()} />
      ) : data && data.content.length === 0 ? (
        <Card>
          <Empty
            title="No job descriptions saved."
            body="Paste a JD to extract the requirements and match a resume against it."
            action={<Button variant="primary" onClick={() => setAddOpen(true)}>Add job description →</Button>}
          />
        </Card>
      ) : (
        <div className="app-jd-grid" style={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: 24 }}>
          {data!.content.map((jd) => (
            <Link key={jd.id} to={`/job-descriptions/${jd.id}`} style={{ textDecoration: "none" }}>
              <Card interactive padding={24} style={{ height: 220, display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
                <div>
                  <div style={{ fontFamily: "var(--font-mono)", fontSize: 10.5, letterSpacing: ".16em", textTransform: "uppercase", color: "var(--fg-4)" }}>
                    JD · #{jd.id}
                  </div>
                  <h3 style={{ marginTop: 12, fontSize: 18, lineHeight: 1.24, overflow: "hidden", textOverflow: "ellipsis", display: "-webkit-box", WebkitLineClamp: 3, WebkitBoxOrient: "vertical" }}>
                    {jd.title}
                  </h3>
                </div>
                <div>
                  <div style={{ fontFamily: "var(--font-mono)", fontSize: 11, letterSpacing: ".02em", color: "var(--fg-4)" }}>
                    added {formatRelativeTime(jd.createdAt)}
                  </div>
                  <div style={{ marginTop: 12, fontFamily: "var(--font-mono)", fontSize: 11, letterSpacing: ".14em", textTransform: "uppercase", color: "var(--cyan-500)" }}>
                    Open →
                  </div>
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}

      <style>{`
        @media (max-width: 1023px){ .app-jd-grid { grid-template-columns: repeat(2, minmax(0, 1fr)) !important; } }
        @media (max-width: 640px){ .app-jd-grid { grid-template-columns: 1fr !important; } }
      `}</style>
    </div>
  );
}
