import "@testing-library/jest-dom/vitest";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

// RTL auto-cleanup between tests to keep DOM state isolated. jest-dom matchers
// are wired directly into vitest via the /vitest entry above.
afterEach(() => {
  cleanup();
});
