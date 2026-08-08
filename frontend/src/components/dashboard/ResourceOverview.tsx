import { ResourceCard } from "./ResourceCard";
import { AnalysisIcon, JobIcon, ResumeIcon } from "../layout/icons";

interface Stat {
  count: number | undefined;
  isPending: boolean;
  isError: boolean;
  onRetry: () => void;
}

interface ResourceOverviewProps {
  resumes: Stat;
  jobDescriptions: Stat;
  analyses: Stat;
}

/**
 * At-a-glance counts for the three core resources. Each card loads and retries
 * independently. Static presentation config (label/icon/route/noun) lives here;
 * the page supplies only the query-derived stats.
 */
export function ResourceOverview({
  resumes,
  jobDescriptions,
  analyses,
}: ResourceOverviewProps) {
  return (
    <section aria-labelledby="overview-heading" className="mb-10">
      <h2 id="overview-heading" className="mb-4 font-display text-xl text-ink">
        Your workspace
      </h2>
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
        <ResourceCard
          to="/resumes"
          label="Resumes"
          icon={<ResumeIcon />}
          noun={{ one: "resume", many: "resumes" }}
          {...resumes}
        />
        <ResourceCard
          to="/job-descriptions"
          label="Job Descriptions"
          icon={<JobIcon />}
          noun={{ one: "job description", many: "job descriptions" }}
          {...jobDescriptions}
        />
        <ResourceCard
          to="/analyses"
          label="Analyses"
          icon={<AnalysisIcon />}
          noun={{ one: "analysis", many: "analyses" }}
          {...analyses}
        />
      </div>
    </section>
  );
}
