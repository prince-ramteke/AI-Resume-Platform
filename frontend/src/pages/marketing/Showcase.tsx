import React from "react";
import { Reveal, SectionHead } from "./Atmosphere";
import { Card, Button, Tag, Icon, MatchBar } from "./SiteChrome";

const riRow: React.CSSProperties = { display:"flex", alignItems:"center", gap:"10px", padding:"9px 0", borderTop:"1px solid var(--line-1)" };
const riMono: React.CSSProperties = { fontFamily:"var(--font-mono)", fontSize:"9.5px", letterSpacing:".08em", color:"var(--fg-5)" };
const riLabel: React.CSSProperties = { fontSize:"11.5px", color:"var(--fg-2)", whiteSpace:"nowrap", overflow:"hidden", textOverflow:"ellipsis" };

function MiniChrome({ nav, title, children }:{ nav: number; title: string; children?: React.ReactNode }){
  const glyphs: ("chart-no-axes-column" | "file-plus-2" | "file-check-2" | "history")[] =
    ["chart-no-axes-column","file-plus-2","file-check-2","history"];
  return (
    <div style={{ display:"grid", gridTemplateColumns:"52px minmax(0,1fr)", height:"100%", minHeight:0 }}>
      <div style={{ borderRight:"1px solid var(--line-1)", padding:"12px 0", display:"flex", flexDirection:"column",
        alignItems:"center", gap:"14px", background:"rgba(4,6,10,.35)" }}>
        <span style={{ color:"var(--cyan-400)" }}><Icon name="file-search" size={13}/></span>
        {glyphs.map((g, i) => (
          <span key={g} style={{ color: i === nav ? "var(--fg-1)" : "var(--fg-5)" }}><Icon name={g} size={13}/></span>
        ))}
      </div>
      <div style={{ minWidth:0, display:"flex", flexDirection:"column" }}>
        <div style={{ height:"34px", flex:"0 0 auto", padding:"0 14px", display:"flex", alignItems:"center",
          justifyContent:"space-between", borderBottom:"1px solid var(--line-1)" }}>
          <span style={{ fontSize:"11.5px", color:"var(--fg-2)" }}>{title}</span>
          <span style={riMono}>PRINCE@LOCAL</span>
        </div>
        <div style={{ padding:"14px", minWidth:0, overflow:"hidden" }}>{children}</div>
      </div>
    </div>
  );
}

function MiniDashboard(){
  return (
    <MiniChrome nav={0} title="Dashboard">
      <div style={{ fontFamily:"var(--font-display)", fontSize:"17px", color:"var(--fg-1)" }}>Your workspace</div>
      <div style={{ marginTop:"12px", display:"grid", gridTemplateColumns:"repeat(3,1fr)", gap:"8px" }}>
        {[["Resumes","3"],["Job descriptions","7"],["Analyses","12"]].map(([l, n]) => (
          <div key={l} style={{ padding:"10px", border:"1px solid var(--line-1)", borderRadius:"var(--r-2)", background:"rgba(244,247,250,.02)" }}>
            <div style={{ fontFamily:"var(--font-display)", fontSize:"18px", color:"var(--fg-1)" }}>{n}</div>
            <div style={{ ...riMono, marginTop:"4px", textTransform:"uppercase" }}>{l}</div>
          </div>
        ))}
      </div>
      <div style={{ marginTop:"14px", ...riMono, textTransform:"uppercase" }}>Recent analyses</div>
      <div style={{ marginTop:"4px" }}>
        {[["Senior ML Engineer","78"],["Platform Engineer","64"],["Data Engineer","81"]].map(([t, s]) => (
          <div key={t} style={riRow}>
            <span style={riLabel}>{t}</span>
            <span style={{ marginLeft:"auto", fontFamily:"var(--font-mono)", fontSize:"11px", color:"var(--fg-1)" }}>{s}</span>
          </div>
        ))}
      </div>
    </MiniChrome>
  );
}

