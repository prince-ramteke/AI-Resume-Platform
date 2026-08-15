import { Reveal, StageBg } from "./Atmosphere";
import { Button, Icon, Tag, MatchBar, DocumentSheet } from "./SiteChrome";

export function Hero(){
  return (
    <section data-screen-label="Hero" style={{ position:"relative", overflow:"hidden", padding:"132px 44px 148px", minHeight:"780px" }}>
      <StageBg/>
      <div style={{ position:"relative", maxWidth:"var(--max-page)", margin:"0 auto", display:"grid",
        gridTemplateColumns:"minmax(0,1fr) 470px", gap:"88px", alignItems:"center" }}>
        <Reveal threshold={.05}>
          <span className="mkt-eyebrow">ATS analysis · hybrid retrieval · evidence</span>
          <h1 style={{ marginTop:"24px", fontFamily:"var(--font-display)", fontSize:"var(--fs-display-1)",
            lineHeight:"var(--lh-display-1)", letterSpacing:"var(--ls-display-1)", color:"var(--fg-1)", maxWidth:"19ch" }}>
            Your resume, read the way machines read it
          </h1>
          <p style={{ marginTop:"28px", fontSize:"var(--fs-body-lg)", lineHeight:"var(--lh-body-lg)", color:"var(--fg-2)", maxWidth:"52ch" }}>
            We parse the document under the same limitations a tracking system has, retrieve the passages a job description
            actually asks for, and cite the evidence behind every recommendation.
          </p>
          <div style={{ marginTop:"40px", display:"flex", gap:"14px", alignItems:"center" }}>
            <Button to="/register" size="lg"><Icon name="scan-line" size={17}/>Analyze My Resume</Button>
            <Button href="#how-it-works" as="a" size="lg" variant="outline">See How It Works</Button>
          </div>
          <div style={{ marginTop:"52px", fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono-sm)",
            letterSpacing:"var(--ls-mono-sm)", color:"var(--fg-5)" }}>
            nomic-embed-text 768d · rrf k=60 · top-k 8 · recall@3 +13.33pp
          </div>
        </Reveal>
        <Reveal threshold={.05} delay={160} y={22} style={{ position:"relative", display:"flex", justifyContent:"center" }}>
          <div style={{ animation:"mkt-float 9000ms var(--ease-in-out) infinite" }}>
            <DocumentSheet width={352} lines={18} tilt={-16} scan highlights={[2, 6, 11, 15]}/>
          </div>
          <div style={{ position:"absolute", left:"-72px", bottom:"22px", width:"206px", padding:"16px",
            background:"var(--glass)", border:"1px solid var(--glass-border)", borderRadius:"var(--r-3)",
            backdropFilter:"var(--glass-blur)", WebkitBackdropFilter:"var(--glass-blur)", boxShadow:"var(--shadow-4)" }}>
            <span className="mkt-eyebrow">Scanning</span>
            <div style={{ marginTop:"12px" }}><MatchBar label="Parsed" value={100} detail="9 sections · 0 dropped"/></div>
            <div style={{ marginTop:"14px" }}><MatchBar label="Keywords" value={72} tone="amber" detail="18 of 25 jd terms"/></div>
          </div>
          <div style={{ position:"absolute", right:"-44px", top:"18px", display:"flex", flexDirection:"column", gap:"8px", alignItems:"flex-end" }}>
            {["kubernetes","vector database","bm25"].map((k, i) => (
              <span key={k} style={{ animation: `mkt-keyword 5200ms var(--ease-in-out) ${i * 700}ms infinite` }}>
                <Tag matched>{k}</Tag>
              </span>
            ))}
            <span style={{ marginTop:"4px" }}><Tag matched={false}>reranking</Tag></span>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
