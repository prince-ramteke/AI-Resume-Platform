interface NavRollLabelProps {
  children: string;
}

/**
 * V2 text-roll label (§16.1.2). Fixed-height viewport clips a two-row stack;
 * on hover/focus the track translates -1em revealing the brighter bottom copy.
 * Active links hold the rolled state. Screen readers see the label once via
 * aria-label on the outer span; the visual copies are aria-hidden.
 */
export function NavRollLabel({ children }: NavRollLabelProps) {
  return (
    <span className="nav-roll" aria-label={children}>
      <span className="nav-roll__viewport" aria-hidden="true">
        <span className="nav-roll__stack">
          <span className="nav-roll__row">{children}</span>
          <span className="nav-roll__row">{children}</span>
        </span>
      </span>
    </span>
  );
}
