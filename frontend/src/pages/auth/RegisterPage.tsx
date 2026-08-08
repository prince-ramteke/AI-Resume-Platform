import { Link } from "react-router-dom";
import { AuthLayout } from "../../components/layout/AuthLayout";

export function RegisterPage() {
  return (
    <AuthLayout
      title="Create account"
      subtitle="The full registration form arrives in M6.2."
    >
      <p className="text-sm text-muted">
        Already have an account?{" "}
        <Link
          to="/login"
          className="font-medium text-accent hover:text-accent-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
        >
          Sign in
        </Link>
        .
      </p>
    </AuthLayout>
  );
}
