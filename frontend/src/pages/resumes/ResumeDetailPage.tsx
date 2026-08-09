import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { PageHeader } from "../../components/layout/PageHeader";
import {
  Alert,
  Button,
  Card,
  EmptyState,
  ErrorState,
  LinkButton,
  Skeleton,
  ConfirmDialog,
} from "../../components/ui";
import { ResumeIcon, DownloadIcon, UploadIcon, TrashIcon } from "../../components/layout/icons";
import { ResumeFileDialog } from "../../components/resumes/ResumeFileDialog";
import { useResume } from "../../hooks/useResume";
import { useReplaceResume } from "../../hooks/useReplaceResume";
import { useDeleteResume } from "../../hooks/useDeleteResume";
import { useDownloadResume } from "../../hooks/useDownloadResume";
import { parseApiError } from "../../api/errors";
import { formatFileSize } from "../../lib/formatFileSize";
import { fileKindLabel } from "../../lib/fileKind";

const BACK = { to: "/resumes", label: "Resumes" };

function absoluteDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function DetailSkeleton() {
  return (
    <Card>
      <Skeleton className="h-6 w-64" />
      <Skeleton className="mt-3 h-4 w-40" />
      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-4 w-32" />
        ))}
      </div>
    </Card>
  );
}

/**
 * Single-resume view: metadata, extracted-text preview, and the download /
 * replace / delete actions. A 404 (missing or not owned — enumeration defense)
 * renders a neutral not-found state, never a permissions error.
 */
export function ResumeDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const idNum = Number(idParam);
  const validId = Number.isInteger(idNum) && idNum > 0;

  const navigate = useNavigate();
  const query = useResume(idNum);

  const [replaceOpen, setReplaceOpen] = useState(false);
  const replace = useReplaceResume(idNum);

  const [deleteOpen, setDeleteOpen] = useState(false);
  const del = useDeleteResume();

  const download = useDownloadResume();

  // Invalid route param → same neutral not-found as a real 404.
  if (!validId) {
    return (
      <div>
        <PageHeader title="Resume" back={BACK} />
        <EmptyState
          title="Resume not found"
          description="This resume isn't available. It may have been deleted."
          action={<LinkButton to="/resumes">Back to resumes</LinkButton>}
        />
      </div>
    );
  }

  if (query.isPending) {
    return (
      <div>
        <PageHeader title="Resume" back={BACK} />
        <DetailSkeleton />
      </div>
    );
  }

  if (query.isError) {
    const parsed = parseApiError(query.error);
    if (parsed.status === 404) {
      return (
        <div>
          <PageHeader title="Resume" back={BACK} />
          <EmptyState
            title="Resume not found"
            description="This resume isn't available. It may have been deleted."
            action={<LinkButton to="/resumes">Back to resumes</LinkButton>}
          />
        </div>
      );
    }
    return (
      <div>
        <PageHeader title="Resume" back={BACK} />
        <ErrorState
          title="Couldn't load this resume"
          message={parsed.message}
          onRetry={() => query.refetch()}
        />
      </div>
    );
  }

  const resume = query.data;
  const kind = fileKindLabel(resume.contentType, resume.filename);

  function handleReplace(file: File) {
    replace.mutate(file, {
      onSuccess: () => setReplaceOpen(false),
    });
  }

  function confirmDelete() {
    del.mutate(idNum, {
      onSuccess: () => navigate("/resumes"),
    });
  }

  return (
    <div>
      <PageHeader title="Resume" back={BACK} />

      {replace.isSuccess && !replaceOpen && (
        <div className="mb-6">
          <Alert tone="success">Resume file replaced.</Alert>
        </div>
      )}

      <Card>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex min-w-0 items-start gap-3">
            <span className="mt-0.5 shrink-0 text-accent" aria-hidden="true">
              <ResumeIcon />
            </span>
            <div className="min-w-0">
              <h2
                className="break-words font-display text-xl text-ink"
                title={resume.filename}
              >
                {resume.filename}
              </h2>
              <p className="mt-0.5 text-sm text-muted">
                {kind} · {formatFileSize(resume.fileSize)}
              </p>
            </div>
          </div>

          <div className="flex shrink-0 flex-wrap items-center gap-2">
            <Button
              variant="secondary"
              size="sm"
              leftIcon={<DownloadIcon className="h-4 w-4" />}
              isLoading={download.isPending}
              onClick={() =>
                download.mutate({ id: resume.id, fallbackName: resume.filename })
              }
            >
              Download
            </Button>
            <Button
              variant="secondary"
              size="sm"
              leftIcon={<UploadIcon className="h-4 w-4" />}
              onClick={() => {
                replace.reset();
                setReplaceOpen(true);
              }}
            >
              Replace
            </Button>
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<TrashIcon className="h-4 w-4" />}
              className="text-danger hover:bg-danger-soft"
              onClick={() => {
                del.reset();
                setDeleteOpen(true);
              }}
            >
              Delete
            </Button>
          </div>
        </div>

        {download.isError && (
          <p role="alert" className="mt-3 text-sm text-danger">
            Couldn't download the file. Try again.
          </p>
        )}

        <dl className="mt-6 grid gap-4 border-t border-border pt-6 sm:grid-cols-2">
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-muted">
              Uploaded
            </dt>
            <dd className="mt-1 text-sm text-ink">{absoluteDate(resume.createdAt)}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-muted">
              Last replaced
            </dt>
            <dd className="mt-1 text-sm text-ink">
              {resume.updatedAt ? absoluteDate(resume.updatedAt) : "—"}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-muted">
              Pages
            </dt>
            <dd className="mt-1 text-sm text-ink">{resume.pageCount ?? "—"}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-muted">
              Language
            </dt>
            <dd className="mt-1 text-sm text-ink">{resume.language ?? "—"}</dd>
          </div>
        </dl>
      </Card>

      <section aria-labelledby="extracted-text-heading" className="mt-8">
        <h2
          id="extracted-text-heading"
          className="mb-3 font-display text-lg text-ink"
        >
          Extracted text
        </h2>
        <Card>
          {resume.rawText?.trim() ? (
            <div className="max-h-96 overflow-y-auto whitespace-pre-wrap break-words text-sm leading-relaxed text-ink">
              {resume.rawText}
            </div>
          ) : (
            <p className="text-sm text-muted">
              No text could be extracted from this file.
            </p>
          )}
        </Card>
      </section>

      <ResumeFileDialog
        open={replaceOpen}
        mode="replace"
        isPending={replace.isPending}
        errorMessage={
          replace.isError ? parseApiError(replace.error).message : null
        }
        onSubmit={handleReplace}
        onClose={() => {
          if (!replace.isPending) setReplaceOpen(false);
        }}
      />

      <ConfirmDialog
        open={deleteOpen}
        title="Delete resume"
        description={`"${resume.filename}" will be removed from your resumes. This can't be undone here.`}
        confirmLabel="Delete"
        isLoading={del.isPending}
        errorMessage={del.isError ? parseApiError(del.error).message : null}
        onConfirm={confirmDelete}
        onCancel={() => {
          if (del.isPending) return;
          del.reset();
          setDeleteOpen(false);
        }}
      />
    </div>
  );
}
