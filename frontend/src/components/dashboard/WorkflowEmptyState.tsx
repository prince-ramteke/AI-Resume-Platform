import { Link } from "react-router-dom";
import { EvidenceThread } from "./EvidenceThread";

const STEPS = [
  {
    n: 1,
    to: "/resumes",
    title: "Upload a resume",
    description: "Add the resume you want to evaluate.",
  },
  {
    n: 2,
    to: "/job-descriptions",
    title: "Add a job description",
    description: "Paste or upload the role to match against.",
  },
  {
    n: 3,
    to: "/analyses/new",
    title: "Run an analysis",
    description: "Get an evidence-backed match score and recommendations.",
  },
] as const;

/**
 * First-run guidance shown when the account has no resumes, job descriptions,
 * or analyses yet. Lays out the core product loop as three concrete next steps
 * under the evidence-thread motif. Each step links to its (future) section.
 */
export function WorkflowEmptyState() {
  return (
    <section
      aria-labelledby="workflow-heading"
      className="rounded-card border border-border bg-surface px-6 py-8"
    >
      <h2 id="workflow-heading" className="font-display text-xl text-ink">
        Get started in three steps
      </h2>
      <p className="mt-1.5 text-sm text-muted">
        Turn a resume and a job description into an evidence-backed match.
      </p>

      <div className="mt-5">
        <EvidenceThread />
      </div>

      <ol className="mt-6 grid gap-3 sm:grid-cols-3">
        {STEPS.map((step) => (
          <li key={step.n}>
            <Link
              to={step.to}
              className="flex h-full flex-col rounded-card border border-border bg-bg px-4 py-4 transition-colors hover:border-muted/40 hover:bg-surface-sunken focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-surface"
            >
              <span className="font-mono text-xs text-accent">
                Step {step.n}
              </span>
              <span className="mt-1 font-medium text-ink">{step.title}</span>
              <span className="mt-1 text-sm text-muted">
                {step.description}
              </span>
            </Link>
          </li>
        ))}
      </ol>
    </section>
  );
}
