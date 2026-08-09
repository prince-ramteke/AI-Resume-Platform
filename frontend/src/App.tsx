import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./routes/ProtectedRoute";
import { PublicOnlyRoute } from "./routes/PublicOnlyRoute";
import { RoleRoute } from "./routes/RoleRoute";
import { AppLayout } from "./components/layout/AppLayout";
import { LoginPage } from "./pages/auth/LoginPage";
import { RegisterPage } from "./pages/auth/RegisterPage";
import { DashboardPage } from "./pages/DashboardPage";
import { ResumeListPage } from "./pages/resumes/ResumeListPage";
import { ResumeDetailPage } from "./pages/resumes/ResumeDetailPage";
import { PlaceholderPage } from "./pages/PlaceholderPage";
import { NotFoundPage } from "./pages/NotFoundPage";

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />

      {/* Public, auth-only pages */}
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      {/* Authenticated app shell */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/resumes" element={<ResumeListPage />} />
          <Route path="/resumes/:id" element={<ResumeDetailPage />} />
          <Route
            path="/job-descriptions"
            element={<PlaceholderPage title="Job Descriptions" slice="M6.5" />}
          />
          <Route
            path="/analyses/new"
            element={<PlaceholderPage title="New Analysis" slice="M6.6" />}
          />
          <Route
            path="/analyses"
            element={<PlaceholderPage title="History" slice="M6.7" />}
          />

          {/* Reserved for a later admin area — no screen ships in M6. */}
          <Route element={<RoleRoute role="ADMIN" />}>
            <Route
              path="/admin/metrics"
              element={<PlaceholderPage title="Admin metrics" slice="a later milestone" />}
            />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

export default App;
