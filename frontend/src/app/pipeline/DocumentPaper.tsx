import React from "react";
import { ScanBeam } from "./ScanBeam";

interface Props {
  title?: string;
  width?: number;
  lines?: number;
  highlights?: number[];
  scanning?: boolean;
  children?: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}

/**
 * The 2.5D resume paper — perspective(1200px) rotateX(4deg) rotateZ(-.4deg)
 * on a warm-paper sheet. Content is optional: passing children replaces the
 * default fake resume lines. Set `scanning` to overlay a persistent scan beam.
 */
export function DocumentPaper({
  title = "RESUME.PDF",
  width = 320,
  lines = 14,
  highlights = [],
  scanning = false,
  children,
  className,
  style,
}: Props) {
  const rows = Array.from({ length: lines }, (_, i) => i);
  const widths = [96, 88, 72, 92, 60, 84, 78, 66, 90, 52, 86, 74, 94, 68, 82, 58, 90, 70, 88, 62];
  return (
    <div className={`app-doc ${className ?? ""}`.trim()} style={{ width, ...style }}>
      <div className="app-doc__sheet">
        <div className="app-doc__title">{title}</div>
        {children ??
          rows.map((i) => (
            <div
              key={i}
              className="app-doc__line"
              data-hl={highlights.includes(i) ? "true" : undefined}
              style={{ width: `${widths[i % widths.length]}%` }}
            />
          ))}
        {scanning && <ScanBeam mode="vertical" />}
      </div>
    </div>
  );
}
