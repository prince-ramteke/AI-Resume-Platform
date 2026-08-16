import React from "react";

interface Props {
  children: React.ReactNode;
  className?: string;
  as?: "span" | "div";
}

export function Eyebrow({ children, className, as = "span" }: Props) {
  const Tag = as;
  return <Tag className={`app-eyebrow ${className ?? ""}`.trim()}>{children}</Tag>;
}
