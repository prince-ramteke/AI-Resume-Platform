import { Link } from "react-router-dom";
import { PageHeader } from "../components/layout/PageHeader";
import { Card } from "../components/ui";
import { useAuth } from "../hooks/useAuth";

const ENTRIES = [
  {
    to: "/resumes",
    title: "Resumes",
    description: "Upload and manage the resumes you analyze.",
  },
  {
    to: "/job-descriptions",
    title: "Job Descriptions",
    description: "Paste or upload the roles to match against.",
  },
  {
    to: "/analyses/new",
    title: "New Analysis",
    description: "Score a resume against a job description.",
  },
  {
    to: "/analyses",
    title: "History",
    description: "Revisit past analyses and their evidence.",
  },
];

export function DashboardPage() {
  const { user } = useAuth();

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description={
          user ? `Signed in as ${user.email}.` : "Welcome back."
        }
      />
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {ENTRIES.map((entry) => (
          <Link
            key={entry.to}
            to={entry.to}
            className="rounded-card focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg"
          >
            <Card interactive className="h-full">
              <h3 className="font-medium text-ink">{entry.title}</h3>
              <p className="mt-1.5 text-sm text-muted">{entry.description}</p>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
