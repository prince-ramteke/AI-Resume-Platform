import { QueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";

/**
 * Shared server-state client. Dashboard data is short-lived, so a 30s staleTime
 * covers revisits without spamming the API; window-focus refetch is off to keep
 * things quiet. A 401 is terminal (the Axios interceptor clears auth and the
 * route guard redirects), so we never retry it.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        if (error instanceof AxiosError && error.response?.status === 401) {
          return false;
        }
        return failureCount < 1;
      },
    },
  },
});
