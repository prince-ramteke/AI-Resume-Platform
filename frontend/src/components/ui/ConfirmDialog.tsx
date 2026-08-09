import type { ReactNode } from "react";
import { Modal } from "./Modal";
import { Button } from "./Button";
import { Alert } from "./Alert";

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  /** `danger` styles the confirm button for destructive actions (default). */
  tone?: "danger" | "primary";
  isLoading?: boolean;
  /** Shown as an inline error banner when a confirm attempt fails. */
  errorMessage?: string | null;
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * Generic confirm/cancel dialog for irreversible actions. The confirm button
 * carries the loading state and is disabled while pending to prevent double
 * submission; failures surface as an inline banner without closing the dialog.
 */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  tone = "danger",
  isLoading = false,
  errorMessage,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <Modal
      open={open}
      onClose={onCancel}
      title={title}
      description={description}
      footer={
        <>
          <Button variant="secondary" onClick={onCancel} disabled={isLoading}>
            {cancelLabel}
          </Button>
          <Button variant={tone} onClick={onConfirm} isLoading={isLoading}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      {errorMessage && <Alert tone="error">{errorMessage}</Alert>}
    </Modal>
  );
}
