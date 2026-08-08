import { AxiosError } from "axios";
import type { ErrorResponse } from "../types";

/** Normalized, display-ready error shape for the UI. */
export interface ApiError {
  message: string;
  status?: number;
}

function defaultMessageFor(status: number): string {
  switch (status) {
    case 401:
      return "Your session has expired. Please sign in again.";
    case 403:
      return "You don't have access to this.";
    case 404:
      // Ownership violations also return 404 (enumeration defense) — never
      // phrase this as a permissions error.
      return "This item isn't available. It may have been deleted.";
    case 409:
      return "That conflicts with something that already exists.";
    case 413:
      return "That file is too large. The limit is 10 MB.";
    case 422:
      return "We couldn't produce a reliable result. Try again or adjust the inputs.";
    default:
      return status >= 500
        ? "Something went wrong on our end. Please try again."
        : "Something went wrong. Please try again.";
  }
}

/**
 * Turn an unknown thrown value into a display-ready message.
 *
 * The backend's 400s return a single joined `message` string (not a field map),
 * so we surface `message` verbatim when present and fall back to a generic,
 * status-appropriate line otherwise.
 */
export function parseApiError(err: unknown): ApiError {
  if (err instanceof AxiosError) {
    const status = err.response?.status;
    const serverMessage = (err.response?.data as Partial<ErrorResponse> | undefined)
      ?.message;

    if (serverMessage) return { message: serverMessage, status };
    if (typeof status === "number") {
      return { message: defaultMessageFor(status), status };
    }
    return {
      message: "Can't reach the server. Check your connection and try again.",
    };
  }

  return { message: "Something went wrong. Please try again." };
}
