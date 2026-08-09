import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "path";

// Kept intentionally minimal: a jsdom env, jest-dom matchers loaded once,
// the same `@` → `src` alias vite uses, and coverage off by default (M6.7
// scope is focused, not fleet-wide). Add coverage later if the CI grows.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { "@": path.resolve(__dirname, "./src") },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    css: false,
    restoreMocks: true,
  },
});
