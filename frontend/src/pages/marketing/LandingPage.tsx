import React from "react";
import { SiteNav, SiteFooter } from "./SiteChrome";
import { Hero } from "./Hero";
import { AtsStory } from "./AtsStory";
import { PipelineStory } from "./PipelineStory";
import { EvidenceStory } from "./EvidenceStory";
import { Showcase } from "./Showcase";
import { FinalCta } from "./FinalCta";

/* Resolve initial theme before first paint to avoid a light-flash on refresh. */
function resolveInitialTheme(): "light" | "dark" {
  if (typeof window === "undefined") return "dark";
  try {
    const stored = localStorage.getItem("ri-theme");
    if (stored === "light" || stored === "dark") return stored;
  } catch {}
  return window.matchMedia && window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark";
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
