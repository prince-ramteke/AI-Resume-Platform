import { useEffect, useId, useRef, useState } from "react";
import { Modal, Button, Alert } from "../ui";
import { validateResumeFile } from "../../lib/validators";
import { formatFileSize } from "../../lib/formatFileSize";

interface ResumeFileDialogProps {
  open: boolean;
  mode: "upload" | "replace";
  /** Mutation in flight — disables inputs and shows the button spinner. */
  isPending: boolean;
  /** Server-side error from the failed mutation (already normalized). */
  errorMessage?: string | null;
  onSubmit: (file: File) => void;
  onClose: () => void;
}

const COPY = {
  upload: {
    title: "Upload a resume",
    description: "PDF or DOCX, up to 10 MB. We'll extract the text for analysis.",
    submit: "Upload",
  },
  replace: {
    title: "Replace this resume",
    description:
      "Upload a new PDF or DOCX (up to 10 MB) to replace the current file. The existing resume stays until the new one is accepted.",
    submit: "Replace file",
  },
} as const;

/**
 * File-selection dialog shared by the upload (create) and replace flows. Owns
 * only local selection + client-side validation state; the actual mutation,
 * loading, and server error live in the parent (passed back as props). Resets
 * whenever it (re)opens so a prior selection never leaks across sessions.
 */
export function ResumeFileDialog({
  open,
  mode,
  isPending,
  errorMessage,
  onSubmit,
  onClose,
}: ResumeFileDialogProps) {
  const copy = COPY[mode];
  const inputRef = useRef<HTMLInputElement>(null);
  const inputId = useId();
  const errorId = useId();
  const [file, setFile] = useState<File | null>(null);
  const [clientError, setClientError] = useState<string | undefined>(undefined);

  // Reset on open so a reopened dialog starts clean.
  useEffect(() => {
    if (open) {
      setFile(null);
      setClientError(undefined);
      if (inputRef.current) inputRef.current.value = "";
    }
  }, [open]);

  function handleFileChange(selected: File | null) {
    setFile(selected);
    setClientError(validateResumeFile(selected));
  }

  function handleSubmit() {
    const validationError = validateResumeFile(file);
    if (validationError || !file) {
      setClientError(validationError ?? "Choose a PDF or DOCX file.");
      return;
    }
    onSubmit(file);
  }

  const shownError = clientError ?? errorMessage ?? null;

  return (
    <Modal
      open={open}
      onClose={isPending ? () => {} : onClose}
      title={copy.title}
      description={copy.description}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isPending}>
            Cancel
          </Button>
          <Button
            onClick={handleSubmit}
            isLoading={isPending}
            disabled={!file || Boolean(clientError)}
          >
            {copy.submit}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-2">
        <label htmlFor={inputId} className="text-[13px] font-medium text-ink">
          Resume file
        </label>
        <input
          ref={inputRef}
          id={inputId}
          type="file"
          accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          disabled={isPending}
          aria-invalid={shownError ? true : undefined}
          aria-describedby={shownError ? errorId : undefined}
          onChange={(e) => handleFileChange(e.target.files?.[0] ?? null)}
          className="block w-full rounded-control border border-border bg-surface-sunken text-sm text-ink file:mr-3 file:cursor-pointer file:border-0 file:bg-surface file:px-4 file:py-2 file:text-sm file:font-medium file:text-ink hover:file:bg-border/60 focus:outline-none focus:ring-2 focus:ring-accent"
        />
        {file && !clientError && (
          <p className="text-[13px] text-muted">
            Selected: {file.name} ({formatFileSize(file.size)})
          </p>
        )}
        {shownError && (
          <div id={errorId}>
            <Alert tone="error">{shownError}</Alert>
          </div>
        )}
      </div>
    </Modal>
  );
}
