import React from "react";
import { Link } from "react-router-dom";

/* ---------------- Shared primitives (inlined from the design system) ---------------- */

type IconName =
  | "file-search" | "scan-line" | "sun" | "moon" | "sliders-horizontal"
  | "chart-no-axes-column" | "file-plus-2" | "file-check-2" | "history"
  | "git-merge" | "link-2" | "check" | "loader" | "circle";

/* Minimal Lucide-compatible inline SVGs (stroke 1.5, 24-grid, round caps). */
const ICONS: Record<IconName, string> = {
  "file-search": '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h5"/><path d="M14 2v6h6"/><circle cx="16.5" cy="16.5" r="2.5"/><path d="m21 21-2.5-2.5"/>',
  "scan-line": '<path d="M3 7V5a2 2 0 0 1 2-2h2"/><path d="M17 3h2a2 2 0 0 1 2 2v2"/><path d="M21 17v2a2 2 0 0 1-2 2h-2"/><path d="M7 21H5a2 2 0 0 1-2-2v-2"/><path d="M7 12h10"/>',
  "sun": '<circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/>',
  "moon": '<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>',
  "sliders-horizontal": '<line x1="21" x2="14" y1="4" y2="4"/><line x1="10" x2="3" y1="4" y2="4"/><line x1="21" x2="12" y1="12" y2="12"/><line x1="8" x2="3" y1="12" y2="12"/><line x1="21" x2="16" y1="20" y2="20"/><line x1="12" x2="3" y1="20" y2="20"/><line x1="14" x2="14" y1="2" y2="6"/><line x1="8" x2="8" y1="10" y2="14"/><line x1="16" x2="16" y1="18" y2="22"/>',
  "chart-no-axes-column": '<line x1="18" x2="18" y1="20" y2="10"/><line x1="12" x2="12" y1="20" y2="4"/><line x1="6" x2="6" y1="20" y2="14"/>',
  "file-plus-2": '<path d="M4 22h14a2 2 0 0 0 2-2V7l-5-5H6a2 2 0 0 0-2 2v4"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M3 15h6"/><path d="M6 12v6"/>',
  "file-check-2": '<path d="M4 22h14a2 2 0 0 0 2-2V7l-5-5H6a2 2 0 0 0-2 2v4"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="m3 15 2 2 4-4"/>',
  "history": '<path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M12 7v5l4 2"/>',
  "git-merge": '<circle cx="18" cy="18" r="3"/><circle cx="6" cy="6" r="3"/><path d="M6 21V9a9 9 0 0 0 9 9"/>',
  "link-2": '<path d="M9 17H7A5 5 0 0 1 7 7h2"/><path d="M15 7h2a5 5 0 1 1 0 10h-2"/><line x1="8" x2="16" y1="12" y2="12"/>',
  "check": '<polyline points="20 6 9 17 4 12"/>',
  "loader": '<line x1="12" x2="12" y1="2" y2="6"/><line x1="12" x2="12" y1="18" y2="22"/><line x1="4.93" x2="7.76" y1="4.93" y2="7.76"/><line x1="16.24" x2="19.07" y1="16.24" y2="19.07"/><line x1="2" x2="6" y1="12" y2="12"/><line x1="18" x2="22" y1="12" y2="12"/><line x1="4.93" x2="7.76" y1="19.07" y2="16.24"/><line x1="16.24" x2="19.07" y1="7.76" y2="4.93"/>',
  "circle": '<circle cx="12" cy="12" r="10"/>'
};

export function Icon({ name, size = 16, stroke = 1.5, color = "currentColor", style }:{
  name: IconName; size?: number; stroke?: number; color?: string; style?: React.CSSProperties;
}){
  return (
    <span aria-hidden="true" style={{ display:"inline-flex", width:size, height:size, flex:"0 0 auto", ...style }}
      dangerouslySetInnerHTML={{ __html:
        `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="${stroke}" stroke-linecap="round" stroke-linejoin="round">${ICONS[name] || ""}</svg>`
      }}/>
  );
}

