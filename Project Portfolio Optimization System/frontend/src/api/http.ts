import { clearSession, getAccessToken, setAccessToken } from "../lib/session";

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type ProblemBody = {
  title?: string;
  detail?: string;
  code?: string;
  status?: number;
};

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  /** Skip Authorization header (login/register/refresh) */
  auth?: boolean;
  /** Do not attempt refresh+retry on 401 */
  skipRefresh?: boolean;
};

let refreshInFlight: Promise<boolean> | null = null;

async function parseProblem(res: Response): Promise<ApiError> {
  let code = "HTTP_ERROR";
  let message = res.statusText || `HTTP ${res.status}`;
  try {
    const data = (await res.json()) as ProblemBody;
    if (data.code) code = data.code;
    if (data.detail || data.title) message = data.detail || data.title || message;
  } catch {
    /* non-JSON */
  }
  return new ApiError(res.status, code, message);
}

async function tryRefresh(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const res = await fetch(`${API_BASE}/api/v1/auth/refresh`, {
          method: "POST",
          credentials: "include",
        });
        if (!res.ok) return false;
        const data = (await res.json()) as { accessToken?: string };
        if (!data.accessToken) return false;
        setAccessToken(data.accessToken);
        return true;
      } catch {
        return false;
      } finally {
        refreshInFlight = null;
      }
    })();
  }
  return refreshInFlight;
}

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const {
    body,
    auth = true,
    skipRefresh = false,
    headers: initHeaders,
    ...rest
  } = options;

  const headers = new Headers(initHeaders);
  if (body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (auth) {
    const token = getAccessToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers,
    credentials: "include",
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (res.status === 401 && auth && !skipRefresh) {
    const ok = await tryRefresh();
    if (ok) {
      return apiRequest<T>(path, { ...options, skipRefresh: true });
    }
    clearSession();
  }

  if (res.status === 204) {
    return undefined as T;
  }

  if (!res.ok) {
    throw await parseProblem(res);
  }

  if (res.status === 201 || res.headers.get("content-type")?.includes("json")) {
    return (await res.json()) as T;
  }

  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}
