import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import { Link, useLocation, useNavigate } from "react-router-dom";
import type { NavCtaSpec, NavLinkSpec } from "./NavLinks";
import { ThemeToggle } from "./ThemeToggle";
import { useAuth } from "../../hooks/useAuth";

interface MobileNavSheetProps {
  open: boolean;
  onClose: () => void;
  links: NavLinkSpec[];
  cta: NavCtaSpec;
  variant: "marketing" | "app";
  returnFocusTo?: React.RefObject<HTMLElement | null>;
}

export function MobileNavSheet({ open, onClose, links, cta, variant, returnFocusTo }: MobileNavSheetProps) {
  const sheetRef = useRef<HTMLDivElement | null>(null);
  const firstFocusRef = useRef<HTMLButtonElement | null>(null);
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { logout, isAuthenticated } = useAuth();

  useEffect(() => {
    if (!open) return;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const mainEl = document.getElementById("main");
    if (mainEl) mainEl.setAttribute("inert", "");

    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
      if (e.key === "Tab") {
        const sheet = sheetRef.current;
        if (!sheet) return;
        const focusables = Array.from(
          sheet.querySelectorAll<HTMLElement>(
            'a, button, input, select, [tabindex]:not([tabindex="-1"])'
          )
        ).filter((el) => !el.hasAttribute("disabled"));
        if (focusables.length === 0) return;
        const first = focusables[0]!;
        const last = focusables[focusables.length - 1]!;
        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    };
    document.addEventListener("keydown", onKey);
    const focusTimer = window.setTimeout(() => firstFocusRef.current?.focus(), 20);
    return () => {
      document.body.style.overflow = prevOverflow;
      if (mainEl) mainEl.removeAttribute("inert");
      document.removeEventListener("keydown", onKey);
      window.clearTimeout(focusTimer);
      returnFocusTo?.current?.focus();
    };
  }, [open, onClose, returnFocusTo]);

  useEffect(() => {
    if (open) onClose();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pathname]);

  if (!open) return null;

  function isActive(link: NavLinkSpec): boolean {
    if (link.matches) return link.matches(pathname);
    if (link.exact) return pathname === link.to;
    return pathname === link.to || pathname.startsWith(link.to + "/");
  }

  const handleAux = () => {
    if (variant === "app" && isAuthenticated) {
      onClose();
      logout();
      navigate("/login", { replace: true });
    } else {
      onClose();
      navigate("/login");
    }
  };

  return createPortal(
    <>
      <div className="app-sheet-backdrop" onClick={onClose} aria-hidden="true" />
      <div
        ref={sheetRef}
        id="app-nav-sheet"
        className="app-sheet"
        role="dialog"
        aria-modal="true"
        aria-label="Primary navigation"
      >
        <div className="app-sheet__header">
          <span className="app-nav__brand">
            <span className="app-nav__brand-mark" aria-hidden="true">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h5" />
                <path d="M14 2v6h6" />
                <circle cx="16.5" cy="16.5" r="2.5" />
                <path d="m21 21-2.5-2.5" />
              </svg>
            </span>
            <span className="app-nav__brand-word">Resume<em>Intelligence</em></span>
          </span>
          <button
            ref={firstFocusRef}
            type="button"
            className="app-nav__hamburger app-nav__hamburger--close"
            style={{ display: "inline-flex" }}
            onClick={onClose}
            aria-label="Close menu"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M18 6 6 18" />
              <path d="m6 6 12 12" />
            </svg>
          </button>
        </div>

        <nav aria-label="Primary">
          {links.map((link, i) => (
            <Link
              key={link.to}
              to={link.to}
              onClick={onClose}
              className="app-sheet__row"
              data-active={isActive(link) ? "true" : undefined}
              style={{ animationDelay: `${60 + i * 40}ms` }}
            >
              <span className="app-sheet__row-label">{link.label}</span>
              <span className="app-sheet__row-idx">
                {String(i + 1).padStart(2, "0")}/{String(links.length).padStart(2, "0")}
              </span>
            </Link>
          ))}
        </nav>

        <div className="app-sheet__divider" />

        <div className="app-sheet__cta" style={{ animationDelay: `${60 + links.length * 40}ms` }}>
          <Link to={cta.to} onClick={onClose} className="app-btn" data-variant="primary" data-full="true">
            {cta.label}
          </Link>
        </div>
        <div className="app-sheet__footer" style={{ animationDelay: `${60 + (links.length + 1) * 40}ms` }}>
          <ThemeToggle />
          <button type="button" className="app-btn" data-variant="ghost" data-size="sm" onClick={handleAux}>
            {variant === "app" && isAuthenticated ? "Sign out" : "Sign in"}
          </button>
        </div>
      </div>
    </>,
    document.body
  );
}