function MiniResumes(){
  return (
    <MiniChrome nav={1} title="Resumes">
      <div style={{ display:"flex", alignItems:"center", justifyContent:"space-between" }}>
        <div style={{ fontFamily:"var(--font-display)", fontSize:"17px", color:"var(--fg-1)" }}>Resumes</div>
        <span style={{ height:"22px", padding:"0 9px", display:"inline-flex", alignItems:"center", borderRadius:"var(--r-2)",
          background:"var(--cyan-500)", color:"var(--text-on-accent)", fontSize:"10.5px" }}>Upload</span>
      </div>
      <div style={{ marginTop:"12px" }}>
        {[["prince-ramteke-2026.pdf","184 KB","PARSED"],["backend-focus.pdf","171 KB","PARSED"],["ml-variant.docx","96 KB","QUEUED"]].map(([f, s, st]) => (
          <div key={f} style={riRow}>
            <span style={{ color:"var(--fg-4)" }}><Icon name="file-check-2" size={13}/></span>
            <span style={riLabel}>{f}</span>
            <span style={{ marginLeft:"auto", ...riMono }}>{s}</span>
            <span style={{ ...riMono, color: st === "PARSED" ? "var(--signal-pass)" : "var(--fg-4)" }}>{st}</span>
          </div>
        ))}
      </div>
      <div style={{ marginTop:"14px", ...riMono }}>page 1 of 1 · 3 of 3</div>
    </MiniChrome>
  );
}

function MiniJobDescription(){
  return (
    <MiniChrome nav={2} title="Job descriptions">
      <div style={{ fontFamily:"var(--font-display)", fontSize:"17px", color:"var(--fg-1)" }}>Senior ML Engineer</div>
      <div style={{ marginTop:"6px", ...riMono }}>added 12 aug 2026 · 4 812 chars</div>
      <div style={{ marginTop:"12px", display:"flex", flexWrap:"wrap", gap:"6px" }}>
        {["pgvector","rrf","kubernetes","postgres","evaluation"].map(k => (
          <Tag key={k} style={{ height:"22px", fontSize:"10.5px" }}>{k}</Tag>
        ))}
      </div>
      <div style={{ marginTop:"12px", padding:"10px", border:"1px solid var(--line-1)", borderRadius:"var(--r-2)",
        background:"rgba(244,247,250,.02)" }}>
        {[92,78,86,64].map((w, i) => (
          <div key={i} style={{ height:"4px", width: w + "%", marginBottom:"7px", borderRadius:"1px",
            background: i === 1 ? "rgba(232,176,75,.5)" : "rgba(244,247,250,.09)" }}/>
        ))}
      </div>
      <div style={{ marginTop:"10px", ...riMono }}>≤5 distinctive technical tokens extracted</div>
    </MiniChrome>
  );
}

function MiniResult(){
  const groups: [string, string[], boolean | null][] = [["Matched",["rrf","pgvector"],true],["Weak",["k8s"],null],["Missing",["reranking"],false]];
  return (
    <MiniChrome nav={3} title="Analysis 1184">
      <div style={{ display:"flex", gap:"14px", alignItems:"center" }}>
        <div style={{ fontFamily:"var(--font-display)", fontSize:"34px", lineHeight:1, color:"var(--fg-1)" }}>78</div>
        <div style={{ minWidth:0, flex:1 }}>
          <MatchBar label="Match" value={78} detail="ollama · llama3.1:8b"/>
        </div>
      </div>
      <div style={{ marginTop:"14px", display:"grid", gridTemplateColumns:"repeat(3,1fr)", gap:"8px" }}>
        {groups.map(([t, items, m]) => (
          <div key={t}>
            <div style={{ ...riMono, textTransform:"uppercase" }}>{t}</div>
            <div style={{ marginTop:"7px", display:"flex", flexWrap:"wrap", gap:"5px" }}>
              {items.map(s => <Tag key={s} matched={m} style={{ height:"21px", fontSize:"10px" }}>{s}</Tag>)}
            </div>
          </div>
        ))}
      </div>
      <div style={{ marginTop:"14px", padding:"10px", border:"1px solid var(--line-1)", borderRadius:"var(--r-2)" }}>
        <div style={{ ...riMono, textTransform:"uppercase" }}>Recommendation · high</div>
        <div style={{ marginTop:"6px", fontSize:"11.5px", lineHeight:1.55, color:"var(--fg-3)" }}>
          Name the cluster work under the retrieval role, with scope and outcome.
        </div>
      </div>
    </MiniChrome>
  );
}

