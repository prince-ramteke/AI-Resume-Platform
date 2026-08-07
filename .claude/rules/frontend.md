# Rule: Frontend

Always-on constraints for the React app.

## Always
- React + Vite + TypeScript. Functional components + hooks only.
- Put all HTTP calls in `src/api/` as typed functions; components use hooks that call them.
- Type everything. Define shared types in `src/types/`. No `any`.
- Attach the JWT via an Axios interceptor; store it in memory/context (not scattered).
- Guard authenticated routes with a protected-route wrapper.
- Keep components small and presentational; lift logic into hooks.
- Show loading, empty, and error states for every async view.

## Never
- Never call Axios directly inside a component.
- Never use class components.
- Never hardcode the API URL — read `VITE_API_BASE_URL`.
- Never store secrets in the frontend; the bundle is public.

## UI scope (v1)
Auth (register/login), resume upload, JD input, run-analysis, results (score gauge + skill chips + ranked recommendations + expandable evidence), history list/detail. Styling: **Tailwind CSS** (decided — do not use MUI).

## Work that belongs here
React pages/components, forms, client state, routing, the typed API layer, styling/theming, loading/empty/error states, accessibility.

## Skills for this area
- **Auto-consult:** `frontend-design` (visual direction), `ui-styling` (Tailwind/shadcn components). Also read `rules/api` (to match the contract) and `rules/testing`.
- **Task-specific:** `impeccable` when polishing/critiquing an existing screen; `ui-ux-pro-max` when choosing palettes/layout/motion/a11y patterns.
- **Verify before done:** `superpowers:verification-before-completion`.
- **Ignore:** backend/database/deployment skills and brand/marketing design skills. Frontend changes should not drag in server-side guidance unless the API contract itself is changing.
