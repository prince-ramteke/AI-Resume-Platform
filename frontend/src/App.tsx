import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./hooks/useAuth";
import LandingPage from "./pages/marketing/LandingPage";
import { ProtectedRoute } from "./routes/ProtectedRoute";
import { PublicOnlyRoute } from "./routes/PublicOnlyRoute";
import { RoleRoute } from "./routes/RoleRoute";
import { AppLayout } from "./components/layout/AppLayout";
import { LoginPage } from "./pages/auth/LoginPage";
import { RegisterPage } from "./pages/auth/RegisterPage";
import { DashboardPage } from "./pages/DashboardPage";
import { ResumeListPage } from "./pages/resumes/ResumeListPage";
import { ResumeDetailPage } from "./pages/resumes/ResumeDetailPage";
import { JobDescriptionListPage } from "./pages/jobDescriptions/JobDescriptionListPage";
import { JobDescriptionDetailPage } from "./pages/jobDescriptions/JobDescriptionDetailPage";
import { NewAnalysisPage } from "./pages/analysis/NewAnalysisPage";
import { AnalysisHistoryPage } from "./pages/analysis/AnalysisHistoryPage";
import { AnalysisResultPage } from "./pages/analysis/AnalysisResultPage";
import { PlaceholderPage } from "./pages/PlaceholderPage";
import { NotFoundPage } from "./pages/NotFoundPage";

function App() {
  const { isAuthenticated } = useAuth();
  return (
    <Routes>
      <Route
        path="/"
        element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <LandingPage />}
      />

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
          <Route path="/job-descriptions" element={<JobDescriptionListPage />} />
          <Route
            path="/job-descriptions/:id"
            element={<JobDescriptionDetailPage />}
          />
          <Route path="/analyses/new" element={<NewAnalysisPage />} />
          <Route path="/analyses" element={<AnalysisHistoryPage />} />
          <Route path="/analyses/:id" element={<AnalysisResultPage />} />

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
