import { useEffect, useState } from "react";
import apiClient from "../api/client";

function LandingPage() {
  const [healthStatus, setHealthStatus] = useState<string>("checking...");

  useEffect(() => {
    apiClient
      .get("/actuator/health", { baseURL: import.meta.env.VITE_API_BASE_URL?.replace("/api", "") || "" })
      .then(() => setHealthStatus("connected"))
      .catch(() => setHealthStatus("unavailable"));
  }, []);

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto max-w-4xl px-6 py-5 flex items-center justify-between">
          <h1 className="text-lg font-semibold tracking-tight">
            Resume<span className="text-indigo-600">AI</span>
          </h1>
          <span className="text-xs text-slate-400">v0.1.0</span>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-6 py-16">
        <div className="space-y-6">
          <h2 className="text-3xl font-bold tracking-tight text-slate-900">
            AI Resume Intelligence Platform
          </h2>
          <p className="text-lg text-slate-600 max-w-2xl">
            Score your resume against any job description. Get a match score,
            skill-gap analysis, and prioritized recommendations — each grounded
            in evidence from the source documents.
          </p>

          <div className="flex items-center gap-3 pt-4">
            <div
              className={`h-2.5 w-2.5 rounded-full ${
                healthStatus === "connected"
                  ? "bg-emerald-500"
                  : healthStatus === "unavailable"
                    ? "bg-red-500"
                    : "bg-amber-400 animate-pulse"
              }`}
            />
            <span className="text-sm text-slate-500">
              Backend: {healthStatus}
            </span>
          </div>
        </div>

        <div className="mt-16 grid gap-6 sm:grid-cols-3">
          {[
            {
              title: "Upload",
              description: "Upload your resume (PDF or DOCX) and paste a job description.",
            },
            {
              title: "Analyze",
              description: "Our RAG pipeline scores your resume against the JD requirements.",
            },
            {
              title: "Improve",
              description: "Get actionable recommendations with evidence from both documents.",
            },
          ].map((step) => (
            <div
              key={step.title}
              className="rounded-lg border border-slate-200 bg-white p-6"
            >
              <h3 className="font-semibold text-slate-900">{step.title}</h3>
              <p className="mt-2 text-sm text-slate-500">{step.description}</p>
            </div>
          ))}
        </div>

        <p className="mt-16 text-sm text-slate-400">
          M0 scaffold — auth, upload, and analysis coming in M1.
        </p>
      </main>
    </div>
  );
}

export default LandingPage;
