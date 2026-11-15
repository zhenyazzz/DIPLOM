import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import {
  Ban,
  Check,
  ChevronLeft,
  ChevronRight,
  LoaderCircle,
  Search,
  Shield,
  UserRound,
} from "lucide-react";
import { AnimatePresence, LayoutGroup, motion } from "motion/react";
import {
  AdminUsersApiError,
  changeRole,
  listUsers,
  setBlocked,
} from "../api/identity/adminUsers";
import {
  USER_ROLES,
  userRoleLabel,
  type PageResponse,
  type UserResponse,
  type UserRole,
} from "../api/identity/types";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";
import { cn } from "../lib/cn";
import {
  easeOutExpo,
  fadeUp,
  listItem,
  springSoft,
  staggerContainer,
  staggerFast,
} from "../lib/motion";

const PAGE_SIZE = 20;

function initialsOf(name: string) {
  return (
    name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0]?.toUpperCase() ?? "")
      .join("") || "?"
  );
}

function roleTone(role: UserRole) {
  switch (role) {
    case "ADMIN":
      return "bg-accent-soft text-accent";
    case "DECISION_MAKER":
      return "bg-signal/15 text-signal";
    case "EXPERT_ANALYST":
      return "bg-amber/15 text-amber";
    default:
      return "bg-panel-2 text-mute";
  }
}

