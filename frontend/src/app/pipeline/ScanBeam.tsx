interface Props {
  mode?: "vertical" | "horizontal";
  className?: string;
}

/**
 * Reusable animated scan beam. Vertical mode: 1px cyan bar drifting top→bottom
 * over 3200ms (used on DocumentPaper). Horizontal mode: cyan bar sliding
 * left→right over 1600ms (used on active pipeline tiles). Blend-mode auto-
 * flips via --beam-blend in theme.css (screen dark, multiply light).
 */
export function ScanBeam({ mode = "vertical", className }: Props) {
  return <span className={`app-scanbeam ${className ?? ""}`.trim()} data-mode={mode} aria-hidden="true" />;
}
