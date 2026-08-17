import React from "react";
import { Reveal, SectionHead } from "./Atmosphere";
import { Card, Tag, Icon, DocumentSheet } from "./SiteChrome";

const RI_EVIDENCE = [
  { ref:"RESUME#2", line: 6,  snippet:"Owned the hybrid retrieval path: pgvector cosine search fused with a PostgreSQL full-text arm via reciprocal rank fusion.", cites:["kubernetes","rrf fusion"] },
  { ref:"RESUME#5", line: 11, snippet:"Built the retrieval evaluation harness — 15 synthetic cases reporting Recall@K and MRR per configuration.",                cites:["evaluation"] },
  { ref:"JD#3",     line: 15, snippet:"Must have production experience operating vector databases and tuning retrieval quality.",                                 cites:["vector database"] }
];

export function EvidenceStory(){
  const [active, setActive] = React.useState(0);
  const activeEvidence = RI_EVIDENCE[active];
  const highlights = activeEvidence ? [activeEvidence.line] : [];
  const groups: [string, string[], boolean | null][] = [
    ["Matched", ["rrf fusion","pgvector","evaluation","postgres"], true],
    ["Weak",    ["kubernetes","observability"],                    null],
    ["Missing", ["vector database","reranking"],                   false]
  ];
  return (
    <section id="evidence-engine" data-screen-label="Evidence engine" style={{ padding:"var(--section-y) 44px", borderTop:"1px solid var(--line-1)" }}>
      <div style={{ maxWidth:"var(--max-page)", margin:"0 auto" }}>
        <Reveal><SectionHead eyebrow="Evidence engine"
          title="A score is only useful if you can argue with it"
          body="Every skill claim carries a citation tag that resolves to a retrieved chunk. Unsupported claims are dropped before you ever see them."/></Reveal>
        <div style={{ marginTop:"72px", display:"grid", gridTemplateColumns:"minmax(0,1fr) 360px", gap:"72px", alignItems:"start" }}>
          <Reveal>
            <Card padding={26}>
              <div style={{ display:"flex", alignItems:"baseline", justifyContent:"space-between", gap:"20px" }}>
                <div>
                  <span className="mkt-eyebrow">Analysis 1184 · senior ml engineer</span>
                  <div style={{ marginTop:"12px", fontFamily:"var(--font-display)", fontSize:"var(--fs-title-1)",
                    lineHeight:1.24, color:"var(--fg-1)", maxWidth:"40ch" }}>
                    Strong retrieval and evaluation evidence; thin on production Kubernetes ownership.
                  </div>
                </div>
                <div style={{ textAlign:"right", flex:"0 0 auto" }}>
                  <div style={{ fontFamily:"var(--font-display)", fontSize:"46px", lineHeight:1, color:"var(--fg-1)" }}>78</div>
                  <div style={{ marginTop:"6px", fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono-sm)",
                    letterSpacing:"var(--ls-mono-sm)", color:"var(--fg-5)" }}>match score</div>
                </div>
              </div>
              <div style={{ marginTop:"28px", display:"grid", gridTemplateColumns:"repeat(3,1fr)", gap:"22px",
                paddingTop:"24px", borderTop:"1px solid var(--line-1)" }}>
                {groups.map(([t, items, m], ci) => (
                  <div key={t}>
                    <span className="mkt-eyebrow">{t}</span>
                    <div style={{ marginTop:"14px", display:"flex", flexWrap:"wrap", gap:"7px" }}>
                      {items.map((s, i) => <Reveal key={s} delay={ci * 120 + i * 90} y={6}><Tag matched={m}>{s}</Tag></Reveal>)}
                    </div>
                  </div>
                ))}
              </div>
              <div style={{ marginTop:"26px", paddingTop:"24px", borderTop:"1px solid var(--line-1)" }}>
                <span className="mkt-eyebrow">Recommendation · high impact</span>
                <p style={{ marginTop:"12px", fontSize:"14px", lineHeight:1.65, color:"var(--fg-2)", maxWidth:"62ch" }}>
                  Name the cluster work explicitly under the retrieval role. The job description asks for production
                  Kubernetes ownership and your only mention of it sits in a project bullet without scope or outcome.
                </p>
              </div>
              <div style={{ marginTop:"26px", paddingTop:"24px", borderTop:"1px solid var(--line-1)" }}>
                <span className="mkt-eyebrow">Cited evidence</span>
                <div style={{ marginTop:"16px", display:"grid", gap:"10px" }}>
                  {RI_EVIDENCE.map((e, i) => {
                    const on = i === active;
                    return (
                      <div key={e.ref} onMouseEnter={() => setActive(i)} onFocus={() => setActive(i)} tabIndex={0}
                        style={{ padding:"14px 16px", borderRadius:"var(--r-2)", cursor:"pointer",
                          background: on ? "rgba(76,201,232,.06)" : "rgba(244,247,250,.02)",
                          border:"1px solid " + (on ? "rgba(76,201,232,.34)" : "var(--line-1)"), transition:"var(--t-surface)" }}>
                        <div style={{ display:"flex", gap:"10px", alignItems:"center" }}>
                          <span style={{ fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono)", color: on ? "var(--cyan-400)" : "var(--fg-4)" }}>{e.ref}</span>
                          <span style={{ fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono-sm)", letterSpacing:"var(--ls-mono-sm)", color:"var(--fg-5)" }}>
                            chunk {e.line} · cited by {e.cites.join(" · ")}
                          </span>
                          <span style={{ marginLeft:"auto", color: on ? "var(--cyan-400)" : "var(--fg-5)" }}><Icon name="link-2" size={14}/></span>
                        </div>
                        <p style={{ marginTop:"9px", fontSize:"13px", lineHeight:1.6, color: on ? "var(--fg-1)" : "var(--fg-3)" }}>{e.snippet}</p>
                      </div>
                    );
                  })}
                </div>
              </div>
            </Card>
          </Reveal>
          <Reveal delay={140} style={{ display:"flex", justifyContent:"center", position:"relative" }}>
            <div style={{ position:"sticky", top:"110px" }}>
              <DocumentSheet width={330} lines={20} tilt={-11} highlights={highlights}/>
              <div style={{ marginTop:"20px", textAlign:"center", fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono-sm)",
                letterSpacing:"var(--ls-mono-sm)", color:"var(--fg-5)" }}>
                {activeEvidence?.ref} → chunk {activeEvidence?.line}
              </div>
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}
