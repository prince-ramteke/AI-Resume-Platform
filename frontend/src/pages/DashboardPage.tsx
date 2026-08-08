import { useAuth } from "../hooks/useAuth";
import { useRecentResumes } from "../hooks/useRecentResumes";
import { useRecentJobDescriptions } from "../hooks/useRecentJobDescriptions";
import { useRecentAnalyses } from "../hooks/useRecentAnalyses";
import { DashboardHeader } from "../components/dashboard/DashboardHeader";
import { PrimaryActionCard } from "../components/dashboard/PrimaryActionCard";
import { ResourceOverview } from "../components/dashboard/ResourceOverview";
import { RecentAnalyses } from "../components/dashboard/RecentAnalyses";
import { WorkflowEmptyState } from "../components/dashboard/WorkflowEmptyState";

/**
 * The authenticated user's workspace. Three independent recent-list queries run
 * in parallel; each serves both a count (Page.totalElements) and a recent slice.
 * A brand-new account (everything empty) gets first-run guidance in place of the
 * recent feed. No page-level auth logic — the route guard and the Axios 401
 * interceptor own that.
 */
export function DashboardPage() {
  const { user } = useAuth();
  const resumes = useRecentResumes();
  const jobDescriptions = useRecentJobDescriptions();
  const analyses = useRecentAnalyses();

  const isFirstRun =
    resumes.isSuccess &&
    jobDescriptions.isSuccess &&
    analyses.isSuccess &&
    resumes.data.totalElements === 0 &&
    jobDescriptions.data.totalElements === 0 &&
    analyses.data.totalElements === 0;

  return (
    <div>
      <DashboardHeader email={user?.email} />
      <PrimaryActionCard />
      <ResourceOverview
        resumes={{
          count: resumes.data?.totalElements,
          isPending: resumes.isPending,
          isError: resumes.isError,
          onRetry: resumes.refetch,
        }}
        jobDescriptions={{
          count: jobDescriptions.data?.totalElements,
          isPending: jobDescriptions.isPending,
          isError: jobDescriptions.isError,
          onRetry: jobDescriptions.refetch,
        }}
        analyses={{
          count: analyses.data?.totalElements,
          isPending: analyses.isPending,
          isError: analyses.isError,
          onRetry: analyses.refetch,
        }}
      />

      {isFirstRun ? (
        <WorkflowEmptyState />
      ) : (
        <RecentAnalyses
          analyses={analyses.data?.content}
          isPending={analyses.isPending}
          isError={analyses.isError}
          onRetry={analyses.refetch}
        />
      )}
    </div>
  );
}
