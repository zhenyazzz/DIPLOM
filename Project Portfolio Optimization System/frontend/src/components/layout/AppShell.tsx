import { useEffect, useMemo, useState } from "react";
import { Link, NavLink, Navigate, Outlet, useLocation, useNavigate } from "react-router-dom";
import { LogOut } from "lucide-react";
import { AnimatePresence, LayoutGroup, motion } from "motion/react";
import { logout as apiLogout } from "../../api/identity/auth";
import { userRoleLabel } from "../../api/identity/types";
import { ThemeToggle } from "../ui/ThemeToggle";
import { Button } from "../ui/Button";
import { cn } from "../../lib/cn";
import { currentUser } from "../../data/profile";
import { restoreSession } from "../../lib/authBootstrap";
import { clearSession, getSession, isAdmin, isSignedIn } from "../../lib/session";
import { easeOutExpo, pageTransition, springSoft } from "../../lib/motion";

const baseLinks = [
  { to: "/projects", label: "Проекты" },
  { to: "/scenario", label: "Оптимизация" },
];

export function AppShell() {
  const location = useLocation();
  const navigate = useNavigate();
  const [ready, setReady] = useState(() => isSignedIn());
  const [session, setSessionState] = useState(() => getSession());
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (isSignedIn()) {
        if (!cancelled) {
          setSessionState(getSession());
          setReady(true);
        }
        return;
      }
      if (getSession()) {
        await restoreSession();
      }
      if (!cancelled) {
        setSessionState(getSession());
        setReady(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const links = useMemo(
    () =>
      isAdmin()
        ? [...baseLinks, { to: "/users", label: "Пользователи" }]
        : baseLinks,
    [session?.role],
  );

  if (!ready) {
    return (
      <div className="flex min-h-svh items-center justify-center bg-black">
        <p className="text-[14px] font-medium text-mute">Загрузка сессии…</p>
      </div>
    );
  }

  if (!isSignedIn()) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (location.pathname.startsWith("/users") && !isAdmin()) {
    return <Navigate to="/projects" replace />;
  }

  const shortName = session?.displayName ?? currentUser.shortName;
  const roleLabel = session?.role
    ? userRoleLabel[session.role]
    : currentUser.role;
  const initials =
    session?.displayName
      ?.split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0]?.toUpperCase() ?? "")
      .join("") || currentUser.initials;

  async function logout() {
    setLoggingOut(true);
    try {
      await apiLogout();
    } finally {
      clearSession();
      navigate("/", { replace: true });
      setLoggingOut(false);
    }
  }

  return (
    <div className="min-h-svh bg-black text-dim">
      <motion.header
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, ease: easeOutExpo }}
        className="sticky top-0 z-30 border-b border-hairline bg-black/90 backdrop-blur-md"
      >
        <div className="mx-auto flex h-14 max-w-[1440px] items-center justify-between gap-6 px-5 lg:px-8">
          <NavLink to="/projects" className="group flex min-w-0 items-center gap-2.5">
            <motion.span
              aria-hidden
              className="inline-block size-2 rotate-45 rounded-[1px] bg-accent"
              whileHover={{ rotate: 135, scale: 1.15 }}
              transition={springSoft}
            />
            <p className="truncate text-[15px] font-medium tracking-[-0.02em] text-paper transition-colors group-hover:text-accent">
              PORTFEL
            </p>
          </NavLink>

          <LayoutGroup id="app-nav">
            <nav className="flex items-center gap-1" aria-label="Разделы">
              {links.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  className={({ isActive }) =>
                    cn(
                      "relative rounded-md px-3.5 py-1.5 text-[14px] font-medium tracking-[-0.01em]",
                      isActive ? "text-paper" : "text-mute hover:text-paper",
                    )
                  }
                >
                  {({ isActive }) => (
                    <>
                      {isActive && (
                        <motion.span
                          layoutId="nav-active-pill"
                          className="absolute inset-0 rounded-md bg-panel-2"
                          transition={springSoft}
                        />
                      )}
                      <span className="relative z-10">{link.label}</span>
                    </>
                  )}
                </NavLink>
              ))}
            </nav>
          </LayoutGroup>

          <motion.div
            className="flex items-center gap-2 sm:gap-3"
            initial={{ opacity: 0, x: 8 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.12, duration: 0.4, ease: easeOutExpo }}
          >
            <ThemeToggle />
            <Link
              to="/profile"
              data-cursor-label="Профиль"
              className={cn(
                "group flex items-center gap-2.5 rounded-md px-1.5 py-1 transition-colors",
                location.pathname === "/profile"
                  ? "bg-panel-2"
                  : "hover:bg-panel-2/70",
              )}
            >
              <span className="grid size-7 place-items-center rounded-md border border-hairline bg-ink font-mono text-[10px] tracking-tight text-accent">
                {initials}
              </span>
              <div className="hidden text-right sm:block">
                <p className="text-[13px] font-medium text-paper transition-colors group-hover:text-accent">
                  {shortName}
                </p>
                <p className="text-[11px] text-mute">{roleLabel}</p>
              </div>
            </Link>
            <Button
              variant="ghost"
              type="button"
              onClick={() => void logout()}
              disabled={loggingOut}
              data-cursor-label="Выйти"
              className="h-9 px-2.5 text-[13px] text-mute"
              aria-label="Выйти"
            >
              <LogOut size={15} />
              <span className="hidden md:inline">Выйти</span>
            </Button>
          </motion.div>
        </div>
      </motion.header>

      <main className="mx-auto max-w-[1440px] px-5 py-6 lg:px-8 lg:py-8">
        <AnimatePresence mode="wait">
          <motion.div
            key={location.pathname}
            initial={pageTransition.initial}
            animate={pageTransition.animate}
            exit={pageTransition.exit}
            transition={pageTransition.transition}
          >
            <Outlet />
          </motion.div>
        </AnimatePresence>
      </main>
    </div>
  );
}
