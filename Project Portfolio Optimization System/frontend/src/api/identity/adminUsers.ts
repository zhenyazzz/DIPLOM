/**
 * Admin users client — AdminUsersApi via gateway.
 *
 *   GET  /api/v1/admin/users?page&size&sort&filter
 *   PUT  /api/v1/admin/users/{userId}/role      body: { role }
 *   PUT  /api/v1/admin/users/{userId}/blocked   body: { blocked }
 */

import { ApiError, apiRequest } from "../http";
import type {
  ChangeRoleRequest,
  ListUsersParams,
  PageResponse,
  SetBlockedRequest,
  UserResponse,
} from "./types";

export class AdminUsersApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "AdminUsersApiError";
  }
}

function toAdminError(e: unknown): never {
  if (e instanceof ApiError) {
    throw new AdminUsersApiError(e.status, e.code, e.message);
  }
  throw e;
}

/** Free-text → RSQL OR on email/displayName; pass-through if already RSQL-ish */
function toRsqlFilter(filter?: string): string | undefined {
  if (!filter?.trim()) return undefined;
  const q = filter.trim();
  if (/[=<>!]/.test(q) || q.includes("==") || q.includes("=ilike=")) {
    return q;
  }
  const escaped = q.replace(/([\\*])/g, "\\$1");
  return `email=ilike=*${escaped}*,displayName=ilike=*${escaped}*`;
}

/** GET /api/v1/admin/users */
export async function listUsers(
  params: ListUsersParams = {},
): Promise<PageResponse<UserResponse>> {
  const page = Math.max(params.page ?? 0, 0);
  const size = Math.min(Math.max(params.size ?? 20, 1), 100);
  const qs = new URLSearchParams();
  qs.set("page", String(page));
  qs.set("size", String(size));
  if (params.sort) qs.set("sort", params.sort);
  const filter = toRsqlFilter(params.filter);
  if (filter) qs.set("filter", filter);

  try {
    return await apiRequest<PageResponse<UserResponse>>(
      `/api/v1/admin/users?${qs.toString()}`,
    );
  } catch (e) {
    toAdminError(e);
  }
}

/** PUT /api/v1/admin/users/{userId}/role */
export async function changeRole(
  userId: string,
  request: ChangeRoleRequest,
): Promise<UserResponse> {
  try {
    return await apiRequest<UserResponse>(`/api/v1/admin/users/${userId}/role`, {
      method: "PUT",
      body: request,
    });
  } catch (e) {
    toAdminError(e);
  }
}

/** PUT /api/v1/admin/users/{userId}/blocked */
export async function setBlocked(
  userId: string,
  request: SetBlockedRequest,
): Promise<UserResponse> {
  try {
    return await apiRequest<UserResponse>(
      `/api/v1/admin/users/${userId}/blocked`,
      {
        method: "PUT",
        body: request,
      },
    );
  } catch (e) {
    toAdminError(e);
  }
}
