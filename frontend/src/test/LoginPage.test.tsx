import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AxiosError } from "axios";
import { LoginPage } from "../pages/auth/LoginPage";
import { AuthContext } from "../context/AuthContext";
import type { AuthState } from "../context/AuthContext";
import * as authApi from "../api/auth";

/**
 * Exercises the LoginPage's client-side validation banners AND the server-side
 * error banner path. The api layer is mocked at module level so the test never
 * touches Axios/network; the auth context is a stub so `login()` is spy-able.
 */

function renderLogin() {
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
      <MemoryRouter initialEntries={["/login"]}>
        <LoginPage />
      </MemoryRouter>
    </AuthContext.Provider>
  );
}

describe("LoginPage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("shows client-side validation errors and does not submit on invalid input", async () => {
    const loginSpy = vi.spyOn(authApi, "login");
    renderLogin();
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: /sign in/i }));
    expect(await screen.findByText(/enter your email/i)).toBeInTheDocument();
    expect(screen.getByText(/enter your password/i)).toBeInTheDocument();
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it("shows a server error banner with 'Invalid email or password.' on 401", async () => {
    vi.spyOn(authApi, "login").mockRejectedValueOnce(
      new AxiosError(
        "Unauthorized",
        "ERR_BAD_REQUEST",
        undefined,
        undefined,
        { status: 401, data: {} } as never
      )
    );
    renderLogin();
    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/email/i), "u@e.com");
    await user.type(screen.getByLabelText(/password/i), "any");
    await user.click(screen.getByRole("button", { name: /sign in/i }));
    expect(
      await screen.findByText(/invalid email or password/i)
    ).toBeInTheDocument();
  });

  it("shows the parsed server message for a non-401 error", async () => {
    vi.spyOn(authApi, "login").mockRejectedValueOnce(
      new AxiosError(
        "Bad Request",
        "ERR_BAD_REQUEST",
        undefined,
        undefined,
        { status: 400, data: { message: "email must be valid" } } as never
      )
    );
    renderLogin();
    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/email/i), "u@e.com");
    await user.type(screen.getByLabelText(/password/i), "any");
    await user.click(screen.getByRole("button", { name: /sign in/i }));
    expect(
      await screen.findByText(/email must be valid/i)
    ).toBeInTheDocument();
  });
});
