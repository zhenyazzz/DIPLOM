import type { UserRole } from "../api/identity/types";

const SESSION_KEY = "portfel.session";
const TOKEN_KEY = "portfel.accessToken";

export type AppSession = {
  email: string;
  displayName: string;
  role: UserRole;
  userId?: string;
};

export function getSession(): AppSession | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as AppSession;
  } catch {
    return null;
  }
}

export function setSession(session: AppSession) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
  sessionStorage.removeItem(TOKEN_KEY);
}

export function isSignedIn() {
  return getSession() !== null && !!getAccessToken();
}

export function getAccessToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token: string) {
  sessionStorage.setItem(TOKEN_KEY, token);
}

export function isAdmin() {
  return getSession()?.role === "ADMIN";
}