/* Button ---------------------------------------------------------- */
type BVar = "primary" | "secondary" | "ghost" | "outline" | "danger";
type BSize = "sm" | "md" | "lg";
const BTN_BASE: React.CSSProperties = { display:"inline-flex", alignItems:"center", justifyContent:"center", gap:"8px", fontFamily:"var(--font-sans)", fontWeight:500, letterSpacing:"-0.005em", borderRadius:"var(--r-control)", border:"1px solid transparent", cursor:"pointer", transition:"var(--t-control)", whiteSpace:"nowrap", textDecoration:"none", lineHeight:1 };
const BTN_SIZES: Record<BSize, React.CSSProperties> = {
  sm:{ height:"30px", padding:"0 12px", fontSize:"13px" },
  md:{ height:"38px", padding:"0 18px", fontSize:"14px" },
  lg:{ height:"46px", padding:"0 26px", fontSize:"15px" }
};
const BTN_VAR: Record<BVar, React.CSSProperties> = {
  primary:{ background:"var(--cyan-500)", color:"var(--text-on-accent)", borderColor:"var(--cyan-400)", boxShadow:"var(--shadow-2)" },
  secondary:{ background:"var(--ink-700)", color:"var(--fg-1)", borderColor:"var(--line-2)", boxShadow:"var(--inner-light)" },
  ghost:{ background:"transparent", color:"var(--fg-2)", borderColor:"transparent" },
  outline:{ background:"transparent", color:"var(--fg-1)", borderColor:"var(--line-3)" },
  danger:{ background:"transparent", color:"var(--signal-fail)", borderColor:"rgba(242,104,92,.42)" }
};
const BTN_HOVER: Record<BVar, React.CSSProperties> = {
  primary:{ background:"var(--cyan-400)" },
  secondary:{ background:"var(--ink-600)", borderColor:"var(--line-3)" },
  ghost:{ background:"rgba(244,247,250,.05)", color:"var(--fg-1)" },
  outline:{ background:"rgba(244,247,250,.04)" },
  danger:{ background:"rgba(242,104,92,.10)" }
};

export function Button({ variant = "primary", size = "md", disabled = false, full = false, as, to, href, children, style, onClick, ...rest }:{
  variant?: BVar; size?: BSize; disabled?: boolean; full?: boolean;
  as?: "button" | "a"; to?: string; href?: string;
  children?: React.ReactNode; style?: React.CSSProperties;
  onClick?: (e: React.MouseEvent) => void;
} & Record<string, unknown>){
  const [hover, setHover] = React.useState(false);
  const [down, setDown] = React.useState(false);
  const s: React.CSSProperties = { ...BTN_BASE, ...BTN_SIZES[size], ...BTN_VAR[variant],
    ...(hover && !disabled ? BTN_HOVER[variant] : null),
    ...(down && !disabled ? { transform:"translateY(0.5px) scale(0.995)" } : null),
    ...(full ? { width:"100%" } : null),
    ...(disabled ? { opacity:.38, cursor:"not-allowed", boxShadow:"none" } : null),
    ...style };
  const handlers = {
    onMouseEnter:() => setHover(true),
    onMouseLeave:() => { setHover(false); setDown(false); },
    onMouseDown:() => setDown(true),
    onMouseUp:() => setDown(false),
    onClick: disabled ? undefined : onClick
  };
  if (to){
    return <Link to={to} style={s} {...handlers} {...(rest as Record<string, unknown>)}>{children}</Link>;
  }
  if (as === "a" || href){
    return <a href={href} style={s} {...handlers} {...(rest as Record<string, unknown>)}>{children}</a>;
  }
  return <button type="button" disabled={disabled} style={s} {...handlers} {...(rest as Record<string, unknown>)}>{children}</button>;
}

