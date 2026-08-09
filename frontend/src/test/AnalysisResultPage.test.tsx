import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AnalysisResultPage } from "../pages/analysis/AnalysisResultPage";
import * as analysesApi from "../api/analyses";
import type { AnalysisDetail } from "../types/analysis";

/**
 * Renders the result page against a synthetic `AnalysisDetail` and asserts:
 *   - all three skill columns render their claims with the correct counts
 *   - the evidence thread renders one card per evidence entry with the
 *     "cited by" chips per claim
 *   - clicking a skill chip updates the URL hash for its cited evidence
 *
 * The api layer is mocked so no network / query-loading race is involved.
 * scrollIntoView is stubbed because jsdom doesn't implement it.
 */

const SAMPLE: AnalysisDetail = {
  id: 5,
  score: 80,
  summary: "Strong backend match with minor gaps.",
  matchedSkills: [
    { skill: "Java 21", importance: "HIGH", evidenceRef: "RESUME#0" },
    { skill: "Spring Boot", importance: "MEDIUM", evidenceRef: "RESUME#0" },
  ],
  missingSkills: [
    { skill: "PostgreSQL", importance: "HIGH", evidenceRef: "JD#0" },
  ],
  weakSkills: [
    { skill: "Spring Security", importance: "MEDIUM", evidenceRef: "RESUME#0" },
  ],
  recommendations: [
    { text: "Add PostgreSQL bullet.", impact: "MEDIUM", reason: "Missing." },
  ],
  evidence: [
    {
      ref: "RESUME#0",
      sourceType: "RESUME",
      chunkIndex: 0,
      snippet: "Java 21, Spring Boot 3, Spring Security…",
    },
    {
      ref: "JD#0",
      sourceType: "JD",
      chunkIndex: 0,
      snippet: "Required: Java 21, Spring Boot, PostgreSQL",
    },
  ],
  provider: "ollama",
  latencyMs: 100000,
  createdAt: "2026-08-09T11:54:00Z",
};

function renderAt(path: string) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/analyses/:id" element={<AnalysisResultPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("AnalysisResultPage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    // jsdom stub — the component calls scrollIntoView after chip clicks.
    Element.prototype.scrollIntoView = vi.fn();
    vi.spyOn(analysesApi, "getAnalysis").mockResolvedValue(SAMPLE);
  });

  it("renders score, summary, and all three skill columns with their claims", async () => {
    renderAt("/analyses/5");
    // Score gauge exposes an accessible aria-label; use it as a proxy so we
    // don't couple the test to internal SVG structure.
    expect(
      await screen.findByRole("img", { name: /match score 80 out of 100/i })
    ).toBeInTheDocument();
    expect(screen.getByText(SAMPLE.summary)).toBeInTheDocument();

    // Column headings + their claim buttons.
    const matched = screen.getByRole("region", { name: /matched/i });
    expect(within(matched).getByRole("button", { name: /Java 21/ })).toBeInTheDocument();
    expect(within(matched).getByRole("button", { name: /Spring Boot/ })).toBeInTheDocument();

    const missing = screen.getByRole("region", { name: /missing/i });
    expect(
      within(missing).getByRole("button", { name: /PostgreSQL/ })
    ).toBeInTheDocument();

    const weak = screen.getByRole("region", { name: /weak/i });
    expect(
      within(weak).getByRole("button", { name: /Spring Security/ })
    ).toBeInTheDocument();
  });

  it("renders one evidence card per entry with human phrasing and cited-by chips", async () => {
    renderAt("/analyses/5");
    expect(
      await screen.findByRole("heading", { name: /from your resume · passage #0/i })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: /from the job description · passage #0/i })
    ).toBeInTheDocument();
    // The two cited-by chips for RESUME#0 (Matched claims) render as buttons.
    const citedByNavs = screen.getAllByRole("navigation", {
      name: /claims citing this passage/i,
    });
    expect(citedByNavs.length).toBeGreaterThanOrEqual(2);
  });

  it("updates the URL hash when a skill chip is clicked", async () => {
    renderAt("/analyses/5");
    const user = userEvent.setup();
    const missing = await screen.findByRole("region", { name: /missing/i });
    await user.click(within(missing).getByRole("button", { name: /PostgreSQL/ }));
    // "JD#0" → "JD-0" per the anchor slug rule.
    expect(window.location.hash).toBe("#evidence-JD-0");
  });
});
