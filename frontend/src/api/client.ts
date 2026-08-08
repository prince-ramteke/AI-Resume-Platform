import axios, { AxiosError } from "axios";

/**
 * Handlers injected by AuthContext so the Axios layer can read the in-memory
 * token and react to 401s without importing React (avoids a circular import
 * between the client and the context).
 */
interface AuthHandlers {
  getToken: () => string | null;
  onUnauthorized: () => void;
}

let authHandlers: AuthHandlers | null = null;

export function setAuthHandlers(handlers: AuthHandlers): void {
  authHandlers = handlers;
}

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// Attach the bearer token (when present) to every request.
apiClient.interceptors.request.use((config) => {
  const token = authHandlers?.getToken() ?? null;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// On 401, clear auth via the injected handler; navigation is handled inside the
// Router tree (ProtectedRoute reacts to the cleared state).
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      authHandlers?.onUnauthorized();
    }
    return Promise.reject(error);
  }
);

export default apiClient;
