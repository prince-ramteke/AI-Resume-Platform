import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { PageHeader } from "../../components/layout/PageHeader";
import {
  Button,
  EmptyState,
  ErrorState,
  Skeleton,
  Pagination,
  ConfirmDialog,
} from "../../components/ui";
import { UploadIcon } from "../../components/layout/icons";
import { ResumeListItem } from "../../components/resumes/ResumeListItem";
import { ResumeFileDialog } from "../../components/resumes/ResumeFileDialog";
import { useResumes } from "../../hooks/useResumes";
import { useUploadResume } from "../../hooks/useUploadResume";
import { useDeleteResume } from "../../hooks/useDeleteResume";
import { parseApiError } from "../../api/errors";
import type { ResumeSummary } from "../../types/resume";

const PAGE_SIZE = 10;

function ListSkeleton() {
  return (
    <div
      role="status"
      aria-label="Loading resumes"
      className="divide-y divide-border"
    >
      {[0, 1, 2, 3].map((i) => (
        <div key={i} className="flex items-center gap-3 px-4 py-4">
          <Skeleton className="h-5 w-5 rounded-full" />
          <div className="flex-1">
            <Skeleton className="h-4 w-56" />
            <Skeleton className="mt-2 h-3 w-40" />
          </div>
          <Skeleton className="h-8 w-20" />
        </div>
      ))}
    </div>
  );
}

/**
 * The resume library: paginated list with upload, download (per row), and
 * delete. Counts and paging come from the Spring `Page` envelope, never
 * `content.length`. Every async branch (loading / error / empty / list) is
 * handled explicitly, and paging keeps the previous page on screen so it never
 * flashes a skeleton.
 */
export function ResumeListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const pageParam = Number.parseInt(searchParams.get("page") ?? "1", 10);
  const page =
    Number.isFinite(pageParam) && pageParam > 0 ? pageParam - 1 : 0;

  const query = useResumes({
    page,
    size: PAGE_SIZE,
    sort: "createdAt,desc",
  });

  const [uploadOpen, setUploadOpen] = useState(false);
  const upload = useUploadResume();

  const [pendingDelete, setPendingDelete] = useState<ResumeSummary | null>(null);
  const del = useDeleteResume();

  function goToPage(zeroBased: number) {
    setSearchParams({ page: String(zeroBased + 1) });
  }

  // If the current page emptied out (e.g. deleting the last row on page 2),
  // step back so the user doesn't land on a blank page. `setSearchParams` is
  // stable, so the deps stay honest without an exhaustive-deps override.
  const isSuccess = query.isSuccess;
  const emptiedPage =
    isSuccess &&
    page > 0 &&
    query.data.content.length === 0 &&
    query.data.totalElements > 0;
  useEffect(() => {
    if (emptiedPage) {
      setSearchParams({ page: String(page) });
    }
  }, [emptiedPage, page, setSearchParams]);

  function openUpload() {
    upload.reset();
    setUploadOpen(true);
  }

  function handleUpload(file: File) {
    upload.mutate(file, {
      onSuccess: (result) => {
        setUploadOpen(false);
        navigate(`/resumes/${result.id}`);
      },
    });
  }

  function confirmDelete() {
    if (!pendingDelete) return;
    del.mutate(pendingDelete.id, {
      onSuccess: () => setPendingDelete(null),
    });
  }

  function cancelDelete() {
    if (del.isPending) return;
    del.reset();
    setPendingDelete(null);
  }

  const totalElements = query.data?.totalElements ?? 0;
  const totalPages = query.data?.totalPages ?? 0;
  const resumes = query.data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="Resumes"
        description="Upload and manage the resumes you analyze against job descriptions."
        action={
          <Button
            leftIcon={<UploadIcon className="h-4 w-4" />}
            onClick={openUpload}
          >
            Upload Resume
          </Button>
        }
      />

      {query.isPending ? (
        <div className="rounded-card border border-border bg-surface">
          <ListSkeleton />
        </div>
      ) : query.isError ? (
        <ErrorState
          title="Couldn't load your resumes"
          message="Something went wrong fetching your resume list."
          onRetry={() => query.refetch()}
        />
      ) : totalElements === 0 ? (
        <EmptyState
          title="No resumes yet"
          description="Upload your first resume (PDF or DOCX) to start scoring it against roles."
          action={
            <Button
              leftIcon={<UploadIcon className="h-4 w-4" />}
              onClick={openUpload}
            >
              Upload your first resume
            </Button>
          }
        />
      ) : (
        <>
          <div
            className={`rounded-card border border-border bg-surface ${
              query.isFetching ? "opacity-60 transition-opacity" : ""
            }`}
          >
            <ul className="divide-y divide-border">
              {resumes.map((resume) => (
                <li key={resume.id}>
                  <ResumeListItem
                    resume={resume}
                    onRequestDelete={setPendingDelete}
                  />
                </li>
              ))}
            </ul>
          </div>
          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={PAGE_SIZE}
            onPageChange={goToPage}
            isFetching={query.isFetching}
          />
        </>
      )}

      <ResumeFileDialog
        open={uploadOpen}
        mode="upload"
        isPending={upload.isPending}
        errorMessage={upload.isError ? parseApiError(upload.error).message : null}
        onSubmit={handleUpload}
        onClose={() => {
          if (!upload.isPending) setUploadOpen(false);
        }}
      />

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Delete resume"
        description={
          pendingDelete
            ? `"${pendingDelete.filename}" will be removed from your resumes. This can't be undone here.`
            : undefined
        }
        confirmLabel="Delete"
        isLoading={del.isPending}
        errorMessage={del.isError ? parseApiError(del.error).message : null}
        onConfirm={confirmDelete}
        onCancel={cancelDelete}
      />
    </div>
  );
}
