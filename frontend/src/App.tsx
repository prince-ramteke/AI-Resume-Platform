import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import { useAuth } from "./hooks/useAuth";
import LandingPage from "./pages/marketing/LandingPage";
import { ProtectedRoute } from "./routes/ProtectedRoute";
import { PublicOnlyRoute } from "./routes/PublicOnlyRoute";
import { RoleRoute } from "./routes/RoleRoute";
import { PlaceholderPage } from "./pages/PlaceholderPage";
import { NotFoundPage } from "./pages/NotFoundPage";

import { AppRoot } from "./app/AppRoot";
import { AppShell } from "./app/shell/AppShell";
import { LoginPage } from "./app/pages/auth/LoginPage";
import { RegisterPage } from "./app/pages/auth/RegisterPage";
import { DashboardPage } from "./app/pages/DashboardPage";
import { ResumeLibraryPage } from "./app/pages/ResumeLibraryPage";
import { ResumeDetailPage } from "./app/pages/ResumeDetailPage";
import { JobLibraryPage } from "./app/pages/JobLibraryPage";
import { JobDetailPage } from "./app/pages/JobDetailPage";
import { NewAnalysisPage } from "./app/pages/NewAnalysisPage";
import { AnalysisResultPage } from "./app/pages/AnalysisResultPage";
import { AnalysisHistoryPage } from "./app/pages/AnalysisHistoryPage";

/** Wrap the auth pages in the app design scope but without the app shell nav. */
function AuthRootLayout() {
  return (
    <AppRoot>
      <main id="main" className="app-main" data-shell="none">
        <Outlet />
      </main>
    </AppRoot>
  );
}

/** Wrap the authenticated tree in the app design scope + floating nav. */
function AppRootLayout() {
  return (
    <AppRoot>
      <AppShell />
    </AppRoot>
  );
}

function App() {
  const { isAuthenticated } = useAuth();
  return (
    <Routes>
      <Route
        path="/"
        element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <LandingPage />}
      />

      {/* Public, auth-only pages — wrapped in the new .app-root scope */}
      <Route element={<PublicOnlyRoute />}>
        <Route element={<AuthRootLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>
      </Route>

      {/* Authenticated app shell — floating pill navbar + main outlet */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppRootLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/resumes" element={<ResumeLibraryPage />} />
          <Route path="/resumes/:id" element={<ResumeDetailPage />} />
          <Route path="/job-descriptions" element={<JobLibraryPage />} />
          <Route path="/job-descriptions/:id" element={<JobDetailPage />} />
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
