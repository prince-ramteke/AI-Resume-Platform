import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AxiosError } from "axios";
import { VerifyEmailPage } from "../app/pages/auth/VerifyEmailPage";
import { AuthContext } from "../context/AuthContext";
import type { AuthState } from "../context/AuthContext";
import * as authApi from "../api/auth";

/** Renders VerifyEmailPage with a given search string; adds stub login/register routes. */
function renderVerify(search = "?email=alice%40example.com") {
  const value: AuthState = {
    token: null,
    refreshToken: null,
    user: null,
    isAuthenticated: false,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  };
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={[`/verify-email${search}`]}>
        <Routes>
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/login" element={<div>login page</div>} />
          <Route path="/register" element={<div>register page</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>
  );
}

describe("VerifyEmailPage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  // ─── Redirect when no email provided ──────────────────────────────────────

  it("redirects to /register when no email query param is present", () => {
    renderVerify("");
    expect(screen.getByText("register page")).toBeInTheDocument();
  });

  // ─── Rendering ────────────────────────────────────────────────────────────

  it("renders the masked email and OTP input", () => {
    renderVerify();
    expect(screen.getByRole("heading", { name: /check your inbox/i })).toBeInTheDocument();
    // Masked form: a***@example.com
    expect(screen.getByText(/a\*\*\*@example\.com/i)).toBeInTheDocument();
    expect(screen.getByRole("textbox")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /verify email/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /resend/i })).toBeInTheDocument();
  });

  it("verify button is disabled when fewer than 6 digits are entered", async () => {
    renderVerify();
    const user = userEvent.setup();
    await user.type(screen.getByRole("textbox"), "123");
    expect(screen.getByRole("button", { name: /verify email/i })).toBeDisabled();
  });

  // ─── Successful verification → navigate to /login?verified=1 ──────────────

  it("successful verification navigates to /login?verified=1", async () => {
    vi.spyOn(authApi, "verifyEmail").mockResolvedValueOnce({ message: "Email verified successfully" });
    renderVerify();
    const user = userEvent.setup();
    // Type all 6 digits — auto-submit fires via useEffect
    await user.type(screen.getByRole("textbox"), "482931");
    expect(await screen.findByText("login page")).toBeInTheDocument();
    expect(authApi.verifyEmail).toHaveBeenCalledWith({ email: "alice@example.com", otp: "482931" });
  });

  // ─── Invalid OTP ──────────────────────────────────────────────────────────

  it("shows backend error message on 422 invalid OTP", async () => {
    vi.spyOn(authApi, "verifyEmail").mockRejectedValueOnce(
      new AxiosError("Unprocessable", "ERR_BAD_REQUEST", undefined, undefined, {
        status: 422,
        data: { message: "Invalid or expired code." },
      } as never)
    );
    renderVerify();
    const user = userEvent.setup();
    await user.type(screen.getByRole("textbox"), "000000");
    expect(await screen.findByRole("alert")).toHaveTextContent(/invalid or expired code/i);
    // Input should be re-enabled for retry
    expect(screen.getByRole("textbox")).not.toBeDisabled();
  });

  // ─── Too many attempts ────────────────────────────────────────────────────

  it("shows cooldown message and starts countdown on 429 with retryAfterSeconds", async () => {
    vi.spyOn(authApi, "verifyEmail").mockRejectedValueOnce(
      new AxiosError("Too Many Requests", "ERR_BAD_REQUEST", undefined, undefined, {
        status: 429,
        data: { message: "Too many attempts.", retryAfterSeconds: 60 },
      } as never)
    );
    renderVerify();
    const user = userEvent.setup();
    await user.type(screen.getByRole("textbox"), "999999");
    expect(await screen.findByRole("alert")).toHaveTextContent(/too many attempts/i);
    // Resend button replaced by countdown
    expect(screen.queryByRole("button", { name: /resend/i })).not.toBeInTheDocument();
    expect(screen.getByText(/resend in/i)).toBeInTheDocument();
  });

  // ─── Resend ───────────────────────────────────────────────────────────────

  it("resend code calls the API and starts a cooldown", async () => {
    vi.spyOn(authApi, "resendOtp").mockResolvedValueOnce({
      message: "If this email is registered and unverified, a new code has been sent.",
    });
    renderVerify();
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: /resend/i }));
    expect(authApi.resendOtp).toHaveBeenCalledWith({ email: "alice@example.com" });
    // After success a countdown replaces the resend button
    expect(await screen.findByText(/resend in/i)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /resend/i })).not.toBeInTheDocument();
  });

  it("resend shows backend error message on failure", async () => {
    vi.spyOn(authApi, "resendOtp").mockRejectedValueOnce(
      new AxiosError("Bad Request", "ERR_BAD_REQUEST", undefined, undefined, {
        status: 400,
        data: { message: "Email is already verified." },
      } as never)
    );
    renderVerify();
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: /resend/i }));
    expect(await screen.findByRole("alert")).toHaveTextContent(/already verified/i);
  });

  // ─── OTP input hygiene ────────────────────────────────────────────────────

  it("strips non-digit characters from OTP input", async () => {
    renderVerify();
    const user = userEvent.setup();
    const input = screen.getByRole("textbox");
    await user.type(input, "12-AB");
    expect(input).toHaveValue("12");
  });

  it("does not expose the OTP in an accessible error message", async () => {
    vi.spyOn(authApi, "verifyEmail").mockRejectedValueOnce(
      new AxiosError("422", "ERR_BAD_REQUEST", undefined, undefined, {
        status: 422,
        data: { message: "Invalid or expired code." },
      } as never)
    );
    renderVerify();
    const user = userEvent.setup();
    await user.type(screen.getByRole("textbox"), "482931");
    const alert = await screen.findByRole("alert");
    // Error text must never echo the OTP digits
    expect(alert.textContent).not.toContain("482931");
  });

  // ─── Duplicate-submit guard (regression for StrictMode / rapid-click race) ──

  it("issues exactly ONE POST even when submit() is called twice synchronously before the first await resolves", async () => {
    // The race: auto-submit useEffect AND a manual button click both invoke
    // submit() in the same render cycle. Both see isSubmitting=false in their
    // React-state closure because setIsSubmitting(true) hasn't committed yet.
    // The submittingRef guard (a synchronous ref, not a state value) blocks the
    // second caller immediately — no extra POST.
    let resolveFirst!: () => void;
    const spy = vi.spyOn(authApi, "verifyEmail").mockReturnValue(
      new Promise<{ message: string }>((res) => { resolveFirst = () => res({ message: "ok" }); })
    );

    // Render the hook directly so we can call submit() twice synchronously,
    // exactly as the auto-submit effect + button click would in the same tick.
    const { result } = renderHook(
      () => useVerifyEmail("alice@example.com"),
      { wrapper: wrapWithRouter(["/register"]) },
    );

    // Put otp into the 6-digit ready state.
    act(() => { result.current.setOtp("482931"); });

    // Fire submit() twice without yielding — simulates the race window where
    // isSubmitting state hasn't re-rendered yet but submittingRef is already true.
    await act(async () => {
      void result.current.submit();   // first call: acquires ref, calls API
      void result.current.submit();   // second call: ref already true → returns immediately
    });

    // Exactly one API call regardless of how many times submit() was invoked.
    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy).toHaveBeenCalledWith({ email: "alice@example.com", otp: "482931" });

    // Cleanup: resolve so the pending promise doesn't leak into other tests.
    await act(async () => { resolveFirst(); });
  });

  // ─── Loading state ────────────────────────────────────────────────────────

  it("shows 'Verifying…' and disables input while the request is in flight", async () => {
    let resolve!: () => void;
    vi.spyOn(authApi, "verifyEmail").mockReturnValueOnce(
      new Promise<{ message: string }>((res) => { resolve = () => res({ message: "ok" }); })
    );
    renderVerify();
    const user = userEvent.setup();
    await user.type(screen.getByRole("textbox"), "482931");
    // Button label changes to Verifying…
    expect(await screen.findByRole("button", { name: /verifying/i })).toBeInTheDocument();
    expect(screen.getByRole("textbox")).toBeDisabled();
    // Resolve to prevent act() warnings
    await act(async () => { resolve(); });
  });
});

