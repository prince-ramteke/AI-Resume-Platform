import React from "react";

export interface DataListItem {
  label: React.ReactNode;
  value: React.ReactNode;
}

interface Props {
  items: DataListItem[];
  className?: string;
}

export function DataList({ items, className }: Props) {
  return (
    <dl className={`app-datalist ${className ?? ""}`.trim()}>
      {items.map((it, i) => (
        <React.Fragment key={i}>
          <dt>{it.label}</dt>
          <dd>{it.value}</dd>
        </React.Fragment>
      ))}
    </dl>
  );
}
