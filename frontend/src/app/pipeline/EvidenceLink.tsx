interface Props {
  from: { x: number; y: number };
  to: { x: number; y: number };
  active?: boolean;
  dim?: boolean;
}

/**
 * A single amber bezier connector between two coordinates. Rendered inside a
 * parent <svg> overlay by the AnalysisResultPage. Bézier control points sit
 * on the horizontal midline so the curve reads as a soft arc.
 */
export function EvidenceLink({ from, to, active, dim }: Props) {
  const midX = (from.x + to.x) / 2;
  const d = `M ${from.x} ${from.y} C ${midX} ${from.y}, ${midX} ${to.y}, ${to.x} ${to.y}`;
  return <path className="app-evidence-link" d={d} data-active={active ? "true" : undefined} data-dim={dim ? "true" : undefined} />;
}
