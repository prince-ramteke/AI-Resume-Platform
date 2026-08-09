# M6 Frontend — Design Spec

## AI Resume Intelligence Platform

> Design source of truth for the **M6 frontend milestone** (the React application).
> Scope: the authenticated job-seeker flow — register → log in → upload resume → add
> job description → run analysis → read an evidence-grounded result → revisit history.
> This spec is authored under `.claude/rules/frontend.md`, `rules/api.md`,
> `rules/security.md`, and `rules/testing.md`, which outrank it on any conflict.

**Status:** approved (2026-08-08). Implementation proceeds sub-milestone by sub-milestone
(M6.1 → M6.7), each stopping for review; commits happen only after an approved slice.

---

## 0. Milestone numbering note

`docs/ROADMAP.md` labels the frontend as **M4**. This project's working numbering (git
history: backend shipped as M0–M5) calls the frontend **M6**. This spec uses **M6**. A
one-line reconciliation note should be added to `ROADMAP.md` during M6.7 so the docs stop
diverging. This is a documentation cleanup, not a scope change.

---

## 1. Locked decisions (do not re-open)

1. **JWT storage: memory/context only.** No `localStorage`/`sessionStorage` (the bundle is
   public; strict `rules/frontend` compliance). Accepted consequence: a page refresh logs
   the user out and returns them to `/login`. Mitigated by a `?next=<path>` redirect so they
   land back where they were after re-login.
2. **Server state: TanStack Query is the only new runtime dependency.** Forms are
   hand-rolled with a `lib/validators.ts` mirroring backend Bean Validation. Server `400`
   messages are shown as a **form-level banner** (single joined `message` string, not a
   field map).
3. **Admin metrics UI: deferred (later stretch).** M6 covers the job-seeker flow only. A
   `RoleRoute` guard is still built so an ADMIN screen can drop in later without rework.
4. **Design tokens approved before shell code.** M6.1 opens with a design-token proposal
   (4–6 hex tokens, display/body/mono type pairing, the "evidence thread" signature) for
   sign-off **before** any shell is built.

### Confirmed at architecture review (2026-08-08)

5. **Resume/JD UI depth: full CRUD** — upload/create, paginated list, detail, replace/edit,
   delete, download. Exercises every endpoint and gives a real dashboard feel.
6. **Frontend tests added in M6.7** — Vitest + React Testing Library (named in
   `TECH_STACK.md`, not yet installed).
7. **Root route `/` redirects** to `/dashboard` when authenticated, else `/login`. The
   static `LandingPage` is retired; there is no public marketing landing in v1.