// ─── useRegister routing tests ────────────────────────────────────────────────

import { useRegister } from "../hooks/useRegister";
import { useVerifyEmail } from "../hooks/useVerifyEmail";
import { renderHook } from "@testing-library/react";

function wrapWithRouter(initialEntries: string[]) {
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <AuthContext.Provider
        value={{
          token: null, refreshToken: null, user: null, isAuthenticated: false,
          login: vi.fn(), logout: vi.fn(), refresh: vi.fn(),
        }}
      >
        <MemoryRouter initialEntries={initialEntries}>
          <Routes>
            <Route path="/register" element={<>{children}</>} />
            <Route path="/verify-email" element={<div>verify page</div>} />
            <Route path="/login" element={<div>login page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    );
  };
}

describe("useRegister — routing after registration", () => {
  beforeEach(() => { vi.restoreAllMocks(); });

  it("navigates to /verify-email when emailVerificationRequired=true", async () => {
    vi.spyOn(authApi, "register").mockResolvedValueOnce({
      id: 1,
      email: "new@example.com",
      role: "USER",
      emailVerificationRequired: true,
    });
    const { result } = renderHook(() => useRegister(), {
      wrapper: wrapWithRouter(["/register"]),
    });
    await act(async () => {
      await result.current.submit("new@example.com", "Password1!");
    });
    expect(screen.getByText("verify page")).toBeInTheDocument();
  });

  it("navigates to /login?registered=1 when emailVerificationRequired is absent", async () => {
    vi.spyOn(authApi, "register").mockResolvedValueOnce({
      id: 2,
      email: "other@example.com",
      role: "USER",
    });
    const { result } = renderHook(() => useRegister(), {
      wrapper: wrapWithRouter(["/register"]),
    });
    await act(async () => {
      await result.current.submit("other@example.com", "Password1!");
    });
    expect(screen.getByText("login page")).toBeInTheDocument();
  });
});
