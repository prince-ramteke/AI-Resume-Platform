import { PageHeader } from "../components/layout/PageHeader";
import { EmptyState } from "../components/ui";

interface PlaceholderPageProps {
  title: string;
  slice: string;
}

/**
 * Temporary stub for reserved routes so the shell/navigation is fully
 * navigable in M6.1. Each is replaced by its real screen in a later slice.
 */
export function PlaceholderPage({ title, slice }: PlaceholderPageProps) {
  return (
    <div>
      <PageHeader title={title} />
      <EmptyState
        title={`${title} arrives in ${slice}`}
        description="This screen isn't built yet. The app shell, navigation, and design system are in place for it."
      />
    </div>
  );
}
