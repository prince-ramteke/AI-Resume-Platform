import { Outlet } from "react-router-dom";
import { AppNavbar } from "./AppNavbar";

/**
 * Chrome for every authenticated app page. Just the floating pill navbar and
 * a routed <main>. Includes a skip-link that jumps past the nav.
 */
export function AppShell() {
  return (
    <>
      <a href="#main" className="app-skip">Skip to content</a>
      <AppNavbar variant="app" />
      <main id="main" className="app-main">
        <Outlet />
      </main>
    </>
  );
}