function MiniEvidence(){
  const rows: [string, string, string][] = [
    ["RESUME#2","cited by rrf fusion","Owned the hybrid retrieval path: pgvector cosine fused with full-text."],
    ["RESUME#5","cited by evaluation","Built the retrieval evaluation harness — 15 cases, Recall@K and MRR."],
    ["JD#3",    "cited by vector database","Must have production experience operating vector databases."]
  ];
  return (
    <MiniChrome nav={3} title="Evidence thread">
      <div style={{ fontFamily:"var(--font-display)", fontSize:"17px", color:"var(--fg-1)" }}>Evidence</div>
      <div style={{ marginTop:"10px", display:"grid", gap:"8px" }}>
        {rows.map(([r, c, s], i) => (
          <div key={r} style={{ padding:"9px 10px", borderRadius:"var(--r-2)",
            border:"1px solid " + (i === 0 ? "rgba(76,201,232,.34)" : "var(--line-1)"),
            background: i === 0 ? "rgba(76,201,232,.06)" : "rgba(244,247,250,.02)" }}>
            <div style={{ display:"flex", gap:"8px", alignItems:"center" }}>
              <span style={{ fontFamily:"var(--font-mono)", fontSize:"10.5px", color: i === 0 ? "var(--cyan-400)" : "var(--fg-4)" }}>{r}</span>
              <span style={riMono}>{c}</span>
            </div>
            <div style={{ marginTop:"5px", fontSize:"11px", lineHeight:1.5, color:"var(--fg-3)", overflow:"hidden" }}>{s}</div>
          </div>
        ))}
      </div>
    </MiniChrome>
  );
}

const RI_SURFACES: [string, string, React.ComponentType][] = [
  ["Dashboard",         "overview + entry points",     MiniDashboard],
  ["Resume management", "upload · replace · download", MiniResumes],
  ["Job description",   "paste or upload · search",    MiniJobDescription],
  ["Analysis result",   "score · matched / weak / missing", MiniResult],
  ["Evidence thread",   "claims linked to chunks",     MiniEvidence]
];

function SurfaceCard({ title, note, Surface }:{ title: string; note: string; Surface: React.ComponentType }){
  return (
    <div style={{ flex:"0 0 auto", width:"468px" }}>
      <Card padding={0} style={{ height:"290px", overflow:"hidden" }}>
        <div style={{ height:"100%" }}><Surface/></div>
      </Card>
      <div style={{ marginTop:"14px", display:"flex", alignItems:"baseline", gap:"12px" }}>
        <span style={{ fontSize:"13.5px", color:"var(--fg-1)" }}>{title}</span>
        <span style={{ fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono-sm)", letterSpacing:"var(--ls-mono-sm)", color:"var(--fg-5)" }}>{note}</span>
      </div>
    </div>
  );
}

export function Showcase(){
  const [paused, setPaused] = React.useState(false);
  const set = RI_SURFACES.concat(RI_SURFACES);
  return (
    <section data-screen-label="Product showcase"
      style={{ position:"relative", overflow:"hidden", padding:"var(--section-y) 0", borderTop:"1px solid var(--line-1)" }}>
      <div style={{ maxWidth:"var(--max-page)", margin:"0 auto", padding:"0 44px" }}>
        <Reveal><SectionHead eyebrow="The product"
          title="Every surface reports what it did"
          body="Upload a resume, add a job description, run one analysis, and read the result as a thread of citations rather than a verdict."/></Reveal>
      </div>
      <div onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)}
        style={{ marginTop:"64px", position:"relative",
          maskImage:"linear-gradient(90deg,transparent,#000 6%,#000 94%,transparent)",
          WebkitMaskImage:"linear-gradient(90deg,transparent,#000 6%,#000 94%,transparent)" }}>
        <div style={{ display:"flex", gap:"28px", width:"max-content", padding:"0 44px",
          animation:"mkt-marquee 46000ms linear infinite", animationPlayState: paused ? "paused" : "running" }}>
          {set.map(([t, n, S], i) => <SurfaceCard key={t + i} title={t} note={n} Surface={S}/>)}
        </div>
      </div>
      <div style={{ maxWidth:"var(--max-page)", margin:"56px auto 0", padding:"0 44px" }}>
        <Reveal>
          <div style={{ display:"flex", gap:"14px", alignItems:"center" }}>
            <Button to="/register" variant="outline"><Icon name="sliders-horizontal" size={16}/>Tour the workspace</Button>
            <span style={{ fontSize:"13px", color:"var(--fg-4)" }}>Sample data shown. One analysis takes up to a couple of minutes on a local model.</span>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
