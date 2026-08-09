import { useEffect, useId, useRef, useState } from "react";
import { Modal, Button, Alert, Input, Textarea } from "../ui";
import {
  JD_RAW_TEXT_MAX,
  JD_TITLE_MAX,
  validateJdFile,
  validateJdRawText,
  validateJdTitle,
} from "../../lib/validators";
import { formatFileSize } from "../../lib/formatFileSize";
import { cn } from "../../lib/cn";

type Mode = "text" | "file";

interface JobDescriptionCreateDialogProps {
  open: boolean;
  /** True while either create mutation is in flight. */
  isPending: boolean;
  /** Server-side error from the failed mutation (already normalized). */
  errorMessage?: string | null;
  onSubmitText: (input: { title: string; rawText: string }) => void;
  onSubmitFile: (input: { title: string; file: File }) => void;
  onClose: () => void;
}

/**
 * Two-mode create dialog: pasted text (POST /job-descriptions) or uploaded
 * file (POST /job-descriptions/upload). Owns only local form + client-side
 * validation state; the actual mutation, loading, and server error live in the
 * parent. Fully resets on (re)open so a prior draft never leaks across sessions.
 *
 * The two source modes are mutually exclusive at the endpoint level, so we
 * present them as a segmented tab strip and only submit one at a time.
 */
export function JobDescriptionCreateDialog({
  open,
  isPending,
  errorMessage,
  onSubmitText,
  onSubmitFile,
  onClose,
}: JobDescriptionCreateDialogProps) {
  const [mode, setMode] = useState<Mode>("text");
  const [title, setTitle] = useState("");
  const [rawText, setRawText] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [submitted, setSubmitted] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const fileInputId = useId();
  const fileErrorId = useId();

  useEffect(() => {
    if (open) {
      setMode("text");
      setTitle("");
      setRawText("");
      setFile(null);
      setSubmitted(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  }, [open]);

  const titleError = validateJdTitle(title);
  const rawTextError = mode === "text" ? validateJdRawText(rawText) : undefined;
  const fileError = mode === "file" ? validateJdFile(file) : undefined;

  const showTitleError = submitted && titleError ? titleError : undefined;
  const showRawTextError = submitted && rawTextError ? rawTextError : undefined;
  const showFileError = submitted && fileError ? fileError : undefined;

  function switchMode(next: Mode) {
    if (isPending || next === mode) return;
    setMode(next);
    setSubmitted(false);
  }

  function handleSubmit() {
    setSubmitted(true);
    if (titleError) return;
    if (mode === "text") {
      if (rawTextError) return;
      onSubmitText({ title: title.trim(), rawText });
    } else {
      if (fileError || !file) return;
      onSubmitFile({ title: title.trim(), file });
    }
  }

  const canSubmit =
    !titleError &&
    (mode === "text" ? !rawTextError : !fileError && file !== null);

  return (
    <Modal
      open={open}
      onClose={isPending ? () => {} : onClose}
      title="New job description"
      description="Paste the description as text, or upload a PDF, DOCX, or TXT (up to 10 MB)."
      className="max-w-xl"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isPending}>
            Cancel
          </Button>
          <Button
            onClick={handleSubmit}
            isLoading={isPending}
            disabled={!canSubmit}
          >
            {mode === "text" ? "Create" : "Upload"}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div
          role="tablist"
          aria-label="Job description source"
          className="inline-flex self-start rounded-control border border-border bg-surface-sunken p-0.5"
        >
          <TabButton
            selected={mode === "text"}
            onClick={() => switchMode("text")}
            disabled={isPending}
          >
            Paste text
          </TabButton>
          <TabButton
            selected={mode === "file"}
            onClick={() => switchMode("file")}
            disabled={isPending}
          >
            Upload file
          </TabButton>
        </div>

        <Input
          label="Title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={JD_TITLE_MAX}
          disabled={isPending}
          placeholder="e.g. Java Backend Engineer"
          error={showTitleError}
          autoComplete="off"
        />

        {mode === "text" ? (
          <div className="flex flex-col gap-1.5">
            <Textarea
              label="Job description text"
              value={rawText}
              onChange={(e) => setRawText(e.target.value)}
              rows={10}
              maxLength={JD_RAW_TEXT_MAX}
              disabled={isPending}
              placeholder="Paste the full role description here…"
              error={showRawTextError}
            />
            {/*
              A11y: no aria-live here — announcing a character count on every
              keystroke floods screen readers. The count is visual feedback for
              sighted users; the field's own maxLength enforces the ceiling
              server-authoritatively via the validator.
            */}
            <p className="self-end text-[12px] text-muted">
              {rawText.length.toLocaleString()} /{" "}
              {JD_RAW_TEXT_MAX.toLocaleString()}
            </p>
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            <label
              htmlFor={fileInputId}
              className="text-[13px] font-medium text-ink"
            >
              Job description file
            </label>
            <input
              ref={fileInputRef}
              id={fileInputId}
              type="file"
              accept=".pdf,.docx,.txt,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain"
              disabled={isPending}
              aria-invalid={showFileError ? true : undefined}
              aria-describedby={showFileError ? fileErrorId : undefined}
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              className="block w-full rounded-control border border-border bg-surface-sunken text-sm text-ink file:mr-3 file:cursor-pointer file:border-0 file:bg-surface file:px-4 file:py-2 file:text-sm file:font-medium file:text-ink hover:file:bg-border/60 focus:outline-none focus:ring-2 focus:ring-accent"
            />
            {file && !showFileError && (
              <p className="text-[13px] text-muted">
                Selected: {file.name} ({formatFileSize(file.size)})
              </p>
            )}
            {showFileError && (
              <div id={fileErrorId}>
                <Alert tone="error">{showFileError}</Alert>
              </div>
            )}
          </div>
        )}

        {errorMessage && <Alert tone="error">{errorMessage}</Alert>}
      </div>
    </Modal>
  );
}

interface TabButtonProps {
  selected: boolean;
  disabled?: boolean;
  onClick: () => void;
  children: React.ReactNode;
}

function TabButton({ selected, disabled, onClick, children }: TabButtonProps) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={selected}
      disabled={disabled}
      onClick={onClick}
      className={cn(
        "min-w-[7rem] rounded-[calc(theme(borderRadius.control)-2px)] px-3 py-1.5 text-[13px] font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-accent",
        selected
          ? "bg-surface text-ink shadow-sm"
          : "text-muted hover:text-ink",
        disabled && "cursor-not-allowed opacity-60"
      )}
    >
      {children}
    </button>
  );
}