/* Card ------------------------------------------------------------ */
type CardTone = "raised" | "flat" | "glass" | "outline";
export function Card({ tone = "raised", interactive = false, padding = 24, children, style, onClick, ...rest }:{
  tone?: CardTone; interactive?: boolean; padding?: number | string;
  children?: React.ReactNode; style?: React.CSSProperties;
  onClick?: () => void;
} & Record<string, unknown>){
  const [hover, setHover] = React.useState(false);
  const tones: Record<CardTone, React.CSSProperties> = {
    raised:{ background:"var(--surface-card)", border:"1px solid var(--border-subtle)", boxShadow:"var(--elev-card)" },
    flat:{ background:"rgba(244,247,250,.02)", border:"1px solid var(--border-subtle)", boxShadow:"none" },
    glass:{ background:"var(--glass)", border:"1px solid var(--glass-border)", backdropFilter:"var(--glass-blur)", WebkitBackdropFilter:"var(--glass-blur)", boxShadow:"var(--shadow-3)" },
    outline:{ background:"transparent", border:"1px solid var(--border-default)", boxShadow:"none" }
  };
  return (
    <div onClick={onClick}
      onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}
      style={{ position:"relative", borderRadius:"var(--r-card)", padding: typeof padding === "number" ? padding + "px" : padding,
        transition:"var(--t-surface)", ...tones[tone],
        ...(interactive ? { cursor:"pointer" } : null),
        ...(interactive && hover ? { transform:"translateY(-2px)", boxShadow:"var(--shadow-3),var(--inner-light)", borderColor:"var(--border-default)" } : null),
        ...style }}
      {...(rest as Record<string, unknown>)}>
      <div style={{ position:"absolute", inset:0, borderRadius:"inherit", background:"var(--grad-card)", pointerEvents:"none" }}/>
      <div style={{ position:"relative" }}>{children}</div>
    </div>
  );
}

