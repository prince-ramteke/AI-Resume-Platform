import { Link } from "react-router-dom";
import { AuthLayout } from "../../components/layout/AuthLayout";
import { Button } from "../../components/ui";
import { useAuth } from "../../hooks/useAuth";

export function LoginPage() {
  const { login } = useAuth();

  return (
    <AuthLayout
      title="Sign in"
      subtitle="The full sign-in form arrives in M6.2."
    >
      <p className="text-sm text-muted">
        Don't have an account?{" "}
        <Link
          to="/register"
          className="font-medium text-accent hover:text-accent-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
        >
          Create one
        </Link>
        .
      </p>

      {/* Dev-only: preview the authenticated shell before the real auth flow
          lands in M6.2. Never included in a production build. */}
      {import.meta.env.DEV && (
        <div className="mt-6 border-t border-border pt-6">
          <Button
            variant="secondary"
            className="w-full"
            onClick={() =>
              login("dev-preview-token", {
                id: 1,
                email: "dev@resume.ai",
                role: "USER",
              })
            }
          >
            Preview app shell (dev only)
          </Button>
          <p className="mt-2 text-center text-xs text-muted">
            Temporary — replaced by real sign-in in M6.2.
          </p>
        </div>
      )}
    </AuthLayout>
  );
}