8. **Design tokens first in M6.1** (reaffirms #4).

---

## 2. Existing baseline (what M6 starts from)

A bare M0 scaffold under `frontend/`:

- Vite 6, React **19**, TypeScript 5.7, Tailwind **v4** (`@tailwindcss/vite`, CSS-first
  `@import "tailwindcss"`), React Router **7**, Axios 1.7. `@` → `src` alias configured.
  Dev proxy `/api` → `http://localhost:8080`.
- `src/api/client.ts` — one bare Axios instance, **no interceptors**.
- `src/types/index.ts` — only `ErrorResponse`.
- `src/App.tsx` / `main.tsx` — single route `/` → `LandingPage`; `BrowserRouter` at root.
- `src/pages/LandingPage.tsx` — static health-check page (retired in M6.1).
- `Dockerfile` + `nginx.conf` — production build path already wired.

Everything else (auth, domain UI, app shell, data layer, UX-state primitives, forms,
tests, `.env.example`) is new work.

---

## 3. Product goals

A recruiter/job-seeker with **zero API knowledge** can click through the entire flow in the
browser. The app reads like a real SaaS dashboard: a persistent app shell with navigation,
consistent card/table/layout patterns, and first-class loading / empty / error states on
every async view.

---

## 4. Information architecture & routes

Two zones: **public** (auth) and an **authenticated app shell** (persistent sidebar +
topbar) via `<Outlet/>`.

```
/login, /register                      public; redirect to /dashboard if already authed
/                                      redirect → /dashboard (authed) or /login
/app                                   ProtectedRoute → AppLayout
  /dashboard                           overview + entry points
  /resumes            /resumes/:id     list + detail (full CRUD)
  /job-descriptions   /job-descriptions/:id   list (with ?search=) + detail (full CRUD)
  /analyses/new                        run flow (pick resume + JD)
  /analyses           /analyses/:id    history list + result detail (shared view)
  /admin/metrics      RoleRoute(ADMIN) route reserved; screen deferred
*                                      NotFound
```

Route paths may be flattened (e.g. `/resumes` rather than `/app/resumes`) during M6.1
implementation as long as the guard/layout structure holds; the exact prefix is settled in
the M6.1 plan.

---

## 5. Page list

Login, Register, Dashboard, ResumeList, ResumeDetail, JobDescriptionList,
JobDescriptionDetail, NewAnalysis, AnalysisHistory, AnalysisResult (shared by run +
history), NotFound. (AdminMetrics — deferred.)

---

## 6. Component hierarchy

```
main.tsx → QueryClientProvider → AuthProvider → BrowserRouter → App(<Routes>)
  guards:   ProtectedRoute, RoleRoute
  layout:   AppLayout ( Sidebar, Topbar(user menu, logout), <Outlet/> )
  ui/ primitives: Button, Input, TextArea, Card, Badge/Chip, Table, Spinner,
                  EmptyState, ErrorState, Alert/Banner, Modal, FileDropzone, Pagination
  analysis/ feature components: ScoreGauge, SkillChips (matched/missing/weak),
                  RecommendationList, EvidenceAccordion ("evidence thread" signature)
  forms (composed from ui/): ResumeUploadForm, JobDescriptionForm (paste|upload tabs),
                  AnalysisRunForm (resume + JD pickers)
```

Pages are composed from `ui/` primitives — no bespoke one-off styling in page files.

---

## 7. State management

Deliberately minimal — **no Redux/Zustand**.

- **Server state:** TanStack Query (queries, mutations, cache, invalidation, retry) — the
  only new runtime dependency.
- **Auth/session state:** a small React Context (`AuthProvider`) holding
  `{ token, user, login, logout }` in memory.
- **Local UI state:** `useState` / `useReducer` inside hooks. No global store.

---

## 8. API integration

One typed module per feature under `src/api/`, each importing the shared `client`:

| Module | Endpoints |
|---|---|
| `auth.ts` | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| `resumes.ts` | `POST/GET /api/resumes`, `GET/PUT/DELETE /api/resumes/{id}`, `GET /api/resumes/{id}/download` |
| `jobDescriptions.ts` | `POST /api/job-descriptions`, `POST /api/job-descriptions/upload`, `GET` (with `?search=`), `GET/PUT/DELETE /{id}`, `GET /{id}/download` |
| `analyses.ts` | `POST /api/analyses`, `GET /api/analyses`, `GET /api/analyses/{id}` |

TanStack Query hooks in `src/hooks/` wrap these (`useResumes`, `useRunAnalysis`, …).
Domain types in `src/types/` mirror the DTOs exactly, including the Spring `Page<T>`
envelope. `api/errors.ts` exposes `parseApiError(err)` to normalize the standard error
envelope.

### Contract facts the UI must honor

- **Validation `400`s** return a single joined `message` string → render as a **form-level
  banner**, not per-field server errors.
- **Not-owner returns `404`, not `403`** (enumeration defense) → a detail-page `404` renders
  a "not found / no longer available" empty state, **never** a permissions error.
- **`POST /api/analyses` is synchronous and genuinely slow** — a real end-to-end
  measurement on the local Ollama + `llama3.1:8b` stack was **~129 seconds** for one
  analysis (2026-08-09). Treat "up to a couple of minutes" as the honest expectation, not
  seconds. That call gets a **raised Axios timeout (240 s)**, a blocking progress state,
  and explicit `422` handling.
- Auth: login → `{ accessToken, tokenType, expiresAt }`; `/me` → `{ id, email, role }`;
  1-hour TTL; **no refresh token** in v1.
- Pagination: `?page=&size=&sort=` → Spring `Page<T>` response.

---

## 9. Auth handling

- Token lives in `AuthContext` memory only.
- **Request interceptor** attaches `Authorization: Bearer <token>`.
- **Response interceptor** catches `401`, clears auth, redirects to `/login?next=<path>`.
- Login flow: `POST /login` → store token → `GET /me` for `{id,email,role}`; `role` drives
  `RoleRoute`.
- The analysis run call overrides the default timeout.

---

## 10. UX states

- **Loading:** route-level skeletons/spinners on first load; inline spinners + disabled
  controls on mutations; a distinct blocking "Analyzing… (this can take a few seconds)"
  state for the synchronous run.
- **Empty:** dedicated `EmptyState` for zero resumes/JDs/analyses, each with a primary CTA
  ("Upload your first resume", "Run your first analysis"). Detail `404` → not-found empty
  state (never a permissions error).
- **Error:** client validation inline on inputs; server `400` as a form-level banner (joined
  `message`); `422` on the run screen with retry; network/`500` via reusable `ErrorState`
  with retry; a top-level ErrorBoundary for render crashes.

---

## 11. Responsive & accessibility

- **Responsive:** mobile-first Tailwind. Sidebar collapses to a drawer/topbar under `md`;
  tables scroll inside `overflow-x-auto`; grids reflow
  (`grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`); forms single-column on mobile.
- **Accessibility:** labeled inputs (`<label htmlFor>`), visible focus rings,
  keyboard-operable menus/modals/accordions, `aria-live` for async status (analysis
  progress, banners), ≥4.5:1 contrast on tokens, semantic landmarks
  (`header`/`nav`/`main`). Radix primitives (via the `ui-styling` skill) provide correct
  dialog/menu/accordion a11y.

---

## 12. Tailwind design system

Tailwind v4, CSS-first. Tokens declared with `@theme` in `index.css`:

- Palette: 4–6 hex tokens — a neutral scale + one brand accent + semantic
  success/warn/danger.
- Type: a display/body/mono pairing.
- Spacing/radius scale.

Components consume **semantic classes**, never raw hex values. The token set is **proposed
and signed off at the start of M6.1 before any shell code** (locked decision #4/#8). The
exact `@theme` syntax is verified against current Tailwind v4 docs at M6.1 kickoff (there is
no `tailwind.config.js` in this setup).

---

## 13. Data fetching / caching

TanStack Query v5:

- `useQuery` for lists/details with sane `staleTime`.
- `useMutation` for upload/create/replace/delete/run, with
  `queryClient.invalidateQueries` on success (e.g. running an analysis invalidates the
  history list; deleting a resume invalidates the resume list).
- Pagination via the Spring `Page` params.
- **Pre-req:** verify TanStack Query v5 supports React 19 before adding the dependency.

---

## 14. Form validation

Hand-rolled forms + `src/lib/validators.ts` mirroring backend Bean Validation:

| Field | Rule |
|---|---|
| email | valid format, not blank |
| password | ≥8 chars, ≥1 letter + ≥1 digit |
| JD title | not blank, ≤255 |
| JD rawText | not blank, ≤50 000 chars |
| resume file | PDF/DOCX, ≤10 MB (checked client-side before upload) |
| JD file | PDF/DOCX/TXT, ≤10 MB |

Client validation is UX only; the server remains the source of truth, and its `message` is
surfaced on rejection.

---

## 15. Testing (M6.7)

Vitest + React Testing Library (to be installed). Deterministic, no network. Coverage:

- **API layer** — request shape, auth header attachment, `401` interceptor behavior, error
  normalization (Axios mocked).
- **`validators.ts`** — unit tests for each rule.
- **Key components/flows** — `ProtectedRoute` redirect + `?next=`, a form's validation +
  server-error banner, `AnalysisResult` rendering matched/missing/weak chips + evidence
  accordion.

---

## 16. File tree

```
frontend/
├── .env.example                      # VITE_API_BASE_URL
├── src/
│   ├── main.tsx                      # providers: QueryClient → Auth → Router
│   ├── App.tsx                       # <Routes> only
│   ├── index.css                     # Tailwind v4 @theme tokens (signed off M6.1)
│   ├── api/
│   │   ├── client.ts                 # Axios instance + request/response interceptors
│   │   ├── auth.ts  resumes.ts  jobDescriptions.ts  analyses.ts
│   │   └── errors.ts                 # parseApiError(envelope)
│   ├── types/
│   │   ├── index.ts                  # ErrorResponse, Page<T>, Role
│   │   ├── auth.ts  resume.ts  jobDescription.ts  analysis.ts
│   ├── lib/
│   │   └── validators.ts             # mirrors backend Bean Validation
│   ├── context/
│   │   └── AuthContext.tsx           # in-memory token + user
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   ├── useResumes.ts  useJobDescriptions.ts  useAnalyses.ts
│   ├── routes/
│   │   ├── ProtectedRoute.tsx  RoleRoute.tsx
│   ├── components/
│   │   ├── ui/                        # Button, Input, TextArea, Card, Badge, Table,
│   │   │                              # Spinner, EmptyState, ErrorState, Alert, Modal,
│   │   │                              # FileDropzone, Pagination
│   │   ├── layout/                    # AppLayout, Sidebar, Topbar
│   │   └── analysis/                  # ScoreGauge, SkillChips, RecommendationList,
│   │                                  # EvidenceAccordion
│   ├── pages/
│   │   ├── auth/     LoginPage.tsx  RegisterPage.tsx
│   │   ├── DashboardPage.tsx
│   │   ├── resumes/ ResumeListPage.tsx  ResumeDetailPage.tsx
│   │   ├── jobs/    JobDescriptionListPage.tsx  JobDescriptionDetailPage.tsx
│   │   ├── analysis/ NewAnalysisPage.tsx  AnalysisHistoryPage.tsx  AnalysisResultPage.tsx
│   │   └── NotFoundPage.tsx
│   └── test/                          # setup + shared test utils (M6.7)
├── vitest.config.ts                  # (M6.7)
```

---

## 17. Sub-milestone breakdown

Each slice stops for review; commit only after approval.

- **M6.1 — Design + app shell.** Token proposal & sign-off → `index.css` `@theme`; add
  TanStack Query (React-19 compat-verified); `.env.example`; core `ui/` primitives
  (Button/Input/Card/Spinner/EmptyState/ErrorState/Alert); `AppLayout` + Sidebar/Topbar;
  router restructure + `/` redirect; `ProtectedRoute` + `RoleRoute` + `AuthContext`
  skeleton; interceptors in `client.ts`; retire `LandingPage`.
- **M6.2 — Auth.** Login + Register pages; `api/auth.ts`; `useAuth`; wire `AuthContext`,
  `401` redirect + `?next=`, `/me` on login; email/password validators.
- **M6.3 — Resume management (full CRUD).** `api/resumes.ts` + hooks; paginated list;
  upload form (`FileDropzone` + client file validation); detail; replace/edit; delete;
  download.
- **M6.4 — Job description management (full CRUD).** `api/jobDescriptions.ts` + hooks; list
  with `?search=`; create form (paste | upload tabs); detail; edit; delete; download.
- **M6.5 — Analysis creation.** `NewAnalysisPage` (resume + JD pickers); `useRunAnalysis`
  mutation with raised timeout; blocking progress UX; `422`/error handling; redirect to
  result on success.
- **M6.6 — History + result detail.** `AnalysisHistoryPage` (paginated summaries);
  `AnalysisResultPage` (`ScoreGauge`, matched/missing/weak `SkillChips`,
  `RecommendationList`, `EvidenceAccordion`) — shared by run + history.
- **M6.7 — Polish, a11y, tests.** `impeccable` polish pass; responsive/dark-mode check;
  accessibility sweep; install + write Vitest/RTL tests; update `README`/docs; ROADMAP
  numbering note.

---

## 18. Skill usage map

| When | Skill(s) | Why |
|---|---|---|
| Planning | `superpowers:brainstorming` → `writing-plans` | Repo process for new features |
| M6.1 tokens/shell | `frontend-design`, `ui-styling` | Visual direction + Tailwind/Radix primitives |
| M6.2–M6.6 components | `ui-styling` | Accessible styled components against the contract |
| Design decisions (palette/a11y/layout) | `ui-ux-pro-max` (sparingly) | Palette/motion/a11y patterns |
| M6.7 polish | `impeccable` | Critique/polish existing screens |
| Before each slice "done" | `verification-before-completion`, `engineering:code-review` | Evidence + security/correctness gate |
| Always underneath | `rules/frontend`, `rules/api`, `rules/security`, `rules/testing` | Repo rules outrank any skill |

**Not used:** backend/database/deployment skills, brand/marketing design, doc-format skills
(docx/pdf/pptx/xlsx), and personal `resume-*` job-search skills — out of scope per skill
routing map §5.

---

## 19. Risks & mitigations

| Risk | Mitigation |
|---|---|
| TanStack Query × React 19 compatibility | Verify before adopting; pin a known-good v5; fallback to plain hooks around fetch if incompatible |
| In-memory token → refresh logs out | Accepted; `?next=` redirect returns the user to their path |
| Synchronous slow analysis (~129 s measured on local Ollama, not "a few seconds") | Raised Axios timeout (240 s) on that call; long-running blocking progress UX with elapsed timer, honest "up to a couple of minutes" copy, and a client-side "Stop waiting" abort (backend request still finishes server-side); explicit `422` handling |
| Tailwind v4 is new (`@theme`, no config file) | Confirm token approach against v4 docs at M6.1 |
| CRUD scope creep | Full CRUD is scoped intentionally; no features beyond the documented endpoints |
| 404-as-not-found ambiguity | Codify in `parseApiError`/detail pages so 404 never reads as a permissions error |

---

## 20. Definition of Done (M6 overall)

- The full flow works in the browser with no API knowledge (register → analysis → history).
- Every async view has loading, empty, and error states.
- No `any`; all HTTP in `src/api/`; no Axios calls inside components; token never persisted.
- Vitest/RTL suite green (M6.7).
- `README` + affected `docs/*` updated; `.env.example` present; ROADMAP numbering note added.
- Backend, database, and API contracts unchanged.