/* Tag ------------------------------------------------------------- */
export function Tag({ children, matched = null, style, ...rest }:{
  children?: React.ReactNode; matched?: boolean | null; style?: React.CSSProperties;
} & Record<string, unknown>){
  const tone = matched === true
    ? { c:"var(--amber-300)", bg:"rgba(232,176,75,.12)", b:"rgba(232,176,75,.34)" }
    : matched === false
    ? { c:"var(--fg-3)", bg:"transparent", b:"var(--line-2)" }
    : { c:"var(--fg-1)", bg:"rgba(244,247,250,.05)", b:"var(--line-2)" };
  return (
    <span style={{ display:"inline-flex", alignItems:"center", gap:"6px", height:"26px", padding:"0 10px",
      borderRadius:"var(--r-2)", background:tone.bg, border:"1px solid " + tone.b, color:tone.c,
      fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono)", transition:"var(--t-control)", ...style }}
      {...(rest as Record<string, unknown>)}>
      {children}
    </span>
  );
}

/* MatchBar -------------------------------------------------------- */
export function MatchBar({ label, value = 0, detail, tone = "cyan", animate = true, style }:{
  label: React.ReactNode; value?: number; detail?: React.ReactNode;
  tone?: "cyan" | "amber" | "pass"; animate?: boolean; style?: React.CSSProperties;
}){
  const [w, setW] = React.useState(animate ? 0 : value);
  React.useEffect(() => { const t = setTimeout(() => setW(value), 40); return () => clearTimeout(t); }, [value]);
  const stroke = tone === "amber" ? "var(--amber-500)" : tone === "pass" ? "var(--signal-pass)" : "var(--cyan-500)";
  return (
    <div style={style}>
      <div style={{ display:"flex", justifyContent:"space-between", alignItems:"baseline", marginBottom:"8px", gap:"12px" }}>
        <span style={{ fontSize:"13.5px", color:"var(--fg-2)" }}>{label}</span>
        <span style={{ fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono)", color:"var(--fg-1)", fontVariantNumeric:"tabular-nums" }}>{Math.round(value)}%</span>
      </div>
      <div style={{ height:"3px", borderRadius:"var(--r-pill)", background:"rgba(244,247,250,.07)", overflow:"hidden" }}>
        <div style={{ height:"100%", width:"100%", background: stroke,
          transform:`scaleX(${w / 100})`, transformOrigin:"left",
          transition:"transform var(--dur-5) var(--ease-out)" }}/>
      </div>
      {detail && <div style={{ marginTop:"7px", fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono-sm)",
        letterSpacing:"var(--ls-mono-sm)", color:"var(--fg-4)" }}>{detail}</div>}
    </div>
  );
}

/* DocumentSheet --------------------------------------------------- */
export function DocumentSheet({ lines = 14, width = 300, tilt = 0, scan = false, highlights = [], title = "RESUME.PDF", children, style }:{
  lines?: number; width?: number; tilt?: number; scan?: boolean; highlights?: number[];
  title?: string; children?: React.ReactNode; style?: React.CSSProperties;
}){
  const rows = Array.from({ length: lines }, (_, i) => i);
  const widths = [96,88,72,92,60,84,78,66,90,52,86,74,94,68];
  return (
    <div style={{ perspective:"1400px", display:"inline-block", ...style }}>
      <div style={{ position:"relative", width: width + "px", padding:"26px 24px 30px",
        background:"var(--grad-paper)", borderRadius:"var(--r-paper)", boxShadow:"var(--elev-paper)",
        transform: tilt ? `rotateY(${tilt}deg) rotateX(2deg)` : "none",
        transformStyle:"preserve-3d", overflow:"hidden" }}>
        <div style={{ fontFamily:"var(--font-mono)", fontSize:"8.5px", letterSpacing:".16em", textTransform:"uppercase",
          color:"var(--paper-ink-2)", marginBottom:"16px" }}>{title}</div>
        {children || rows.map(i => {
          const hl = highlights.includes(i);
          const w = widths[i % widths.length];
          return <div key={i} style={{ height:"6px", width: w + "%", marginBottom:"9px", borderRadius:"1px",
            background: hl ? "rgba(232,176,75,.55)" : "rgba(25,23,18,.16)",
            boxShadow: hl ? "0 0 0 2px rgba(232,176,75,.16)" : "none" }}/>;
        })}
        {scan && <div style={{ position:"absolute", left:0, right:0, height:"84px", background:"var(--grad-beam)",
          mixBlendMode:"screen", animation:"mkt-scan 2600ms var(--ease-in-out) infinite" }}/>}
      </div>
    </div>
  );
}

/* ScoreDial ------------------------------------------------------- */
export function ScoreDial({ value = 0, size = 168, label = "ATS score", sublabel, animate = true, style }:{
  value?: number; size?: number; label?: string; sublabel?: string; animate?: boolean; style?: React.CSSProperties;
}){
  const [shown, setShown] = React.useState(animate ? 0 : value);
  React.useEffect(() => {
    if (!animate){ setShown(value); return; }
    const reduce = window.matchMedia && window.matchMedia("(prefers-reduced-motion:reduce)").matches;
    if (reduce){ setShown(value); return; }
    let raf: number; let t0 = 0; const dur = 1400;
    const ease = (t: number) => 1 - Math.pow(1 - t, 3);
    const step = (t: number) => { if (!t0) t0 = t; const p = Math.min(1, (t - t0) / dur); setShown(value * ease(p)); if (p < 1) raf = requestAnimationFrame(step); };
    raf = requestAnimationFrame(step); return () => cancelAnimationFrame(raf);
  }, [value, animate]);
  const r = size / 2 - 9, C = 2 * Math.PI * r, pct = Math.max(0, Math.min(100, shown)) / 100;
  const tone = shown >= 80 ? "var(--signal-pass)" : shown >= 60 ? "var(--amber-500)" : "var(--signal-fail)";
  return (
    <div style={{ display:"inline-flex", flexDirection:"column", alignItems:"center", gap:"14px", ...style }}>
      <div style={{ position:"relative", width:size, height:size }}>
        <svg width={size} height={size} style={{ display:"block", transform:"rotate(-90deg)" }}>
          <circle cx={size/2} cy={size/2} r={r} fill="none" stroke="rgba(244,247,250,.07)" strokeWidth="2"/>
          <circle cx={size/2} cy={size/2} r={r} fill="none" stroke={tone} strokeWidth="2.5" strokeLinecap="round"
            strokeDasharray={C} strokeDashoffset={C * (1 - pct)}/>
        </svg>
        <div style={{ position:"absolute", inset:0, display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center" }}>
          <span style={{ fontFamily:"var(--font-display)", fontSize: size * 0.34 + "px", lineHeight:1, color:"var(--fg-1)",
            fontVariantNumeric:"tabular-nums" }}>{Math.round(shown)}</span>
          <span style={{ marginTop:"6px", fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono-sm)",
            letterSpacing:"var(--ls-eyebrow)", textTransform:"uppercase", color:"var(--fg-4)" }}>/ 100</span>
        </div>
      </div>
      <div style={{ textAlign:"center" }}>
        <div style={{ fontFamily:"var(--font-mono)", fontSize:"var(--fs-eyebrow)", letterSpacing:"var(--ls-eyebrow)",
          textTransform:"uppercase", color:"var(--fg-3)" }}>{label}</div>
        {sublabel && <div style={{ marginTop:"6px", fontSize:"12.5px", color:"var(--fg-4)" }}>{sublabel}</div>}
      </div>
    </div>
  );
}

/* PipelineTrack --------------------------------------------------- */
export type PipelineStep = { label: string; detail?: string; state?: "done" | "active" | "pending" };
const PSTATE: Record<NonNullable<PipelineStep["state"]>, { c: string; i: IconName }> = {
  done:{ c:"var(--signal-pass)", i:"check" },
  active:{ c:"var(--cyan-400)", i:"loader" },
  pending:{ c:"var(--fg-5)", i:"circle" }
};
export function PipelineTrack({ steps = [], orientation = "horizontal", style }:{
  steps: PipelineStep[]; orientation?: "horizontal" | "vertical"; style?: React.CSSProperties;
}){
  const vertical = orientation === "vertical";
  return (
    <div style={{ display:"flex", flexDirection: vertical ? "column" : "row", ...style }}>
      {steps.map((s, i) => {
        const st = PSTATE[s.state || "pending"];
        const last = i === steps.length - 1;
        return (
          <div key={s.label + i} style={{ display:"flex", flexDirection: vertical ? "row" : "column", gap: vertical ? "14px" : "0",
            flex: vertical ? "none" : 1, minWidth:0 }}>
            <div style={{ display:"flex", flexDirection: vertical ? "column" : "row", alignItems:"center", width: vertical ? "auto" : "100%" }}>
              <span style={{ width:"22px", height:"22px", borderRadius:"50%", flex:"0 0 auto",
                border:"1px solid " + (s.state === "pending" ? "var(--line-2)" : st.c),
                background: s.state === "active" ? "rgba(76,201,232,.12)" : "transparent",
                color: st.c, display:"flex", alignItems:"center", justifyContent:"center" }}>
                <Icon name={st.i} size={11} stroke={2}/>
              </span>
              {!last && <span style={{ flex:1, ...(vertical ? { width:"1px", minHeight:"34px" } : { height:"1px" }),
                background: s.state === "done" ? "var(--signal-pass)" : "var(--line-2)", opacity: s.state === "done" ? .5 : 1 }}/>}
            </div>
            <div style={{ padding: vertical ? "0 0 22px" : "12px 16px 0 0" }}>
              <div style={{ fontSize:"13.5px", color: s.state === "pending" ? "var(--fg-4)" : "var(--fg-1)" }}>{s.label}</div>
              {s.detail && <div style={{ marginTop:"4px", fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono-sm)",
                letterSpacing:"var(--ls-mono-sm)", color:"var(--fg-4)" }}>{s.detail}</div>}
            </div>
          </div>
        );
      })}
    </div>
  );
}

/* Badge ----------------------------------------------------------- */
export function Badge({ children, style }:{ children?: React.ReactNode; style?: React.CSSProperties }){
  return (
    <span style={{ display:"inline-flex", alignItems:"center", height:"22px", padding:"0 9px", borderRadius:"var(--r-2)",
      background:"var(--wash-2)", color:"var(--fg-2)", fontFamily:"var(--font-mono)",
      fontSize:"var(--fs-mono-sm)", letterSpacing:"var(--ls-mono-sm)", ...style }}>{children}</span>
  );
}

/* ---------------- Nav + Footer ---------------- */

export function ThemeToggle(){
  const [theme, setTheme] = React.useState<string>(() => {
    if (typeof document === "undefined") return "light";
    const root = document.querySelector<HTMLElement>(".mkt-root");
    return root?.getAttribute("data-theme") || "light";
  });
  const [hover, setHover] = React.useState(false);
  const apply = (t: string) => {
    const root = document.querySelector<HTMLElement>(".mkt-root");
    if (root) root.setAttribute("data-theme", t);
    try { localStorage.setItem("ri-theme", t); } catch {}
    setTheme(t);
  };
  const next = theme === "dark" ? "light" : "dark";
  return (
    <button type="button" onClick={() => apply(next)} aria-label={`Switch to ${next} theme`} aria-pressed={theme === "light"}
      onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}
      style={{ width:"30px", height:"30px", display:"inline-flex", alignItems:"center", justifyContent:"center",
        borderRadius:"var(--r-control)", border:"1px solid " + (hover ? "var(--line-3)" : "var(--line-2)"),
        background: hover ? "rgba(244,247,250,.05)" : "transparent",
        color: hover ? "var(--fg-1)" : "var(--fg-3)", cursor:"pointer", transition:"var(--t-control)" }}>
      <Icon name={theme === "dark" ? "sun" : "moon"} size={15}/>
    </button>
  );
}

