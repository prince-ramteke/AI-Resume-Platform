import { useEffect, useRef, useState } from "react";

export interface RailEndpoint {
  startX: number;
  startY: number;
  endX: number;
  endY: number;
}

/**
 * Computes SVG cubic-bezier rail endpoints from highlight midpoints on the
 * paper to pipeline tiles above. Uses ResizeObserver to recompute only on
 * stage change or resize.
 */
export function useEvidenceGeometry(
  theatreRef: React.RefObject<HTMLDivElement | null>,
  highlightCount: number,
  visible: boolean,
): RailEndpoint[] {
  const [rails, setRails] = useState<RailEndpoint[]>([]);
  const roRef = useRef<ResizeObserver | null>(null);

  useEffect(() => {
    if (!visible || !theatreRef.current) {
      setRails([]);
      return;
    }

    const compute = () => {
      const root = theatreRef.current;
      if (!root) return;
      const rootBox = root.getBoundingClientRect();
      const paper = root.querySelector<HTMLElement>(".app-doc__sheet");
      const highlights = root.querySelectorAll<HTMLElement>(".theatre-highlight-bar[data-visible='true']");
      const pipelineStages = document.querySelectorAll<HTMLElement>(".app-pipeline__tile[data-state='active'], .app-pipeline__tile[data-state='done']");

      if (!paper || highlights.length === 0 || pipelineStages.length === 0) {
        setRails([]);
        return;
      }

      const railCount = Math.min(highlights.length, 4, pipelineStages.length);
      const newRails: RailEndpoint[] = [];

      for (let i = 0; i < railCount; i++) {
        const hl = highlights[i];
        const tile = pipelineStages[Math.min(i, pipelineStages.length - 1)];
        if (!hl || !tile) continue;

        const hlBox = hl.getBoundingClientRect();
        const tileBox = tile.getBoundingClientRect();

        newRails.push({
          startX: hlBox.left - rootBox.left + hlBox.width / 2,
          startY: hlBox.top - rootBox.top + hlBox.height / 2,
          endX: tileBox.left - rootBox.left + tileBox.width / 2,
          endY: tileBox.top - rootBox.top + tileBox.height,
        });
      }

      setRails(newRails);
    };

    compute();
    const timer = window.setTimeout(compute, 100);

    roRef.current = new ResizeObserver(compute);
    roRef.current.observe(theatreRef.current);

    return () => {
      window.clearTimeout(timer);
      roRef.current?.disconnect();
    };
  }, [theatreRef, highlightCount, visible]);

  return rails;
}
