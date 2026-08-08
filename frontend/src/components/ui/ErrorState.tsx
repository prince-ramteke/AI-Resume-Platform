import { cn } from "../../lib/cn";
import { Button } from "./Button";

interface ErrorStateProps {
  title?: string;
  message: string;
  onRetry?: () => void;
  className?: string;
}

/**
 * Reusable failure surface for network/500/unexpected errors. Explains what
 * happened and offers a way forward, in the interface's voice — no apology.
 */
export function ErrorState({
  title = "Something went wrong",
  message,
  onRetry,
  className,
}: ErrorStateProps) {
  return (
    <div
      role="alert"
      className={cn(
        "rounded-card border border-border bg-surface px-6 py-6",
        className
      )}
    >
      <h3 className="font-semibold text-danger">{title}</h3>
      <p className="mt-1 text-sm text-muted">{message}</p>
      {onRetry && (
        <div className="mt-4">
          <Button variant="secondary" size="sm" onClick={onRetry}>
            Try again
          </Button>
        </div>
      )}
    </div>
  );
}