export function SiteNav(){
  const links = ["Product", "How it works", "Evidence", "Docs"];
  return (
    <nav style={{ position:"sticky", top:0, zIndex:20, height:"66px", display:"flex", alignItems:"center",
      justifyContent:"space-between", padding:"0 44px", borderBottom:"1px solid var(--line-1)",
      background:"rgba(7,10,15,.72)", backdropFilter:"var(--glass-blur)" }}>
      <div style={{ display:"flex", alignItems:"center", gap:"10px" }}>
        <span style={{ width:"22px", height:"22px", borderRadius:"var(--r-1)", border:"1px solid var(--line-3)",
          display:"flex", alignItems:"center", justifyContent:"center", color:"var(--cyan-400)" }}>
          <Icon name="file-search" size={13}/>
        </span>
        <span style={{ fontFamily:"var(--font-mono)", fontSize:"10.5px", letterSpacing:".16em", textTransform:"uppercase", color:"var(--fg-2)" }}>
          Resume<span style={{ color:"var(--fg-4)" }}>Intelligence</span>
        </span>
      </div>
      <div style={{ display:"flex", alignItems:"center", gap:"28px" }}>
        {links.map(l => <a key={l} href="#" style={{ fontSize:"13.5px", color:"var(--fg-3)" }}>{l}</a>)}
      </div>
      <div style={{ display:"flex", alignItems:"center", gap:"12px" }}>
        <ThemeToggle/>
        <Button to="/login" variant="ghost" size="sm">Sign in</Button>
        <Button to="/register" size="sm">Analyze My Resume</Button>
      </div>
    </nav>
  );
}

