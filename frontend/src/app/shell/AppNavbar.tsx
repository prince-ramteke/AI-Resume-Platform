import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useScrollState } from "./useScrollState";
import { NavHoverCapsule } from "./NavHoverCapsule";
import { NavRollLabel } from "./NavRollLabel";
import { ThemeToggle } from "./ThemeToggle";
import { MobileNavSheet } from "./MobileNavSheet";
import { APP_LINKS, APP_CTA, MARKETING_LINKS, MARKETING_CTA } from "./NavLinks";
import type { NavLinkSpec } from "./NavLinks";
import { useAuth } from "../../hooks/useAuth";

interface AppNavbarProps {
  variant?: "marketing" | "app";
}

function BrandMarkIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h5" />
      <path d="M14 2v6h6" />
      <circle cx="16.5" cy="16.5" r="2.5" />
      <path d="m21 21-2.5-2.5" />
    </svg>
  );
}

/**
 * V2 floating pill navbar (§16.1). Uses hysteresis-aware scroll state,
 * single measured hover capsule, text-roll labels, and a mobile drop sheet.
 */
export function AppNavbar({ variant = "app" }: AppNavbarProps) {
  const links: NavLinkSpec[] = variant === "app" ? APP_LINKS : MARKETING_LINKS;
  const cta = variant === "app" ? APP_CTA : MARKETING_CTA;

  const scrolled = useScrollState(12, 8);
  const [sheetOpen, setSheetOpen] = useState(false);

  const navRef = useRef<HTMLElement | null>(null);
  const linksRefs = useRef<Array<HTMLAnchorElement | null>>([]);
  const hamburgerRef = useRef<HTMLButtonElement | null>(null);

  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { logout, isAuthenticated } = useAuth();

  useEffect(() => {
    const root = document.querySelector<HTMLElement>(".app-root");
    if (!root) return;
    root.style.setProperty("--nav-offset", scrolled ? "88px" : "96px");
  }, [scrolled]);

  function activeIndex(): number | null {
    for (let i = 0; i < links.length; i++) {
      const link = links[i]!;
      const active = link.matches
        ? link.matches(pathname)
        : link.exact
        ? pathname === link.to
        : pathname === link.to || pathname.startsWith(link.to + "/");
      if (active) return i;
    }
    return null;
  }
  const activeIdx = activeIndex();

  const handleAuxAction = () => {
    if (variant === "app" && isAuthenticated) {
      logout();
      navigate("/login", { replace: true });
    } else {
      navigate("/login");
    }
  };

  const wordmarkTo = variant === "app" ? "/dashboard" : "/";

  return (
    <>
      <header
        className="app-nav"
        data-scrolled={scrolled ? "true" : "false"}
      >
        <div className="app-nav__pill">
          <Link to={wordmarkTo} className="app-nav__brand" aria-label="Resume Intelligence — home">
            <span className="app-nav__brand-mark" aria-hidden="true"><BrandMarkIcon /></span>
            <span className="app-nav__brand-word">Resume<em>Intelligence</em></span>
          </Link>

          <nav
            ref={navRef}
            className="app-nav__links"
            aria-label="Primary"
            data-hovering="false"
          >
            <NavHoverCapsule linksRefs={linksRefs} navRef={navRef} initialIndex={activeIdx} />
            {links.map((link, i) => {
              const active = link.matches
                ? link.matches(pathname)
                : link.exact
                ? pathname === link.to
                : pathname === link.to || pathname.startsWith(link.to + "/");
              return (
                <Link
                  key={link.to}
                  to={link.to}
                  ref={(el) => { linksRefs.current[i] = el as HTMLAnchorElement | null; }}
                  className="app-nav__link"
                  data-active={active ? "true" : undefined}
                  aria-current={active ? "page" : undefined}
                >
                  <NavRollLabel>{link.label}</NavRollLabel>
                </Link>
              );
            })}
          </nav>

          <div className="app-nav__actions">
            <ThemeToggle />
            {variant === "app" && isAuthenticated ? (
              <button
                type="button"
                className="app-btn"
                data-variant="ghost"
                data-size="sm"
                onClick={handleAuxAction}
                aria-label="Sign out"
              >
                Sign out
              </button>
            ) : variant === "marketing" ? (
              <Link to="/login" className="app-btn" data-variant="ghost" data-size="sm">
                Sign in
              </Link>
            ) : null}
            <Link to={cta.to} className="app-nav__cta">
              {cta.label}
            </Link>
            <button
              ref={hamburgerRef}
              type="button"
              className="app-nav__hamburger"
              aria-expanded={sheetOpen}
              aria-controls="app-nav-sheet"
              aria-label={sheetOpen ? "Close menu" : "Open menu"}
              onClick={() => setSheetOpen(true)}
            >
              <span className="app-nav__hamburger-bar" />
              <span className="app-nav__hamburger-bar" />
            </button>
          </div>
        </div>
      </header>

      <MobileNavSheet
        open={sheetOpen}
        onClose={() => setSheetOpen(false)}
        links={links}
        cta={cta}
        variant={variant}
        returnFocusTo={hamburgerRef}
      />
    </>
  );
}
