import type { ReactNode } from "react";
import { cn } from "../../lib/cn";

type Tone = "neutral" | "accent" | "success" | "warning" | "danger";

interface BadgeProps {
  children: ReactNode;
  tone?: Tone;
  /** Renders the label in the mono/data face (e.g. RESUME#2, HIGH). */
  mono?: boolean;
  /** Outlined instead of soft-filled — used for "missing" (opportunity, not error). */
  outline?: boolean;
  className?: string;
}

const soft: Record<Tone, string> = {
  neutral: "bg-surface-sunken text-muted",
  accent: "bg-accent-soft text-accent",
  success: "bg-success-soft text-success",
  warning: "bg-warning-soft text-warning",
  danger: "bg-danger-soft text-danger",
};

const outlined: Record<Tone, string> = {
  neutral: "border border-border text-muted",
  accent: "border border-accent/40 text-accent",
  success: "border border-success/40 text-success",
  warning: "border border-warning/40 text-warning",
  danger: "border border-danger/40 text-danger",
};

export function Badge({
  children,
  tone = "neutral",
  mono = false,
  outline = false,
  className,
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-pill px-2.5 py-0.5 text-xs font-medium",
        outline ? outlined[tone] : soft[tone],
        mono && "font-mono tracking-tight",
        className
      )}
    >
      {children}
    </span>
  );
}
