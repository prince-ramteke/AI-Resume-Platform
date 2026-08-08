import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { cn } from "../../lib/cn";
import { useAuth } from "../../hooks/useAuth";
import { Badge } from "../ui";
import { ChevronDownIcon, LogoutIcon, MenuIcon } from "./icons";
import { Wordmark } from "./Sidebar";

function UserMenu() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function onPointerDown(e: MouseEvent) {
      if (!containerRef.current?.contains(e.target as Node)) setOpen(false);
    }
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  function handleLogout() {
    setOpen(false);
    logout();
    navigate("/login", { replace: true });
  }

  if (!user) return null;

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="menu"
        aria-expanded={open}
        className="flex items-center gap-2 rounded-control px-2 py-1.5 text-sm text-ink transition-colors hover:bg-surface-sunken focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
      >
        <span className="hidden max-w-[16ch] truncate sm:inline">
          {user.email}
        </span>
        <ChevronDownIcon
          className={cn("text-muted transition-transform", open && "rotate-180")}
        />
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 z-20 mt-2 w-56 rounded-card border border-border bg-surface p-1.5 shadow-sm"
        >
          <div className="px-3 py-2">
            <p className="truncate text-sm font-medium text-ink">{user.email}</p>
            <div className="mt-1">
              <Badge tone="accent" mono>
                {user.role}
              </Badge>
            </div>
          </div>
          <div className="my-1 border-t border-border" />
          <button
            type="button"
            role="menuitem"
            onClick={handleLogout}
            className="flex w-full items-center gap-2 rounded-control px-3 py-2 text-left text-sm text-ink transition-colors hover:bg-surface-sunken focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
          >
            <LogoutIcon className="text-muted" />
            Log out
          </button>
        </div>
      )}
    </div>
  );
}

interface TopbarProps {
  onOpenNav: () => void;
}

export function Topbar({ onOpenNav }: TopbarProps) {
  return (
    <header className="sticky top-0 z-10 flex h-16 items-center gap-3 border-b border-border bg-bg/85 px-4 backdrop-blur md:px-8">
      <button
        type="button"
        onClick={onOpenNav}
        aria-label="Open navigation"
        className="rounded-control p-2 text-ink transition-colors hover:bg-surface-sunken focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent md:hidden"
      >
        <MenuIcon />
      </button>

      <div className="md:hidden">
        <Wordmark />
      </div>

      <div className="ml-auto">
        <UserMenu />
      </div>
    </header>
  );
}
