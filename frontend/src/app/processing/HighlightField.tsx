import { useMemo } from "react";

interface Props {
  analysisId?: string | number;
  count?: number;
  visible?: boolean;
}

/**
 * L5 — amber highlight bars on the document paper. Positions are seeded
 * deterministically per analysis ID (mulberry32 hash) so the same run
 * looks consistent across renders. Highlights fade in staggered pairs
 * once the Chunking stage completes.
 */
function mulberry32(seed: number): () => number {
  return () => {
    seed |= 0;
    seed = (seed + 0x6d2b79f5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function hashId(id: string | number): number {
  const s = String(id);
  let h = 0;
  for (let i = 0; i < s.length; i++) {
    h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  }
  return h;
}

export function HighlightField({ analysisId = 0, count = 5, visible = false }: Props) {
  const bars = useMemo(() => {
    const rng = mulberry32(hashId(analysisId));
    return Array.from({ length: count }, (_, i) => {
      const top = 12 + rng() * 72;
      const left = 6 + rng() * 10;
      const width = 40 + rng() * 40;
      return { top, left, width, delay: Math.floor(i / 2) * 60 };
    });
  }, [analysisId, count]);

  return (
    <div className="theatre-highlights" aria-hidden="true">
      {bars.map((bar, i) => (
        <span
          key={i}
          className="theatre-highlight-bar"
          data-visible={visible ? "true" : "false"}
          style={{
            top: `${bar.top}%`,
            left: `${bar.left}%`,
            width: `${bar.width}%`,
            animationDelay: `${bar.delay}ms`,
          }}
        />
      ))}
    </div>
  );
}
