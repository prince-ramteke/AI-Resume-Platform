import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { PageHeader } from "../../components/layout/PageHeader";
import {
  Button,
  EmptyState,
  ErrorState,
  Input,
  Skeleton,
  Pagination,
  ConfirmDialog,
} from "../../components/ui";
import { UploadIcon } from "../../components/layout/icons";
import { JobDescriptionListItem } from "../../components/jobDescriptions/JobDescriptionListItem";
import { JobDescriptionCreateDialog } from "../../components/jobDescriptions/JobDescriptionCreateDialog";
import { useJobDescriptions } from "../../hooks/useJobDescriptions";
import { useCreateJobDescriptionFromText } from "../../hooks/useCreateJobDescriptionFromText";
import { useCreateJobDescriptionFromFile } from "../../hooks/useCreateJobDescriptionFromFile";
import { useDeleteJobDescription } from "../../hooks/useDeleteJobDescription";
import { parseApiError } from "../../api/errors";
import type { JobDescriptionSummary } from "../../types/jobDescription";

const PAGE_SIZE = 10;

function ListSkeleton() {
  return (
    <div
      role="status"
      aria-label="Loading job descriptions"
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
 * The JD library: paginated list with search, create (text or file), and delete.
 * Counts and paging come from the Spring `Page` envelope, never `content.length`.
 * Both `?page` and `?q` live in the URL so search and paging are shareable and
 * back-button-friendly; local input state is only what the user has typed but
 * not yet submitted.
 */
export function JobDescriptionListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const pageParam = Number.parseInt(searchParams.get("page") ?? "1", 10);
  const page =
    Number.isFinite(pageParam) && pageParam > 0 ? pageParam - 1 : 0;
  const activeSearch = (searchParams.get("q") ?? "").trim();

  const [searchInput, setSearchInput] = useState(activeSearch);
  // Sync the input when the URL changes underneath us (e.g. back/forward).
  useEffect(() => {
    setSearchInput(activeSearch);
  }, [activeSearch]);

  const query = useJobDescriptions({
    page,
    size: PAGE_SIZE,
    sort: "createdAt,desc",
    ...(activeSearch ? { search: activeSearch } : {}),
  });

  const [createOpen, setCreateOpen] = useState(false);
  const createText = useCreateJobDescriptionFromText();
  const createFile = useCreateJobDescriptionFromFile();

  const [pendingDelete, setPendingDelete] =
    useState<JobDescriptionSummary | null>(null);
  const del = useDeleteJobDescription();

  function updateParams(next: { page?: number; q?: string | null }) {
    const params = new URLSearchParams(searchParams);
    if (next.page !== undefined) {
      if (next.page <= 0) params.delete("page");
      else params.set("page", String(next.page + 1));
    }
    if (next.q !== undefined) {
      if (next.q === null || next.q === "") params.delete("q");
      else params.set("q", next.q);
    }
    setSearchParams(params);
  }

  function goToPage(zeroBased: number) {
    updateParams({ page: zeroBased });
  }

  function handleSearchSubmit(e: FormEvent) {
    e.preventDefault();
    const trimmed = searchInput.trim();
    // Any search change also resets to page 1.
    updateParams({ q: trimmed || null, page: 0 });
  }

  function clearSearch() {
    setSearchInput("");
    updateParams({ q: null, page: 0 });
  }

  // If the current page emptied out (e.g. deleting the last row on page 2),
  // step back so the user doesn't land on a blank page.
  const isSuccess = query.isSuccess;
  const emptiedPage =
    isSuccess &&
    page > 0 &&
    query.data.content.length === 0 &&
    query.data.totalElements > 0;
  useEffect(() => {
    if (emptiedPage) updateParams({ page: page - 1 });
    // updateParams closes over searchParams intentionally; changing search
    // deps chases its own tail — this effect only fires on the emptied case.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [emptiedPage, page]);

  function openCreate() {
    createText.reset();
    createFile.reset();
    setCreateOpen(true);
  }

  function handleCreateText(input: { title: string; rawText: string }) {
    createText.mutate(input, {
      onSuccess: (jd) => {
        setCreateOpen(false);
        navigate(`/job-descriptions/${jd.id}`);
      },
    });
  }

  function handleCreateFile(input: { title: string; file: File }) {
    createFile.mutate(input, {
      onSuccess: (jd) => {
        setCreateOpen(false);
        navigate(`/job-descriptions/${jd.id}`);
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
  const jds = query.data?.content ?? [];

  const isCreating = createText.isPending || createFile.isPending;
  const createError = createText.isError
    ? parseApiError(createText.error).message
    : createFile.isError
      ? parseApiError(createFile.error).message
      : null;

  return (
    <div>
      <PageHeader
        title="Job Descriptions"
        description="Save the roles you want to analyze against your resumes."
        action={
          <Button
            leftIcon={<UploadIcon className="h-4 w-4" />}
            onClick={openCreate}
          >
            New Job Description
          </Button>
        }
      />

      <form
        role="search"
        aria-label="Search job descriptions by title"
        onSubmit={handleSearchSubmit}
        className="mb-6 flex flex-wrap items-end gap-2"
      >
        <div className="min-w-[16rem] flex-1">
          <Input
            label="Search"
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Filter by title"
            disabled={query.isPending}
            autoComplete="off"
          />
        </div>
        <div className="flex gap-2">
          <Button type="submit" variant="secondary" disabled={query.isPending}>
            Search
          </Button>
          {activeSearch && (
            <Button
              type="button"
              variant="ghost"
              onClick={clearSearch}
              disabled={query.isPending}
            >
              Clear
            </Button>
          )}
        </div>
      </form>

      {query.isPending ? (
        <div className="rounded-card border border-border bg-surface">
          <ListSkeleton />
        </div>
      ) : query.isError ? (
        <ErrorState
          title="Couldn't load your job descriptions"
          message="Something went wrong fetching your job description list."
          onRetry={() => query.refetch()}
        />
      ) : totalElements === 0 ? (
        activeSearch ? (
          <EmptyState
            title="No matches"
            description={`No job descriptions match "${activeSearch}". Try a different title or clear the search.`}
            action={
              <Button variant="secondary" onClick={clearSearch}>
                Clear search
              </Button>
            }
          />
        ) : (
          <EmptyState
            title="No job descriptions yet"
            description="Add your first job description (paste the text or upload a PDF, DOCX, or TXT) to score resumes against it."
            action={
              <Button
                leftIcon={<UploadIcon className="h-4 w-4" />}
                onClick={openCreate}
              >
                Add your first job description
              </Button>
            }
          />
        )
      ) : (
        <>
          <div
            className={`rounded-card border border-border bg-surface ${
              query.isFetching ? "opacity-60 transition-opacity" : ""
            }`}
          >
            <ul className="divide-y divide-border">
              {jds.map((jd) => (
                <li key={jd.id}>
                  <JobDescriptionListItem
                    jobDescription={jd}
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

      <JobDescriptionCreateDialog
        open={createOpen}
        isPending={isCreating}
        errorMessage={createError}
        onSubmitText={handleCreateText}
        onSubmitFile={handleCreateFile}
        onClose={() => {
          if (!isCreating) setCreateOpen(false);
        }}
      />

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Delete job description"
        description={
          pendingDelete
            ? `"${pendingDelete.title}" will be removed from your job descriptions. This can't be undone here.`
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