export function SiteFooter(){
  const productLinks: [string, string][] = [
    ["Overview", "/"],
    ["How it works", "#how-it-works"],
    ["ATS analysis", "#ats-analysis"],
    ["Evidence engine", "#evidence-engine"]
  ];
  const projectLinks: [string, string][] = [
    ["GitHub", "https://github.com/prince-ramteke/AI-Resume-Platform"],
    ["Documentation", "https://github.com/prince-ramteke/AI-Resume-Platform/tree/main/docs"],
    ["Architecture", "https://github.com/prince-ramteke/AI-Resume-Platform/blob/main/docs/SYSTEM_ARCHITECTURE.md"]
  ];
  const connectLinks: [string, string][] = [
    ["LinkedIn", "https://www.linkedin.com/in/prince-ramteke-13178a348/"],
    ["prince.ramteke.tech@gmail.com", "mailto:prince.ramteke.tech@gmail.com"],
    ["princeramteke575@gmail.com", "mailto:princeramteke575@gmail.com"]
  ];

  return (
    <footer style={{ borderTop:"1px solid var(--line-1)", padding:"64px 44px 44px", background:"var(--ink-1000)" }}>
      <div style={{ maxWidth:"var(--max-page)", margin:"0 auto", display:"grid", gridTemplateColumns:"1.4fr repeat(3,1fr)", gap:"48px" }}>
        <div>
          <div style={{ fontFamily:"var(--font-mono)", fontSize:"10.5px", letterSpacing:".16em", textTransform:"uppercase", color:"var(--fg-2)" }}>
            Resume<span style={{ color:"var(--fg-4)" }}>Intelligence</span>
          </div>
          <p style={{ marginTop:"16px", fontSize:"13px", color:"var(--fg-4)", lineHeight:1.7, maxWidth:"34ch" }}>
            Career intelligence for people who would rather read the evidence than trust a score.
          </p>
        </div>
        <div>
          <div className="mkt-eyebrow">Product</div>
          <div style={{ marginTop:"16px", display:"flex", flexDirection:"column", gap:"10px" }}>
            {productLinks.map(([label, href]) => (
              href.startsWith("http")
                ? <a key={label} href={href} target="_blank" rel="noopener noreferrer" style={{ fontSize:"13px", color:"var(--fg-3)" }}>{label}</a>
                : href.startsWith("#") || href === "/"
                ? <a key={label} href={href} style={{ fontSize:"13px", color:"var(--fg-3)" }}>{label}</a>
                : <span key={label} style={{ fontSize:"13px", color:"var(--fg-3)" }}>{label}</span>
            ))}
          </div>
        </div>
        <div>
          <div className="mkt-eyebrow">Project</div>
          <div style={{ marginTop:"16px", display:"flex", flexDirection:"column", gap:"10px" }}>
            {projectLinks.map(([label, href]) => (
              <a key={label} href={href} target="_blank" rel="noopener noreferrer" style={{ fontSize:"13px", color:"var(--fg-3)" }}>{label}</a>
            ))}
          </div>
        </div>
        <div>
          <div className="mkt-eyebrow">Connect</div>
          <div style={{ marginTop:"16px", display:"flex", flexDirection:"column", gap:"10px" }}>
            {connectLinks.map(([label, href]) => (
              <a key={label} href={href} {...(href.startsWith("http") ? { target:"_blank", rel:"noopener noreferrer" } : {})} style={{ fontSize:"13px", color:"var(--fg-3)" }}>{label}</a>
            ))}
          </div>
        </div>
      </div>
      <div style={{ maxWidth:"var(--max-page)", margin:"48px auto 0", paddingTop:"22px", borderTop:"1px solid var(--line-1)",
        display:"flex", justifyContent:"space-between", fontFamily:"var(--font-mono)", fontSize:"10.5px", letterSpacing:".06em", color:"var(--fg-5)" }}>
        <span>© 2026 Prince Ramteke · AI Resume Intelligence Platform</span>
      </div>
    </footer>
  );
}
