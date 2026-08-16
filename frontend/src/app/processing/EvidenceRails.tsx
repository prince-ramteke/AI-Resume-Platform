import type { RailEndpoint } from "./useEvidenceGeometry";

interface Props {
  rails: RailEndpoint[];
  visible?: boolean;
}

/**
 * L6 — SVG cubic-bezier curves from highlight midpoints on the paper toward
 * pipeline tiles above. Cyan stroke with dashed flow animation. Only visible
 * from the Retrieval stage onward.
 */
export function EvidenceRails({ rails, visible = false }: Props) {
  if (!visible || rails.length === 0) return null;

  return (
    <svg
      className="theatre-rails"
      aria-hidden="true"
      style={{
        position: "absolute",
        inset: 0,
        width: "100%",
        height: "100%",
        pointerEvents: "none",
        overflow: "visible",
      }}
    >
      {rails.map((r, i) => {
        const cpY = r.startY - (r.startY - r.endY) * 0.4;
        const d = `M ${r.startX} ${r.startY} C ${r.startX} ${cpY}, ${r.endX} ${cpY}, ${r.endX} ${r.endY}`;
        return (
          <path
            key={i}
            d={d}
            className="theatre-rail-path"
            style={{ animationDelay: `${i * 200}ms` }}
          />
        );
      })}
    </svg>
  );
}
