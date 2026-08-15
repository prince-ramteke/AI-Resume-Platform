import React from "react";
import { Reveal, SectionHead, StageBg, useReveal } from "./Atmosphere";
import { Card, Tag, Icon, PipelineTrack } from "./SiteChrome";
import type { PipelineStep } from "./SiteChrome";

const RI_STAGES = [
  { label:"Parse",    detail:"pdf / docx → text" },
  { label:"Chunk",    detail:"section-aware split" },
  { label:"Embed",    detail:"nomic-embed-text · 768d" },
  { label:"Retrieve", detail:"vector + full-text" },
  { label:"Fuse",     detail:"rrf k=60 · pool 20" },
  { label:"Evidence", detail:"top-k 8 cited chunks" },
  { label:"Verdict",  detail:"llama3.1:8b" }
];

const RI_VECTOR:  [string, string][] = [["0.81","resume#2 · built hybrid retrieval over"],["0.74","resume#5 · owned the eval harness"],["0.66","resume#9 · latency budget work"]];
const RI_KEYWORD: [string, string][] = [["12.4","resume#5 · bm25 · rrf · recall@k"],["9.8","resume#2 · postgres · pgvector"],["7.1","resume#11 · kubernetes · mtls"]];
const RI_FUSED:   [string, string, string][] = [["1","resume#5","0.0328"],["2","resume#2","0.0321"],["3","resume#9","0.0161"]];

function FragmentRail(){
  return (
    <div aria-hidden="true" style={{ position:"relative", height:"46px", marginTop:"34px", overflow:"hidden" }}>
      <div style={{ position:"absolute", left:0, right:0, top:"22px", height:"1px", background:"var(--line-1)" }}/>
      {[0,1,2,3,4,5].map(i => (
        <span key={i} style={{ position:"absolute", top:"14px", left:0, width:"26px", height:"17px",
          borderRadius:"var(--r-paper)", background:"var(--grad-paper)", boxShadow:"var(--shadow-2)", opacity:.9,
          animation: `mkt-travel 7600ms linear ${i * 1180}ms infinite` }}/>
      ))}
    </div>
  );
}

function RankColumn({ title, rows, tone, mono }:{
  title: string; rows: [string, string][]; tone: string; mono: string;
}){
  return (
    <Card tone="flat" padding={18} style={{ minWidth:0 }}>
      <div style={{ display:"flex", alignItems:"center", justifyContent:"space-between", gap:"12px" }}>
        <span className="mkt-eyebrow">{title}</span>
        <span style={{ fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono-sm)", letterSpacing:"var(--ls-mono-sm)", color:"var(--fg-5)" }}>{mono}</span>
      </div>
      <div style={{ marginTop:"16px", display:"grid", gap:"10px" }}>
        {rows.map(([s, t], i) => (
          <Reveal key={t} delay={i * 140} y={8}>
            <div style={{ display:"flex", gap:"12px", alignItems:"baseline", minWidth:0 }}>
              <span style={{ fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono)", color: tone, flex:"0 0 auto", fontVariantNumeric:"tabular-nums" }}>{s}</span>
              <span style={{ fontSize:"12.5px", color:"var(--fg-3)", overflow:"hidden", textOverflow:"ellipsis", whiteSpace:"nowrap" }}>{t}</span>
            </div>
          </Reveal>
        ))}
      </div>
    </Card>
  );
}

export function PipelineStory(){
  const [ref, seen] = useReveal(.35);
  const [step, setStep] = React.useState(0);
  React.useEffect(() => {
    if (!seen) return;
    const reduce = window.matchMedia && window.matchMedia("(prefers-reduced-motion:reduce)").matches;
    if (reduce){ setStep(RI_STAGES.length); return; }
    const t = setInterval(() => setStep(s => s >= RI_STAGES.length ? 1 : s + 1), 900);
    return () => clearInterval(t);
  }, [seen]);
  const steps: PipelineStep[] = RI_STAGES.map((s, i) => ({ ...s, state: i < step - 1 ? "done" : i === step - 1 ? "active" : "pending" }));
  return (
    <section id="how-it-works" data-screen-label="Retrieval pipeline"
      style={{ position:"relative", overflow:"hidden", padding:"var(--section-y) 44px", borderTop:"1px solid var(--line-1)" }}>
      <StageBg veil={false}/>
      <div ref={ref} style={{ position:"relative", maxWidth:"var(--max-page)", margin:"0 auto" }}>
        <Reveal><SectionHead eyebrow="Retrieval pipeline"
          title="Seven stages, all of them inspectable"
          body="Dense vectors catch meaning, PostgreSQL full-text catches exact terms. Reciprocal rank fusion decides which passages reach the verdict — and every one of them is kept as evidence."/></Reveal>
        <Reveal delay={120}><FragmentRail/></Reveal>
        <div style={{ marginTop:"20px" }}><PipelineTrack steps={steps}/></div>
        <div style={{ marginTop:"78px", display:"grid", gridTemplateColumns:"1fr 1fr 34px 1fr", gap:"22px", alignItems:"center" }}>
          <RankColumn title="Vector arm"  mono="cosine"  tone="var(--cyan-400)"  rows={RI_VECTOR}/>
          <RankColumn title="Keyword arm" mono="ts_rank" tone="var(--amber-400)" rows={RI_KEYWORD}/>
          <div style={{ display:"flex", flexDirection:"column", alignItems:"center", gap:"8px", color:"var(--fg-4)" }}>
            <Icon name="git-merge" size={18}/>
            <span style={{ fontFamily:"var(--font-mono)", fontSize:"9.5px", letterSpacing:".14em", textTransform:"uppercase", color:"var(--fg-5)" }}>rrf</span>
          </div>
          <Card padding={18}>
            <div style={{ display:"flex", alignItems:"center", justifyContent:"space-between", gap:"12px" }}>
              <span className="mkt-eyebrow">Fused</span>
              <span style={{ fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono-sm)", letterSpacing:"var(--ls-mono-sm)", color:"var(--fg-5)" }}>k=60</span>
            </div>
            <div style={{ marginTop:"16px", display:"grid", gap:"10px" }}>
              {RI_FUSED.map(([r, src, sc], i) => (
                <Reveal key={src} delay={420 + i * 160} y={8}>
                  <div style={{ display:"flex", gap:"12px", alignItems:"baseline" }}>
                    <span style={{ fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono-sm)", color:"var(--fg-5)" }}>{r}</span>
                    <span style={{ fontSize:"12.5px", color:"var(--fg-1)" }}>{src}</span>
                    <span style={{ marginLeft:"auto", fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono)", color:"var(--cyan-400)", fontVariantNumeric:"tabular-nums" }}>{sc}</span>
                  </div>
                </Reveal>
              ))}
            </div>
          </Card>
        </div>
        <Reveal delay={200} style={{ marginTop:"34px" }}>
          <div style={{ display:"flex", gap:"10px", flexWrap:"wrap", alignItems:"center" }}>
            <Tag>recall@3 +13.33pp</Tag><Tag>mrr +23.89pp</Tag><Tag>15 eval cases</Tag><Tag>zero regressions</Tag>
            <span style={{ fontSize:"12.5px", color:"var(--fg-5)" }}>Hybrid retrieval is on by default since the v1.2 evaluation.</span>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
