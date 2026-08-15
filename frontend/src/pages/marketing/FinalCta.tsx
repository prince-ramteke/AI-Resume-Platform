import { Reveal, StageBg } from "./Atmosphere";
import { Button, Icon } from "./SiteChrome";

export function FinalCta(){
  return (
    <section data-screen-label="Final CTA"
      style={{ position:"relative", overflow:"hidden", padding:"152px 44px 168px", borderTop:"1px solid var(--line-1)" }}>
      <StageBg/>
      <Reveal threshold={.2} style={{ position:"relative", maxWidth:"760px", margin:"0 auto", textAlign:"center" }}>
        <span className="mkt-eyebrow">Start here</span>
        <h2 style={{ marginTop:"26px", fontFamily:"var(--font-display)", fontSize:"var(--fs-display-2)",
          lineHeight:"var(--lh-display-2)", letterSpacing:"var(--ls-display-2)", color:"var(--fg-1)" }}>
          Your resume, read the way machines read it.
        </h2>
        <div style={{ marginTop:"40px", display:"flex", justifyContent:"center" }}>
          <Button to="/register" size="lg"><Icon name="scan-line" size={17}/>Analyze My Resume</Button>
        </div>
        <div style={{ marginTop:"34px", fontFamily:"var(--font-mono)", fontSize:"var(--fs-mono-sm)",
          letterSpacing:"var(--ls-mono-sm)", color:"var(--fg-5)" }}>
          pdf or docx · up to 10 MB · nothing is shared
        </div>
      </Reveal>
    </section>
  );
}
