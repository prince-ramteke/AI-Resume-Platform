import { useEffect, useState } from "react";

/**
 * Returns true when the window (or an optional target element) has scrolled
 * past `threshold` pixels. Uses `hysteresis` to prevent flickering near the
 * threshold (V2 §16.1.3). Throttled via requestAnimationFrame; registered
 * as `passive` — never blocks scroll.
 */
export function useScrollState(
  threshold = 12,
  hysteresis = 8,
  target?: HTMLElement | null,
): boolean {
  const [scrolled, setScrolled] = useState<boolean>(() => {
    if (typeof window === "undefined") return false;
    const y = target ? target.scrollTop : window.scrollY;
    return y > threshold;
  });

  useEffect(() => {
    if (typeof window === "undefined") return;
    const el: HTMLElement | Window = target ?? window;
    let raf = 0;
    const read = () => {
      const y = target ? target.scrollTop : window.scrollY;
      setScrolled((prev) => {
        if (prev && y < threshold - hysteresis) return false;
        if (!prev && y > threshold) return true;
        return prev;
      });
      raf = 0;
    };
    const onScroll = () => {
      if (raf === 0) raf = window.requestAnimationFrame(read);
    };
    el.addEventListener("scroll", onScroll, { passive: true } as AddEventListenerOptions);
    read();
    return () => {
      el.removeEventListener("scroll", onScroll as EventListener);
      if (raf) window.cancelAnimationFrame(raf);
    };
  }, [threshold, hysteresis, target]);

  return scrolled;
}
