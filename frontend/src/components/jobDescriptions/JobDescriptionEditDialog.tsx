import { useEffect, useState } from "react";
import { Modal, Button, Alert, Input, Textarea } from "../ui";
import {
  JD_RAW_TEXT_MAX,
  JD_TITLE_MAX,
  validateJdRawText,
  validateJdTitle,
} from "../../lib/validators";
import type { JobDescriptionDetail } from "../../types/jobDescription";

interface JobDescriptionEditDialogProps {
  open: boolean;
  /**
   * The JD being edited. Values seed the form on open. Kept optional so the
   * parent can pass `null` while there's no active edit (the dialog just stays
   * closed) without conditionally rendering.
   */
  jobDescription: JobDescriptionDetail | null;
  isPending: boolean;
  /** Server-side error from the failed mutation (already normalized). */
  errorMessage?: string | null;
  onSubmit: (input: { title: string; rawText: string }) => void;
  onClose: () => void;
}

/**
 * Edit dialog for `PUT /job-descriptions/{id}`. Applies to both text-paste and
 * file-based JDs — only the editable fields (title + rawText) are surfaced.
 * The current values seed the form on open; the submit button is disabled
 * until at least one field actually changes so we don't send noise PUTs.
 */
export function JobDescriptionEditDialog({
  open,
  jobDescription,
  isPending,
  errorMessage,
  onSubmit,
  onClose,
}: JobDescriptionEditDialogProps) {
  const [title, setTitle] = useState("");
  const [rawText, setRawText] = useState("");
  const [submitted, setSubmitted] = useState(false);

  useEffect(() => {
    if (open && jobDescription) {
      setTitle(jobDescription.title);
      setRawText(jobDescription.rawText);
      setSubmitted(false);
    }
  }, [open, jobDescription]);

  const titleError = validateJdTitle(title);
  const rawTextError = validateJdRawText(rawText);

  const showTitleError = submitted && titleError ? titleError : undefined;
  const showRawTextError = submitted && rawTextError ? rawTextError : undefined;

  const isDirty =
    jobDescription !== null &&
    (title !== jobDescription.title || rawText !== jobDescription.rawText);

  function handleSubmit() {
    setSubmitted(true);
    if (titleError || rawTextError) return;
    if (!isDirty) return;
    onSubmit({ title: title.trim(), rawText });
  }

  return (
    <Modal
      open={open}
      onClose={isPending ? () => {} : onClose}
      title="Edit job description"
      description="Update the title or the description text. The original uploaded file (if any) isn't affected."
      className="max-w-xl"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isPending}>
            Cancel
          </Button>
          <Button
            onClick={handleSubmit}
            isLoading={isPending}
            disabled={!isDirty || Boolean(titleError) || Boolean(rawTextError)}
          >
            Save changes
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <Input
          label="Title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={JD_TITLE_MAX}
          disabled={isPending}
          error={showTitleError}
          autoComplete="off"
        />
        <div className="flex flex-col gap-1.5">
          <Textarea
            label="Job description text"
            value={rawText}
            onChange={(e) => setRawText(e.target.value)}
            rows={12}
            maxLength={JD_RAW_TEXT_MAX}
            disabled={isPending}
            error={showRawTextError}
          />
          <p className="self-end text-[12px] text-muted" aria-live="polite">
            {rawText.length.toLocaleString()} /{" "}
            {JD_RAW_TEXT_MAX.toLocaleString()}
          </p>
        </div>
        {errorMessage && <Alert tone="error">{errorMessage}</Alert>}
      </div>
    </Modal>
  );
}
