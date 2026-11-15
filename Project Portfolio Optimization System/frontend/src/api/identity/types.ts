/**
 * Mirrors identity-api contracts for a drop-in fetch swap later.
 * Backend: com.portfolio.identity.api.identity.*
 */

export type UserRole =
  | "INITIATOR"
  | "EXPERT_ANALYST"
  | "DECISION_MAKER"
  | "ADMIN";

/** GET/PUT response — UserResponse */
export type UserResponse = {
  id: string;
  email: string;
  displayName: string;
  role: UserRole;
  blocked: boolean;
};

/** PUT /api/v1/admin/users/{id}/role — ChangeRoleRequest */
export type ChangeRoleRequest = {
  role: UserRole;
};

/** PUT /api/v1/admin/users/{id}/blocked — SetBlockedRequest */
export type SetBlockedRequest = {
  blocked: boolean;
};

/** common-web PageResponse<T> */
export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

/** Query for GET /api/v1/admin/users */
export type ListUsersParams = {
  page?: number;
  size?: number;
  /** RSQL: email, displayName, blocked */
  filter?: string;
  /** e.g. "displayName,asc" */
  sort?: string;
};

export const USER_ROLES: UserRole[] = [
  "INITIATOR",
  "EXPERT_ANALYST",
  "DECISION_MAKER",
  "ADMIN",
];

export const userRoleLabel: Record<UserRole, string> = {
  INITIATOR: "Инициатор",
  EXPERT_ANALYST: "Эксперт-аналитик",
  DECISION_MAKER: "ЛПР",
  ADMIN: "Админ",
};
