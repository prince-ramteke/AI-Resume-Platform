import React from "react";
import { Eyebrow } from "./Eyebrow";

interface Props {
  eyebrow?: string;
  title: React.ReactNode;
  meta?: React.ReactNode;
  actions?: React.ReactNode;
}

export function PageHeader({ eyebrow, title, meta, actions }: Props) {
  return (
    <header className="app-page-header">
      <div style={{ minWidth: 0, flex: 1 }}>
        {eyebrow && <div className="app-page-header__eyebrow"><Eyebrow>{eyebrow}</Eyebrow></div>}
        <h1 className="app-page-header__title">{title}</h1>
        {meta && <div className="app-page-header__meta">{meta}</div>}
      </div>
      {actions && <div style={{ display: "flex", gap: 12, alignItems: "center" }}>{actions}</div>}
    </header>
  );
}
