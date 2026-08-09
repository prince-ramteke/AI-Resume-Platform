import apiClient from "./client";
import type { Page } from "../types";
import type {
  ResumeSummary,
  ResumeDetail,
  ResumeUploadResult,
} from "../types/resume";

/**
 * GET /api/resumes — the caller's resumes, paginated. Spring `Page` carries
 * `totalElements`, which the dashboard uses for the resume count.
 */
export async function listResumes(params: {
  page?: number;
  size?: number;
  sort?: string;
}): Promise<Page<ResumeSummary>> {
  const { data } = await apiClient.get<Page<ResumeSummary>>("/resumes", {
    params,
  });
  return data;
}

/** GET /api/resumes/{id} — full resume detail (includes extracted text + metadata). */
export async function getResume(id: number): Promise<ResumeDetail> {
  const { data } = await apiClient.get<ResumeDetail>(`/resumes/${id}`);
  return data;
}

/**
 * POST /api/resumes — upload a new resume (multipart, field `file`). The backend
 * validates type/size/magic-bytes and extracts text; returns the new resume's id.
 */
export async function uploadResume(file: File): Promise<ResumeUploadResult> {
  const form = new FormData();
  form.append("file", file);
  const { data } = await apiClient.post<ResumeUploadResult>("/resumes", form, {
    // Override the instance's default application/json. Without this, axios's
    // transformRequest serializes the FormData to JSON and the file is lost;
    // the browser fills in the real multipart boundary at send time.
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

/**
 * PUT /api/resumes/{id} — replace an existing resume's file (multipart, field
 * `file`). Re-extracts text server-side. Returns the updated summary.
 */
export async function replaceResume(
  id: number,
  file: File
): Promise<ResumeUploadResult> {
  const form = new FormData();
  form.append("file", file);
  const { data } = await apiClient.put<ResumeUploadResult>(
    `/resumes/${id}`,
    form,
    // See uploadResume: force multipart so the FormData isn't JSON-serialized.
    { headers: { "Content-Type": "multipart/form-data" } }
  );
  return data;
}

/** DELETE /api/resumes/{id} — soft-delete. Returns 204 (no body). */
export async function deleteResume(id: number): Promise<void> {
  await apiClient.delete(`/resumes/${id}`);
}

/** A downloaded file: the raw bytes plus the server-provided filename (if any). */
export interface ResumeDownload {
  blob: Blob;
  /** From `Content-Disposition`; null if the header wasn't exposed. */
  filename: string | null;
}

/**
 * GET /api/resumes/{id}/download — fetch the original file as a Blob. The
 * response is binary (not JSON), so we request `responseType: "blob"` and parse
 * the filename out of `Content-Disposition`. Triggering the browser save is the
 * caller's job (see lib/saveBlob) so this stays a pure data fetch.
 */
export async function downloadResume(id: number): Promise<ResumeDownload> {
  const response = await apiClient.get(`/resumes/${id}/download`, {
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
