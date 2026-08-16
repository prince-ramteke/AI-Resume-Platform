export type NavVariant = "marketing" | "app";

export interface NavLinkSpec {
  label: string;
  to: string;
  /** true → NavLink `end` — only exact match lights up (used for /dashboard). */
  exact?: boolean;
  /** Optional set of extra path prefixes that should also mark this link active. */
  matches?: (pathname: string) => boolean;
}

export interface NavCtaSpec {
  label: string;
  to: string;
}

export const APP_LINKS: NavLinkSpec[] = [
  { label: "Dashboard", to: "/dashboard", exact: true },
  { label: "Resumes", to: "/resumes" },
  { label: "Job Descriptions", to: "/job-descriptions" },
  { label: "New Analysis", to: "/analyses/new", exact: true },
  {
    label: "History",
    to: "/analyses",
    matches: (p) =>
      p === "/analyses" ||
      (p.startsWith("/analyses/") && p !== "/analyses/new"),
  },
];

export const APP_CTA: NavCtaSpec = { label: "New Analysis →", to: "/analyses/new" };

export const MARKETING_LINKS: NavLinkSpec[] = [
  { label: "Product", to: "/#product" },
  { label: "How it works", to: "/#how-it-works" },
  { label: "Evidence", to: "/#evidence" },
  { label: "Docs", to: "/docs" },
];

export const MARKETING_CTA: NavCtaSpec = { label: "Analyze My Resume →", to: "/register" };
