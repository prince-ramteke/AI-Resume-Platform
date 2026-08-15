import axios, { AxiosError } from "axios";

/**
 * Handlers injected by AuthContext so the Axios layer can read the in-memory
 * token and react to 401s without importing React (avoids a circular import
 * between the client and the context).
 */
interface AuthHandlers {
  getToken: () => string | null;
  getRefreshToken: () => string | null;
  onUnauthorized: () => void;
  refresh?: (newToken: string, newRefreshToken: string) => void;
}

let authHandlers: AuthHandlers | null = null;
let isRefreshing = false;
let refreshQueue: Array<(token: string) => void> = [];

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

// On 401, attempt to refresh the access token if a refresh token exists.
// If refresh succeeds, retry the original request. If refresh fails or no
// refresh token exists, clear auth via the injected handler.
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const originalRequest = error.config as AxiosError['config'] & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = authHandlers?.getRefreshToken();

      if (!refreshToken) {
        authHandlers?.onUnauthorized();
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve) => {
          refreshQueue.push((token: string) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
            }
            resolve(apiClient(originalRequest));
          });
        });
      }

      isRefreshing = true;
      originalRequest._retry = true;

      return apiClient.post<{ accessToken: string; refreshToken: string }>(
        "/auth/refresh",
        { refreshToken }
      )
        .then((response) => {
          const { accessToken, refreshToken: newRefreshToken } = response.data;
          authHandlers?.refresh?.(accessToken, newRefreshToken);

          refreshQueue.forEach((callback) => callback(accessToken));
          refreshQueue = [];

          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          }
          return apiClient(originalRequest);
        })
        .catch(() => {
          authHandlers?.onUnauthorized();
          return Promise.reject(error);
        })
        .finally(() => {
          isRefreshing = false;
        });
    }

    return Promise.reject(error);
  }
);

export default apiClient;
