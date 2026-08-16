import React from "react";

interface Props extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  selected?: boolean;
  tone?: "default" | "evidence";
}

export function Chip({ selected, tone = "default", className, children, ...rest }: Props) {
  return (
    <button
      type="button"
      className={`app-chip ${className ?? ""}`.trim()}
      data-selected={selected ? "true" : undefined}
      data-tone={tone === "evidence" ? "evidence" : undefined}
      aria-pressed={selected ? "true" : "false"}
      {...rest}
    >
      {children}
    </button>
  );
}
