import apiClient from "./client";
import type { Page } from "../types";
import type {
  JobDescriptionSummary,
  JobDescriptionDetail,
} from "../types/jobDescription";

/**
 * GET /api/job-descriptions — the caller's job descriptions, paginated. The
 * dashboard reads `totalElements` for the count. `search` is optional and
 * server-side matches title case-insensitively.
 */
export async function listJobDescriptions(params: {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
}): Promise<Page<JobDescriptionSummary>> {
  const { data } = await apiClient.get<Page<JobDescriptionSummary>>(
    "/job-descriptions",
    { params }
  );
  return data;
}

/** GET /api/job-descriptions/{id} — full JD detail (includes rawText + metadata). */
export async function getJobDescription(
  id: number
): Promise<JobDescriptionDetail> {
  const { data } = await apiClient.get<JobDescriptionDetail>(
    `/job-descriptions/${id}`
  );
  return data;
}

/**
 * POST /api/job-descriptions — create from pasted text (JSON body). Returns the
 * new JD's full detail so the caller can navigate directly to it.
 */
export async function createJobDescriptionFromText(input: {
  title: string;
  rawText: string;
}): Promise<JobDescriptionDetail> {
  const { data } = await apiClient.post<JobDescriptionDetail>(
    "/job-descriptions",
    input
  );
  return data;
}

/**
 * POST /api/job-descriptions/upload — create from uploaded file (multipart,
 * PDF/DOCX/TXT). Backend re-validates type/size/magic-bytes and extracts text.
 */
export async function createJobDescriptionFromFile(input: {
  title: string;
  file: File;
}): Promise<JobDescriptionDetail> {
  const form = new FormData();
  form.append("file", input.file);
  form.append("title", input.title);
  const { data } = await apiClient.post<JobDescriptionDetail>(
    "/job-descriptions/upload",
    form,
    // Override the instance's default JSON so axios doesn't serialize the
    // FormData; the browser fills in the multipart boundary at send time.
    { headers: { "Content-Type": "multipart/form-data" } }
  );
  return data;
}

/**
 * PUT /api/job-descriptions/{id} — update title + rawText (JSON body). Applies
 * to both text-paste and file-based JDs (only the editable fields change; the
 * uploaded file is left untouched).
 */
export async function updateJobDescription(
  id: number,
  input: { title: string; rawText: string }
): Promise<JobDescriptionDetail> {
  const { data } = await apiClient.put<JobDescriptionDetail>(
    `/job-descriptions/${id}`,
    input
  );
  return data;
}

/** DELETE /api/job-descriptions/{id} — soft-delete. Returns 204 (no body). */
export async function deleteJobDescription(id: number): Promise<void> {
  await apiClient.delete(`/job-descriptions/${id}`);
}

/** A downloaded file: raw bytes plus the server-provided filename (if any). */
export interface JobDescriptionDownload {
  blob: Blob;
  /** From `Content-Disposition`; null if the header wasn't exposed. */
  filename: string | null;
}

/**
 * GET /api/job-descriptions/{id}/download — fetch the original uploaded file as
 * a Blob. Only file-based JDs have a file; text-paste JDs return 404 by design.
 * Triggering the browser save is the caller's job (see lib/saveBlob).
 */
export async function downloadJobDescription(
  id: number
): Promise<JobDescriptionDownload> {
  const response = await apiClient.get(`/job-descriptions/${id}/download`, {
    responseType: "blob",
  });
  return {
    blob: response.data as Blob,
    filename: filenameFromContentDisposition(
      response.headers["content-disposition"]
    ),
  };
}

/** Extract `filename="…"` from a Content-Disposition header value. */
function filenameFromContentDisposition(
  header: string | undefined
): string | null {
  if (!header) return null;
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(header);
  return match && match[1] ? decodeURIComponent(match[1]) : null;
}
