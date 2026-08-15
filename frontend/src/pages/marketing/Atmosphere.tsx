import React from "react";

export function useReveal(threshold = 0.25): [React.RefObject<HTMLDivElement | null>, boolean] {
  const ref = React.useRef<HTMLDivElement>(null);
  const [seen, setSeen] = React.useState(false);
  React.useEffect(() => {
    const el = ref.current; if (!el) return;
    if (!("IntersectionObserver" in window)){ setSeen(true); return; }
    const io = new IntersectionObserver((entries) => {
      const e = entries[0];
      if (e && e.isIntersecting){ setSeen(true); io.disconnect(); }
    }, { threshold });
    io.observe(el); return () => io.disconnect();
  }, [threshold]);
  return [ref, seen];
}

export function Reveal({ children, delay = 0, y = 16, threshold, style }:{
  children?: React.ReactNode; delay?: number; y?: number; threshold?: number; style?: React.CSSProperties;
}){
  const [ref, seen] = useReveal(threshold);
  return (
    <div ref={ref} style={{ opacity: seen ? 1 : 0, transform: seen ? "none" : `translateY(${y}px)`,
      transition: `opacity var(--dur-4) var(--ease-out) ${delay}ms, transform var(--dur-4) var(--ease-out) ${delay}ms`, ...style }}>
      {children}
    </div>
  );
}

const RI_PARTICLES = Array.from({ length: 26 }, (_, i) => {
  const x = (i * 37.3) % 100, y = (i * 53.7) % 100, s = i % 5 === 0 ? 2 : 1;
  return { x, y, s, dur: 9000 + (i * 613) % 11000, delay: (i * 877) % 9000, op: i % 3 === 0 ? .20 : .11 };
});

export function Particles({ style }:{ style?: React.CSSProperties }){
  return (
    <div aria-hidden="true" style={{ position:"absolute", inset:0, overflow:"hidden", pointerEvents:"none", ...style }}>
      {RI_PARTICLES.map((p, i) => (
        <span key={i} style={{ position:"absolute", left: p.x + "%", top: p.y + "%", width: p.s + "px", height: p.s + "px",
          borderRadius:"50%", background:"var(--fg-1)", opacity: p.op,
          animation: `mkt-drift ${p.dur}ms var(--ease-in-out) ${p.delay}ms infinite` }}/>
      ))}
    </div>
  );
}

export function StageBg({ grid = true, particles = true, veil = true, children }:{
  grid?: boolean; particles?: boolean; veil?: boolean; children?: React.ReactNode;
}){
  return (
    <>
      <div aria-hidden="true" style={{ position:"absolute", inset:0, background:"var(--grad-stage)" }}/>
      {grid && <div aria-hidden="true" style={{ position:"absolute", inset:0, backgroundImage:"var(--grid-fine)" }}/>}
      {particles && <Particles/>}
      {veil && <div aria-hidden="true" style={{ position:"absolute", inset:"auto 0 0 0", height:"220px", background:"var(--grad-veil-bottom)" }}/>}
      {children}
    </>
  );
}

export function Eyebrow({ children, tone }:{ children?: React.ReactNode; tone?: string }){
  return <span className="mkt-eyebrow" style={tone ? { color: tone } : undefined}>{children}</span>;
}

export function SectionHead({ eyebrow, title, body, align = "left", maxTitle = "24ch", maxBody = "54ch" }:{
  eyebrow: React.ReactNode; title: React.ReactNode; body?: React.ReactNode;
  align?: "left" | "center"; maxTitle?: string; maxBody?: string;
}){
  const center = align === "center";
  return (
    <div style={{ textAlign: align, margin: center ? "0 auto" : undefined, maxWidth: center ? "720px" : undefined }}>
      <Eyebrow>{eyebrow}</Eyebrow>
      <h2 style={{ marginTop:"20px", fontFamily:"var(--font-display)", fontSize:"var(--fs-display-3)",
        lineHeight:"var(--lh-display-3)", letterSpacing:"var(--ls-display-3)", color:"var(--fg-1)",
        maxWidth: maxTitle, marginLeft: center ? "auto" : undefined, marginRight: center ? "auto" : undefined }}>{title}</h2>
      {body && <p style={{ marginTop:"20px", fontSize:"var(--fs-body-lg)", lineHeight:"var(--lh-body-lg)", color:"var(--fg-2)",
        maxWidth: maxBody, marginLeft: center ? "auto" : undefined, marginRight: center ? "auto" : undefined }}>{body}</p>}
    </div>
  );
}
