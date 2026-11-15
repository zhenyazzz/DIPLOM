import { fetchMe } from "../api/identity/auth";
import { apiRequest } from "../api/http";
import type { TokenResponse } from "../api/identity/auth";
import {
  clearSession,
  getAccessToken,
  getSession,
  setAccessToken,
  setSession,
} from "./session";

/**
 * If profile is in localStorage but access token was dropped (new tab),
 * try refresh cookie once and restore session via /users/me.
 */
export async function restoreSession(): Promise<boolean> {
  if (getAccessToken() && getSession()) return true;
  if (!getSession()) return false;

  try {
    const tokens = await apiRequest<TokenResponse>("/api/v1/auth/refresh", {
      method: "POST",
      auth: false,
      skipRefresh: true,
    });
    setAccessToken(tokens.accessToken);
    const me = await fetchMe();
    setSession({
      email: me.email,
      displayName: me.displayName,
      role: me.role,
      userId: me.id,
    });
    return true;
  } catch {
    clearSession();
    return false;
  }
}
