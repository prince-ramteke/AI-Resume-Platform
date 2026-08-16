import React from "react";

type Kind = "data" | "paper";
type Tone = "raised" | "flat" | "stage";

interface Props extends React.HTMLAttributes<HTMLDivElement> {
  kind?: Kind;
  tone?: Tone;
  interactive?: boolean;
  padding?: number | string;
  children?: React.ReactNode;
}

export function Card({
  kind = "data",
  tone = "raised",
  interactive = false,
  padding,
  children,
  className,
  style,
  ...rest
}: Props) {
  const merged: React.CSSProperties = {
    ...(padding !== undefined ? { padding: typeof padding === "number" ? `${padding}px` : padding } : null),
    ...style,
  };
  return (
    <div
      className={`app-card ${className ?? ""}`.trim()}
      data-kind={kind}
      data-tone={tone === "raised" ? undefined : tone}
      data-interactive={interactive ? "true" : undefined}
      style={merged}
      {...rest}
    >
      {children}
    </div>
  );
}
