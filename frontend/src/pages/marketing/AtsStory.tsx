import { Reveal, SectionHead, useReveal } from "./Atmosphere";
import { Card, Tag, Icon, ScoreDial, MatchBar, DocumentSheet } from "./SiteChrome";

const RI_METRICS: [string, number, "amber" | "cyan" | "pass", string][] = [
  ["Skills coverage", 72, "amber", "18 of 25 jd terms present"],
  ["Section recall", 88, "cyan", "9 of 9 expected sections parsed"],
  ["Format safety", 64, "amber", "1 multi-column table at risk"],
  ["Title alignment", 81, "cyan", "senior ml engineer → ml engineer"]
];

export function AtsStory(){
  const [ref, seen] = useReveal(.3);
  return (
    <section id="ats-analysis" data-screen-label="ATS analysis" style={{ position:"relative", padding:"var(--section-y) 44px", borderTop:"1px solid var(--line-1)" }}>
      <div style={{ maxWidth:"var(--max-page)", margin:"0 auto" }}>
        <Reveal><SectionHead eyebrow="ATS analysis"
          title="What a tracking system sees before a person does"
          body="A parser has no judgement. It reads structure, then it reads terms. We reproduce both passes and score what survives."/></Reveal>
        <div ref={ref} style={{ marginTop:"72px", display:"grid", gridTemplateColumns:"minmax(0,420px) minmax(0,1fr)", gap:"96px", alignItems:"center" }}>
          <div style={{ position:"relative", display:"flex", justifyContent:"center" }}>
            <DocumentSheet width={340} lines={20} tilt={12} scan highlights={[1, 4, 5, 9, 13, 17]}/>
            <div style={{ position:"absolute", right:"-34px", bottom:"58px", display:"flex", flexDirection:"column", gap:"7px", alignItems:"flex-start" }}>
              {["pytorch","kubernetes","bm25"].map((k, i) => (
                <Reveal key={k} delay={420 + i * 220} y={6}><Tag matched>{k}</Tag></Reveal>
              ))}
            </div>
          </div>
          <div style={{ display:"grid", gridTemplateColumns:"210px minmax(0,1fr)", gap:"56px", alignItems:"start" }}>
            <div style={{ paddingTop:"6px" }}>{seen && <ScoreDial value={78} size={196} label="Match score" sublabel="Grounded in 8 retrieved chunks"/>}</div>
            <div style={{ display:"grid", gap:"26px" }}>
              {RI_METRICS.map(([l, v, t, d], i) => (
                <Reveal key={l} delay={240 + i * 160} y={10}>
                  {seen ? <MatchBar label={l} value={v} tone={t} detail={d}/> : null}
                </Reveal>
              ))}
              <Reveal delay={900} y={10}>
                <Card tone="flat" padding={18} style={{ marginTop:"6px" }}>
                  <div style={{ display:"flex", gap:"12px" }}>
                    <span style={{ color:"var(--amber-400)", flex:"0 0 auto", marginTop:"1px" }}><Icon name="file-search" size={16}/></span>
                    <p style={{ fontSize:"13.5px", lineHeight:1.65, color:"var(--fg-2)" }}>
                      The skills table on page two is unreadable. Most parsers drop multi-column tables. Convert it to a
                      comma-separated line under a plain heading.
                    </p>
                  </div>
                </Card>
              </Reveal>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
