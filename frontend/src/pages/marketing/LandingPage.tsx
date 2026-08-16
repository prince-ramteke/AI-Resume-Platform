import React from "react";
import { SiteNav, SiteFooter } from "./SiteChrome";
import { Hero } from "./Hero";
import { AtsStory } from "./AtsStory";
import { PipelineStory } from "./PipelineStory";
import { EvidenceStory } from "./EvidenceStory";
import { Showcase } from "./Showcase";
import { FinalCta } from "./FinalCta";

/* Resolve initial theme before first paint to avoid a dark-flash on refresh.
   Light is the first-visit default for the whole site; a stored ri-theme in
   localStorage overrides it. OS prefers-color-scheme is intentionally NOT
   consulted — the product ships light-first and remembers the user's choice. */
function resolveInitialTheme(): "light" | "dark" {
  if (typeof window === "undefined") return "light";
  try {
    const stored = localStorage.getItem("ri-theme");
    if (stored === "light" || stored === "dark") return stored;
  } catch {}
  return "light";
}

export default function LandingPage(){
  const [theme] = React.useState<"light" | "dark">(resolveInitialTheme);
  return (
    <div className="mkt-root" data-theme={theme} style={{ minHeight:"100vh", overflowX:"hidden" }}>
      <SiteNav/>
      <Hero/>
      <AtsStory/>
      <PipelineStory/>
      <EvidenceStory/>
      <Showcase/>
      <FinalCta/>
      <SiteFooter/>
    </div>
  );
}
