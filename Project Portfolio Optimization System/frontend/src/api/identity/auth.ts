import { apiRequest } from "../http";
import { setAccessToken, setSession } from "../../lib/session";
import type { UserResponse } from "./types";

export type LoginRequest = {
  email: string;
  password: string;
};

export type RegisterRequest = {
  email: string;
  displayName: string;
  password: string;
};

export type TokenResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
};

/** POST /api/v1/auth/register → 201 UserResponse */
export async function register(request: RegisterRequest): Promise<UserResponse> {
  return apiRequest<UserResponse>("/api/v1/auth/register", {
    method: "POST",
    body: request,
    auth: false,
    skipRefresh: true,
  });
}

/** POST /api/v1/auth/login → TokenResponse + refresh cookie */
export async function login(request: LoginRequest): Promise<TokenResponse> {
  const tokens = await apiRequest<TokenResponse>("/api/v1/auth/login", {
    method: "POST",
    body: request,
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
  return tokens;
}

/** GET /api/v1/users/me */
export async function fetchMe(): Promise<UserResponse> {
  return apiRequest<UserResponse>("/api/v1/users/me");
}

/** POST /api/v1/auth/logout — clears refresh cookie */
export async function logout(): Promise<void> {
  try {
    await apiRequest<void>("/api/v1/auth/logout", {
      method: "POST",
      auth: false,
      skipRefresh: true,
    });
  } catch {
    /* still clear local session */
  }
}
