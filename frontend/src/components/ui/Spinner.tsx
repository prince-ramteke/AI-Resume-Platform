import { cn } from "../../lib/cn";

interface SpinnerProps {
  size?: "sm" | "md";
  className?: string;
  /** Accessible label; announced to screen readers. */
  label?: string;
}

export function Spinner({ size = "md", className, label = "Loading" }: SpinnerProps) {
  const dim = size === "sm" ? "h-4 w-4" : "h-6 w-6";
  return (
    <span role="status" className={cn("inline-flex", className)}>
      <svg
        className={cn("animate-spin text-current", dim)}
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
      >
        <circle
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          strokeOpacity="0.2"
          strokeWidth="3"
        />
        <path
          d="M22 12a10 10 0 0 0-10-10"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
        />
      </svg>
      <span className="sr-only">{label}</span>
    </span>
  );
}
