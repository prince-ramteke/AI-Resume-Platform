import { useEffect, useRef, useState } from "react";

/**
 * V2 hover capsule (§16.1.1). A single absolutely-positioned <span> that
 * follows the hovered/focused link inside the nav row. Uses translate3d for
 * GPU compositing. No borders or shadows — just a whisper of contrast.
 *
 * On pointerleave of the nav row (with no link focused), opacity fades to 0
 * but position is retained so the next hover eases from the last spot.
 */
export interface NavHoverCapsuleProps {
  linksRefs: React.MutableRefObject<Array<HTMLAnchorElement | null>>;
  navRef: React.RefObject<HTMLElement | null>;
  initialIndex?: number | null;
}

export function NavHoverCapsule({ linksRefs, navRef, initialIndex = null }: NavHoverCapsuleProps) {
  const capsuleRef = useRef<HTMLSpanElement | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const nav = navRef.current;
    const capsule = capsuleRef.current;
    if (!nav || !capsule) return;

    const moveTo = (link: HTMLAnchorElement | null) => {
      if (!link) return;
      const navBox = nav.getBoundingClientRect();
      const linkBox = link.getBoundingClientRect();
      const x = linkBox.left - navBox.left;
      const w = linkBox.width;
      capsule.style.transform = `translate3d(${x}px, -50%, 0)`;
      capsule.style.width = `${w}px`;
    };

    if (initialIndex != null) {
      moveTo(linksRefs.current[initialIndex] ?? null);
    }

    const onPointerEnter = (e: Event) => {
      const target = e.target;
      if (!(target instanceof HTMLElement)) return;
      const anchor = target.closest("a.app-nav__link") as HTMLAnchorElement | null;
      if (!anchor) return;
      nav.setAttribute("data-hovering", "true");
      moveTo(anchor);
    };

    const onFocusIn = (e: FocusEvent) => {
      const target = e.target;
      if (!(target instanceof HTMLElement)) return;
      const anchor = target.closest("a.app-nav__link") as HTMLAnchorElement | null;
      if (!anchor) return;
      nav.setAttribute("data-hovering", "true");
      moveTo(anchor);
    };

    const onPointerLeave = (e: MouseEvent) => {
      if (!nav.contains(e.relatedTarget as Node | null)) {
        const focused = document.activeElement;
        if (!focused || !nav.contains(focused) || !focused.closest("a.app-nav__link")) {
          nav.setAttribute("data-hovering", "false");
        }
      }
    };

    const onFocusOut = (e: FocusEvent) => {
      if (!nav.contains(e.relatedTarget as Node | null)) {
        nav.setAttribute("data-hovering", "false");
      }
    };

    nav.addEventListener("pointerenter", onPointerEnter, true);
    nav.addEventListener("focusin", onFocusIn);
    nav.addEventListener("pointerleave", onPointerLeave);
    nav.addEventListener("focusout", onFocusOut);

    const ro = new ResizeObserver(() => {
      const active = nav.querySelector<HTMLAnchorElement>('a.app-nav__link[data-active="true"]');
      if (active && nav.getAttribute("data-hovering") !== "true") {
        moveTo(active);
      }
    });
    ro.observe(nav);

    setReady(true);
    return () => {
      nav.removeEventListener("pointerenter", onPointerEnter, true);
      nav.removeEventListener("focusin", onFocusIn);
      nav.removeEventListener("pointerleave", onPointerLeave);
      nav.removeEventListener("focusout", onFocusOut);
      ro.disconnect();
    };
  }, [linksRefs, navRef, initialIndex]);

  return (
    <span
      ref={capsuleRef}
      className="app-nav__hover"
      aria-hidden="true"
      style={ready ? undefined : { display: "none" }}
    />
  );
}
