import React from "react";

type Kind = "neutral" | "pass" | "warn" | "fail" | "info" | "evidence";

interface Props extends React.HTMLAttributes<HTMLSpanElement> {
  kind?: Kind;
  children?: React.ReactNode;
}

export function Badge({ kind = "neutral", className, children, ...rest }: Props) {
  return (
    <span
      className={`app-badge ${className ?? ""}`.trim()}
      data-kind={kind === "neutral" ? undefined : kind}
      {...rest}
    >
      {children}
    </span>
  );
}
