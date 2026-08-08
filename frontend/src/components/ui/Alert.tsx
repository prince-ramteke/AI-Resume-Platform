import type { ReactNode } from "react";
import { cn } from "../../lib/cn";

type Tone = "error" | "success" | "info" | "warning";

interface AlertProps {
  tone?: Tone;
  title?: ReactNode;
  children?: ReactNode;
  className?: string;
}

const tones: Record<Tone, string> = {
  error: "border-danger/30 bg-danger-soft text-danger",
  success: "border-success/30 bg-success-soft text-success",
  info: "border-accent/30 bg-accent-soft text-accent",
  warning: "border-warning/30 bg-warning-soft text-warning",
};

/**
 * Inline banner. Used as the form-level error banner for server 400s (the
 * backend returns one joined `message` string). Errors announce themselves.
 */
export function Alert({ tone = "info", title, children, className }: AlertProps) {
  return (
    <div
      role={tone === "error" ? "alert" : "status"}
      className={cn(
        "rounded-control border px-4 py-3 text-sm",
        tones[tone],
        className
      )}
    >
      {title && <p className="font-semibold">{title}</p>}
      {children && <div className={cn(Boolean(title) && "mt-0.5")}>{children}</div>}
    </div>
  );
}
