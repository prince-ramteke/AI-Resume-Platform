import { describe, expect, it, vi, beforeEach } from "vitest";
import { AxiosError, AxiosHeaders } from "axios";
import apiClient, { setAuthHandlers } from "../api/client";
import { parseApiError } from "../api/errors";

/**
 * Deterministic tests for the shared Axios instance:
 *  - request interceptor attaches the current in-memory token
 *  - response interceptor calls the injected onUnauthorized on 401 only
 *  - parseApiError normalizes the standard error envelope + fallbacks
 *
 * No network: interceptors are exercised directly against mock configs and
 * error objects rather than by mounting axios-adapter behavior.
 */

describe("apiClient interceptors", () => {
  let getToken: ReturnType<typeof vi.fn<() => string | null>>;
  let onUnauthorized: ReturnType<typeof vi.fn<() => void>>;

  beforeEach(() => {
    getToken = vi.fn<() => string | null>();
    onUnauthorized = vi.fn<() => void>();
    setAuthHandlers({ getToken, onUnauthorized });
  });

  it("attaches Bearer token when getToken returns a value", async () => {
    getToken.mockReturnValue("abc.def.ghi");
    const request = apiClient.interceptors.request as unknown as {
      handlers: { fulfilled: (c: unknown) => unknown }[];
    };
    const handler = request.handlers[0]!;
    const config = { headers: new AxiosHeaders() };
    const result = (await handler.fulfilled(config)) as {
      headers: AxiosHeaders;
    };
    expect(result.headers.get("Authorization")).toBe("Bearer abc.def.ghi");
  });

  it("omits Authorization header when no token is present", async () => {
    getToken.mockReturnValue(null);
    const request = apiClient.interceptors.request as unknown as {
      handlers: { fulfilled: (c: unknown) => unknown }[];
    };
    const handler = request.handlers[0]!;
    const config = { headers: new AxiosHeaders() };
    const result = (await handler.fulfilled(config)) as {
      headers: AxiosHeaders;
    };
    expect(result.headers.has("Authorization")).toBe(false);
  });

  it("invokes onUnauthorized on 401 responses", async () => {
    const response = apiClient.interceptors.response as unknown as {
      handlers: { rejected: (e: unknown) => unknown }[];
    };
    const handler = response.handlers[0]!;
    const err = new AxiosError(
      "Unauthorized",
      "ERR_BAD_REQUEST",
      undefined,
      undefined,
      { status: 401 } as never
    );
    await expect(handler.rejected(err)).rejects.toBe(err);
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it("does NOT invoke onUnauthorized on non-401 responses", async () => {
    const response = apiClient.interceptors.response as unknown as {
      handlers: { rejected: (e: unknown) => unknown }[];
    };
    const handler = response.handlers[0]!;
    const err = new AxiosError(
      "Not Found",
      "ERR_BAD_REQUEST",
      undefined,
      undefined,
      { status: 404 } as never
    );
    await expect(handler.rejected(err)).rejects.toBe(err);
    expect(onUnauthorized).not.toHaveBeenCalled();
  });
});

describe("parseApiError", () => {
  it("surfaces the server envelope message verbatim when present", () => {
    const err = new AxiosError(
      "Bad Request",
      "ERR_BAD_REQUEST",
      undefined,
      undefined,
      { status: 400, data: { message: "email must be valid" } } as never
    );
    const parsed = parseApiError(err);
    expect(parsed.status).toBe(400);
    expect(parsed.message).toBe("email must be valid");
  });

  it("falls back to a default message per status when no server message", () => {
    const err = new AxiosError(
      "Payload Too Large",
      "ERR_BAD_REQUEST",
      undefined,
      undefined,
      { status: 413, data: {} } as never
    );
    const parsed = parseApiError(err);
    expect(parsed.status).toBe(413);
    expect(parsed.message).toMatch(/too large/i);
  });

  it("returns a network-oriented message when no response is present", () => {
    const err = new AxiosError("Network Error");
    const parsed = parseApiError(err);
    expect(parsed.status).toBeUndefined();
    expect(parsed.message).toMatch(/reach the server/i);
  });

  it("returns a generic message for non-Axios throwables", () => {
    const parsed = parseApiError(new Error("boom"));
    expect(parsed.status).toBeUndefined();
    expect(parsed.message).toMatch(/something went wrong/i);
  });
});