export function UsersPage() {
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState<UserRole | "ALL">("ALL");
  const [statusFilter, setStatusFilter] = useState<"ALL" | "ACTIVE" | "BLOCKED">(
    "ALL",
  );
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [data, setData] = useState<PageResponse<UserResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [flash, setFlash] = useState<string | null>(null);

  const [draftRole, setDraftRole] = useState<UserRole>("INITIATOR");
  const [draftBlocked, setDraftBlocked] = useState(false);

  useEffect(() => {
    const t = window.setTimeout(() => setDebouncedQuery(query.trim()), 200);
    return () => window.clearTimeout(t);
  }, [query]);

  useEffect(() => {
    setPage(0);
  }, [debouncedQuery, roleFilter, statusFilter]);

  // Text → query `filter` (later: RSQL). Role/status chips refine client-side for now.
  const filterParam = debouncedQuery || undefined;

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await listUsers({
        page,
        size: PAGE_SIZE,
        filter: filterParam,
        sort: "displayName,asc",
      });
      setData(result);
    } catch (e) {
      const msg =
        e instanceof AdminUsersApiError
          ? e.message
          : "Не удалось загрузить пользователей";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [page, filterParam]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const rows = useMemo(() => {
    if (!data) return [];
    return data.content.filter((u) => {
      if (roleFilter !== "ALL" && u.role !== roleFilter) return false;
      if (statusFilter === "BLOCKED" && !u.blocked) return false;
      if (statusFilter === "ACTIVE" && u.blocked) return false;
      return true;
    });
  }, [data, roleFilter, statusFilter]);

  const selected = useMemo(
    () => rows.find((u) => u.id === selectedId) ?? null,
    [rows, selectedId],
  );

  useEffect(() => {
    if (!selected) return;
    setDraftRole(selected.role);
    setDraftBlocked(selected.blocked);
  }, [selected]);

  const stats = useMemo(() => {
    const all = data?.content ?? [];
    return {
      total: data?.totalElements ?? 0,
      shown: rows.length,
      blocked: all.filter((u) => u.blocked).length,
    };
  }, [data, rows.length]);

  const dirty =
    selected != null &&
    (draftRole !== selected.role || draftBlocked !== selected.blocked);

  async function applyChanges() {
    if (!selected || !dirty) return;
    setSaving(true);
    setError(null);
    setFlash(null);
    try {
      let latest = selected;
      if (draftRole !== selected.role) {
        latest = await changeRole(selected.id, { role: draftRole });
      }
      if (draftBlocked !== selected.blocked) {
        latest = await setBlocked(selected.id, { blocked: draftBlocked });
      }
      setFlash("Изменения сохранены");
      await reload();
      setSelectedId(latest.id);
      setDraftRole(latest.role);
      setDraftBlocked(latest.blocked);
    } catch (e) {
      const msg =
        e instanceof AdminUsersApiError
          ? e.code === "CONFLICT"
            ? "Нельзя снять роль или заблокировать последнего активного админа"
            : e.message
          : "Ошибка сохранения";
      setError(msg);
    } finally {
      setSaving(false);
    }
  }

  function resetDraft() {
    if (!selected) return;
    setDraftRole(selected.role);
    setDraftBlocked(selected.blocked);
    setError(null);
  }

  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="grid gap-5 xl:grid-cols-[1.15fr_0.85fr]">
      <motion.div
        className="min-w-0"
        variants={staggerContainer}
        initial="hidden"
        animate="show"
      >
        <motion.div
          variants={fadeUp}
          className="mb-7 flex flex-wrap items-end justify-between gap-4 border-b border-hairline pb-6"
        >
          <div>
            <p className="meta mb-3">Администрирование</p>
            <h1 className="font-display text-4xl text-paper sm:text-5xl">
              Пользователи
            </h1>
            <p className="mt-2 text-[15px] text-mute">
              Управление ролями и доступом
            </p>
          </div>
        </motion.div>

        <motion.div
          variants={staggerFast}
          initial="hidden"
          animate="show"
          className="mb-4 grid grid-cols-3 overflow-hidden rounded-lg border border-hairline bg-panel"
        >
          {[
            { label: "Всего", value: String(stats.total) },
            { label: "На экране", value: String(stats.shown) },
            { label: "Блок на стр.", value: String(stats.blocked) },
          ].map((item) => (
            <motion.div
              key={item.label}
              variants={fadeUp}
              className="border-r border-hairline px-4 py-3.5 last:border-r-0"
            >
              <p className="meta">{item.label}</p>
              <p className="num mt-2 text-xl text-paper">{item.value}</p>
            </motion.div>
          ))}
        </motion.div>

        <motion.div variants={fadeUp} className="relative mb-3">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-3.5 -translate-y-1/2 text-mute" />
          <Input
            className="pl-9"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Email или имя"
            aria-label="Поиск пользователей"
          />
        </motion.div>

        <motion.div
          variants={fadeUp}
          className="mb-3 flex flex-wrap gap-2"
        >
          <FilterChip
            active={statusFilter === "ALL"}
            onClick={() => setStatusFilter("ALL")}
          >
            Все
          </FilterChip>
          <FilterChip
            active={statusFilter === "ACTIVE"}
            onClick={() => setStatusFilter("ACTIVE")}
          >
            Активные
          </FilterChip>
          <FilterChip
            active={statusFilter === "BLOCKED"}
            onClick={() => setStatusFilter("BLOCKED")}
          >
            Заблокированы
          </FilterChip>
          <span className="mx-1 hidden h-6 w-px bg-hairline sm:inline" />
          <FilterChip
            active={roleFilter === "ALL"}
            onClick={() => setRoleFilter("ALL")}
          >
            Любая роль
          </FilterChip>
          {USER_ROLES.map((role) => (
            <FilterChip
              key={role}
              active={roleFilter === role}
              onClick={() => setRoleFilter(role)}
            >
              {userRoleLabel[role]}
            </FilterChip>
          ))}
        </motion.div>

        <motion.div
          variants={fadeUp}
          className="overflow-hidden rounded-lg border border-hairline bg-panel"
        >
          {loading && !data ? (
            <div className="flex items-center justify-center gap-2 px-4 py-16 text-mute">
              <LoaderCircle className="size-4 animate-spin" />
              Загрузка…
            </div>
          ) : rows.length === 0 ? (
            <div className="px-4 py-16 text-center text-[14px] text-mute">
              Пользователи не найдены
            </div>
          ) : (
            <LayoutGroup>
              <div className="max-h-[52vh] overflow-auto">
                <AnimatePresence mode="popLayout" initial={false}>
                  {rows.map((user) => {
                    const active = selectedId === user.id;
                    return (
                      <motion.button
                        key={user.id}
                        type="button"
                        layout
                        variants={listItem}
                        initial="hidden"
                        animate="show"
                        exit={{
                          opacity: 0,
                          height: 0,
                          margin: 0,
                          paddingTop: 0,
                          paddingBottom: 0,
                        }}
                        transition={springSoft}
                        whileHover={{
                          backgroundColor:
                            "color-mix(in srgb, var(--color-panel-2) 70%, transparent)",
                        }}
                        onClick={() =>
                          setSelectedId(active ? null : user.id)
                        }
                        className={cn(
                          "relative flex w-full items-center gap-4 border-b border-hairline px-4 py-3.5 text-left last:border-b-0",
                          active ? "bg-accent-soft" : "",
                        )}
                      >
                        {active && (
                          <motion.span
                            layoutId="user-active-bar"
                            className="absolute top-2 bottom-2 left-0 w-0.5 rounded-full bg-accent"
                            transition={springSoft}
                          />
                        )}
                        <span className="grid size-9 shrink-0 place-items-center rounded-md border border-hairline bg-ink font-mono text-[11px] text-accent">
                          {initialsOf(user.displayName)}
                        </span>
                        <div className="min-w-0 flex-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <p className="truncate text-[15px] font-medium text-paper">
                              {user.displayName}
                            </p>
                            <span
                              className={cn(
                                "inline-flex h-6 items-center rounded-full px-2.5 text-[11px] font-medium",
                                roleTone(user.role),
                              )}
                            >
                              {userRoleLabel[user.role]}
                            </span>
                            {user.blocked && (
                              <span className="inline-flex h-6 items-center rounded-full bg-reject/10 px-2.5 text-[11px] font-medium text-reject">
                                Блок
                              </span>
                            )}
                          </div>
                          <p className="mt-0.5 truncate text-[12px] text-mute">
                            {user.email}
                          </p>
                        </div>
                      </motion.button>
                    );
                  })}
                </AnimatePresence>
              </div>
            </LayoutGroup>
          )}

          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-between gap-3 border-t border-hairline px-4 py-2.5">
              <p className="text-[12px] text-mute">
                Стр. {page + 1} / {totalPages} · {data.totalElements}
              </p>
              <div className="flex gap-1">
                <Button
                  variant="ghost"
                  className="h-8 px-2"
                  disabled={page <= 0 || loading}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  aria-label="Предыдущая страница"
                >
                  <ChevronLeft size={16} />
                </Button>
                <Button
                  variant="ghost"
                  className="h-8 px-2"
                  disabled={page >= totalPages - 1 || loading}
                  onClick={() => setPage((p) => p + 1)}
                  aria-label="Следующая страница"
                >
                  <ChevronRight size={16} />
                </Button>
              </div>
            </div>
          )}
        </motion.div>
      </motion.div>

      <motion.aside
        initial={{ opacity: 0, x: 18 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ delay: 0.12, duration: 0.55, ease: easeOutExpo }}
        className="min-h-[440px] overflow-hidden rounded-xl border border-hairline bg-panel xl:min-h-[640px]"
      >
        <AnimatePresence mode="wait">
          {!selected ? (
            <motion.div
              key="empty"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex h-full min-h-[440px] flex-col items-center justify-center gap-3 px-8 text-center xl:min-h-[640px]"
            >
              <span className="grid size-12 place-items-center rounded-xl border border-hairline bg-ink text-mute">
                <UserRound size={22} strokeWidth={1.5} />
              </span>
              <p className="text-[15px] font-medium text-paper">
                Выберите пользователя
              </p>
              <p className="max-w-xs text-[13px] text-mute">
                Здесь можно сменить роль и заблокировать доступ.
              </p>
            </motion.div>
          ) : (
            <motion.div
              key={selected.id}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.35, ease: easeOutExpo }}
              className="flex h-full flex-col"
            >
              <header className="border-b border-hairline px-5 py-5">
                <p className="meta mb-3">Карточка</p>
                <div className="flex items-start gap-4">
                  <span className="grid size-14 shrink-0 place-items-center rounded-xl border border-hairline bg-ink font-display text-xl text-paper">
                    {initialsOf(selected.displayName)}
                  </span>
                  <div className="min-w-0">
                    <h2 className="font-display text-2xl text-paper">
                      {selected.displayName}
                    </h2>
                    <p className="mt-1 truncate text-[14px] text-mute">
                      {selected.email}
                    </p>
                    <p className="mt-2 font-mono text-[10px] tracking-[0.04em] text-mute break-all">
                      {selected.id}
                    </p>
                  </div>
                </div>
              </header>

              <div className="flex flex-1 flex-col gap-6 p-5">
                <div>
                  <label
                    htmlFor="user-role"
                    className="mb-2 flex items-center gap-2 text-[13px] font-medium text-dim"
                  >
                    <Shield size={14} className="text-accent" />
                    Роль
                  </label>
                  <select
                    id="user-role"
                    value={draftRole}
                    onChange={(e) =>
                      setDraftRole(e.target.value as UserRole)
                    }
                    className="h-11 w-full rounded-md border-[length:var(--theme-border-width)] border-hairline-strong bg-panel-2 px-3.5 text-[15px] text-paper outline-none focus:border-paper focus:shadow-[0_0_0_1px_var(--color-paper)]"
                  >
                    {USER_ROLES.map((role) => (
                      <option key={role} value={role}>
                        {userRoleLabel[role]}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <p className="mb-2 flex items-center gap-2 text-[13px] font-medium text-dim">
                    <Ban size={14} className="text-accent" />
                    Статус доступа
                  </p>
                  <button
                    type="button"
                    role="switch"
                    aria-checked={draftBlocked}
                    onClick={() => setDraftBlocked((v) => !v)}
                    className={cn(
                      "flex w-full items-center justify-between rounded-lg border border-hairline px-4 py-3.5 text-left transition-colors",
                      draftBlocked
                        ? "bg-reject/10 border-reject/25"
                        : "bg-panel-2/50 hover:bg-panel-2",
                    )}
                  >
                    <div>
                      <p className="text-[14px] font-medium text-paper">
                        {draftBlocked ? "Заблокирован" : "Активен"}
                      </p>
                      <p className="mt-0.5 text-[12px] text-mute">
                        {draftBlocked
                          ? "Вход в систему недоступен"
                          : "Может входить в систему"}
                      </p>
                    </div>
                    <span
                      className={cn(
                        "relative h-6 w-11 rounded-full transition-colors",
                        draftBlocked ? "bg-reject" : "bg-accent",
                      )}
                    >
                      <span
                        className={cn(
                          "absolute top-0.5 size-5 rounded-full bg-paper transition-transform",
                          draftBlocked ? "left-0.5" : "left-[1.35rem]",
                        )}
                      />
                    </span>
                  </button>
                </div>

                <AnimatePresence>
                  {(error || flash) && (
                    <motion.p
                      initial={{ opacity: 0, y: 4 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0 }}
                      className={cn(
                        "rounded-md px-3 py-2 text-[13px]",
                        error
                          ? "bg-reject/10 text-reject"
                          : "bg-signal/15 text-signal",
                      )}
                    >
                      {error ?? flash}
                    </motion.p>
                  )}
                </AnimatePresence>

                <div className="mt-auto flex flex-wrap gap-2 border-t border-hairline pt-5">
                  <Button
                    disabled={!dirty || saving}
                    onClick={() => void applyChanges()}
                    data-cursor-label="Сохранить"
                  >
                    {saving ? (
                      <LoaderCircle size={14} className="animate-spin" />
                    ) : (
                      <Check size={14} />
                    )}
                    Сохранить
                  </Button>
                  <Button
                    variant="line"
                    disabled={!dirty || saving}
                    onClick={resetDraft}
                  >
                    Отменить
                  </Button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.aside>
    </div>
  );
}

function FilterChip({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "rounded-md px-2.5 py-1 text-[12px] font-medium transition-colors",
        active
          ? "bg-panel-2 text-paper"
          : "text-mute hover:bg-panel-2/60 hover:text-paper",
      )}
    >
      {children}
    </button>
  );
}
