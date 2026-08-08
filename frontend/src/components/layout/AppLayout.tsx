import { useEffect, useState } from "react";
import { Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";
import { CloseIcon } from "./icons";

/**
 * Authenticated shell: fixed sidebar on desktop, a slide-in drawer on mobile,
 * and a sticky topbar carrying the user menu. Renders the routed page in <main>.
 */
export function AppLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false);

  // Close the drawer on Escape.
  useEffect(() => {
    if (!drawerOpen) return;
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") setDrawerOpen(false);
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [drawerOpen]);

  return (
    <div className="min-h-screen bg-bg text-ink">
      {/* Desktop sidebar */}
      <Sidebar className="fixed inset-y-0 left-0 z-20 hidden md:flex" />

      {/* Mobile drawer */}
      {drawerOpen && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div
            className="absolute inset-0 bg-ink/40"
            onClick={() => setDrawerOpen(false)}
            aria-hidden="true"
          />
          <div className="absolute inset-y-0 left-0 flex">
            <Sidebar onNavigate={() => setDrawerOpen(false)} />
            <button
              type="button"
              onClick={() => setDrawerOpen(false)}
              aria-label="Close navigation"
              className="m-2 h-10 w-10 self-start rounded-control bg-surface p-2 text-ink shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
            >
              <CloseIcon />
            </button>
          </div>
        </div>
      )}

      <div className="md:pl-60">
        <Topbar onOpenNav={() => setDrawerOpen(true)} />
        <main className="mx-auto max-w-[1140px] px-4 py-8 md:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
