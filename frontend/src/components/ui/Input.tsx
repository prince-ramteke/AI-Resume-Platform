import { useId } from "react";
import type { InputHTMLAttributes } from "react";
import { cn } from "../../lib/cn";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  helperText?: string;
}

export function Input({
  label,
  error,
  helperText,
  id,
  className,
  ...props
}: InputProps) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const describedBy = error
    ? `${inputId}-error`
    : helperText
      ? `${inputId}-help`
      : undefined;

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={inputId} className="text-[13px] font-medium text-ink">
        {label}
      </label>
      <input
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={cn(
          "h-10 rounded-control border bg-surface-sunken px-3 text-sm text-ink placeholder:text-muted",
          "transition-colors focus:bg-surface focus:outline-none focus:ring-2 focus:ring-accent",
          error ? "border-danger" : "border-border focus:border-accent",
          className
        )}
        {...props}
      />
      {error ? (
        <p id={`${inputId}-error`} className="text-[13px] text-danger">
          {error}
        </p>
      ) : helperText ? (
        <p id={`${inputId}-help`} className="text-[13px] text-muted">
          {helperText}
        </p>
      ) : null}
    </div>
  );
}
