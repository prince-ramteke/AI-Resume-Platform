import { useId } from "react";
import type { TextareaHTMLAttributes } from "react";
import { cn } from "../../lib/cn";

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string;
  error?: string;
  helperText?: string;
}

export function Textarea({
  label,
  error,
  helperText,
  id,
  className,
  ...props
}: TextareaProps) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  const describedBy = error
    ? `${fieldId}-error`
    : helperText
      ? `${fieldId}-help`
      : undefined;

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={fieldId} className="text-[13px] font-medium text-ink">
        {label}
      </label>
      <textarea
        id={fieldId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={cn(
          "min-h-30 rounded-control border bg-surface-sunken px-3 py-2 text-sm text-ink placeholder:text-muted",
          "transition-colors focus:bg-surface focus:outline-none focus:ring-2 focus:ring-accent",
          error ? "border-danger" : "border-border focus:border-accent",
          className
        )}
        {...props}
      />
      {error ? (
        <p id={`${fieldId}-error`} className="text-[13px] text-danger">
          {error}
        </p>
      ) : helperText ? (
        <p id={`${fieldId}-help`} className="text-[13px] text-muted">
          {helperText}
        </p>
      ) : null}
    </div>
  );
}
