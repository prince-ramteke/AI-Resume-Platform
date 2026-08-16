import { useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useResumes } from "../../hooks/useResumes";
import { useUploadResume } from "../../hooks/useUploadResume";
import { PageHeader } from "../primitives/PageHeader";
import { Card } from "../primitives/Card";
import { Button } from "../primitives/Button";
import { Empty } from "../primitives/Empty";
import { ErrorState } from "../primitives/ErrorState";
import { Badge } from "../primitives/Badge";
import { validateResumeFile } from "../../lib/validators";
import { formatFileSize } from "../../lib/formatFileSize";
import { formatRelativeTime } from "../../lib/formatDate";

export function ResumeLibraryPage() {
  const [params, setParams] = useSearchParams();
  const openUploader = params.get("new") === "1";
  const { data, isPending, isError, refetch } = useResumes({ page: 0, size: 24, sort: "createdAt,desc" });
  const upload = useUploadResume();
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  function submitFile(file: File | null) {
    setUploadError(null);
    const err = validateResumeFile(file);
    if (err || !file) { setUploadError(err ?? "Choose a file."); return; }
    upload.mutate(file, {
      onSuccess: () => {
        const next = new URLSearchParams(params);
        next.delete("new");
        setParams(next, { replace: true });
      },
      onError: (e) => setUploadError(e instanceof Error ? e.message : "Upload failed."),
    });
  }

  return (
    <div style={{ maxWidth: "var(--max-content)", margin: "0 auto", padding: "48px 32px" }}>
      <PageHeader
        eyebrow="Resumes"
        title="Your resumes."
        actions={
          <Button variant="primary" onClick={() => { setParams({ new: "1" }, { replace: true }); inputRef.current?.click(); }}>
            Upload resume →
          </Button>
        }
      />

      {(openUploader || uploadError) && (
        <div style={{ marginBottom: 32 }}>
          <div
            onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
            onDragLeave={() => setDragging(false)}
            onDrop={(e) => { e.preventDefault(); setDragging(false); submitFile(e.dataTransfer.files?.[0] ?? null); }}
            style={{
              padding: 48,
              borderRadius: "var(--r-paper)",
              background: "var(--grad-paper)",
              boxShadow: "var(--elev-paper)",
              border: `1.5px dashed ${dragging ? "var(--cyan-500)" : "rgba(20,19,16,.28)"}`,
              color: "var(--paper-ink)",
              textAlign: "center",
              cursor: "pointer",
            }}
            onClick={() => inputRef.current?.click()}
          >
            <h3 style={{ fontFamily: "var(--font-display)", fontSize: 20, color: "var(--paper-ink)" }}>
              {upload.isPending ? "Uploading…" : "Drop a PDF or DOCX"}
            </h3>
            <div style={{ marginTop: 8, fontFamily: "var(--font-mono)", fontSize: 12, letterSpacing: ".08em", color: "var(--paper-ink-2)" }}>
              or click to browse · up to 10 MB
            </div>
            {uploadError && (
              <div style={{ marginTop: 16, color: "var(--signal-fail)", fontFamily: "var(--font-mono)", fontSize: 12 }}>{uploadError}</div>
            )}
          </div>
          <input
            ref={inputRef}
            type="file"
            accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            style={{ display: "none" }}
            onChange={(e) => submitFile(e.target.files?.[0] ?? null)}
          />
        </div>
      )}

      {isPending ? (
        <Card><div style={{ color: "var(--fg-3)" }}>Loading resumes…</div></Card>
      ) : isError ? (
        <ErrorState code="fetch.resumes" onRetry={() => void refetch()} />
      ) : data && data.content.length === 0 ? (
        <Card>
          <Empty
            title="No resumes on file yet."
            body="Upload a PDF or DOCX and we'll parse it, chunk it, and embed it into your workspace."
            action={<Button variant="primary" onClick={() => { setParams({ new: "1" }, { replace: true }); inputRef.current?.click(); }}>Upload resume →</Button>}
          />
        </Card>
      ) : (
        <div className="app-resume-grid" style={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: 24 }}>
          {data!.content.map((r) => (
            <Link key={r.id} to={`/resumes/${r.id}`} style={{ textDecoration: "none" }}>
              <Card kind="paper" interactive padding={20} style={{ height: 260, display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
                <div>
                  <div style={{ fontFamily: "var(--font-mono)", fontSize: 10.5, letterSpacing: ".16em", textTransform: "uppercase", color: "var(--paper-ink-2)" }}>
                    RESUME · #{r.id}
                  </div>
                  <h3 style={{ marginTop: 12, fontFamily: "var(--font-display)", fontSize: 20, lineHeight: 1.24, color: "var(--paper-ink)", overflow: "hidden", textOverflow: "ellipsis", display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical" }}>
                    {r.filename}
                  </h3>
                </div>
                <div>
                  <div style={{ display: "flex", gap: 6, flexWrap: "wrap", marginBottom: 12 }}>
                    <Badge kind="neutral">{r.contentType?.includes("pdf") ? "PDF" : "DOCX"}</Badge>
                    <Badge kind="neutral">{formatFileSize(r.fileSize)}</Badge>
                  </div>
                  <div style={{ fontFamily: "var(--font-mono)", fontSize: 11, letterSpacing: ".02em", color: "var(--paper-ink-2)" }}>
                    added {formatRelativeTime(r.createdAt)}
                  </div>
                  <div style={{ marginTop: 12, fontFamily: "var(--font-mono)", fontSize: 11, letterSpacing: ".14em", textTransform: "uppercase", color: "var(--paper-ink)" }}>
                    Open →
                  </div>
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}

      <style>{`
        @media (max-width: 1023px){
          .app-resume-grid { grid-template-columns: repeat(2, minmax(0, 1fr)) !important; }
        }
        @media (max-width: 640px){
          .app-resume-grid { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}
