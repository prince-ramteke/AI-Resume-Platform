import { Link } from "react-router-dom";
import { Button } from "../ui";
import { EvidenceThread } from "./EvidenceThread";

/**
 * The dashboard's primary action. Visually strongest element on the page: a
 * prominent ink CTA into the analysis workflow, with the evidence-thread motif
 * showing where it leads. The workflow itself lives in a later milestone.
 */
export function PrimaryActionCard() {
  return (
    <section
      aria-labelledby="primary-action-heading"
      className="mb-10 flex flex-col gap-5 rounded-card border border-border bg-surface px-6 py-6 lg:flex-row lg:items-center lg:justify-between"
    >
      <div className="min-w-0">
        <h2
          id="primary-action-heading"
          className="font-display text-xl text-ink"
        >
          Score a resume against a role
        </h2>
        <div className="mt-3">
          <EvidenceThread />
        </div>
      </div>
      <Link to="/analyses/new" className="shrink-0 rounded-control">
        <Button size="md" className="w-full lg:w-auto">
          Run a new analysis
        </Button>
      </Link>
    </section>
  );
}
