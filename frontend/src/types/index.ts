/** Standard error envelope returned by the backend global handler. */
export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  traceId: string;
}

/** Roles issued by the backend (see docs/SECURITY.md). */
export type Role = "USER" | "ADMIN";

/** The authenticated user, as returned by GET /api/auth/me. */
export interface AuthUser {
  id: number;
  email: string;
  role: Role;
}

/** Spring Data `Page<T>` envelope for paginated list endpoints. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  /** Zero-based current page index. */
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
