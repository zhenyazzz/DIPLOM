import { useNavigate } from "react-router-dom";
import {
  ArrowUpRight,
  Bell,
  Building2,
  Calendar,
  Check,
  Globe2,
  LayoutGrid,
  LogOut,
  Mail,
  MapPin,
  Phone,
  Shield,
} from "lucide-react";
import { motion } from "motion/react";
import { Button } from "../components/ui/Button";
import { currentUser } from "../data/profile";
import { cn } from "../lib/cn";
import { logout as apiLogout } from "../api/identity/auth";
import { userRoleLabel } from "../api/identity/types";
import { clearSession, getSession } from "../lib/session";
import {
  easeOutExpo,
  fadeUp,
  listItem,
  metricPop,
  springSoft,
  staggerContainer,
  staggerFast,
} from "../lib/motion";

const toneDot: Record<(typeof currentUser.activity)[number]["tone"], string> = {
  accent: "bg-accent",
  signal: "bg-signal",
  amber: "bg-amber",
  mute: "bg-mute",
};

export function ProfilePage() {
  const navigate = useNavigate();
  const session = getSession();
  const user = {
    ...currentUser,
    email: session?.email ?? currentUser.email,
    firstName: session?.displayName?.split(/\s+/)[0] ?? currentUser.firstName,
    lastName:
      session?.displayName?.split(/\s+/).slice(1).join(" ") || currentUser.lastName,
  };

  async function logout() {
    try {
      await apiLogout();
    } finally {
      clearSession();
      navigate("/", { replace: true });
    }
  }

  const roleLabel = session?.role
    ? userRoleLabel[session.role]
    : user.role;

  return (
    <motion.div
      className="space-y-5"
      variants={staggerContainer}
      initial="hidden"
      animate="show"
    >
      {/* Identity hero — one composition, brand-adjacent */}
      <motion.section
        variants={fadeUp}
        className="relative overflow-hidden rounded-xl border border-hairline bg-panel"
      >
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 opacity-[0.55]"
          style={{
            background:
              "radial-gradient(ellipse 80% 60% at 0% 0%, color-mix(in srgb, var(--color-accent) 22%, transparent), transparent 55%), radial-gradient(ellipse 50% 40% at 100% 100%, color-mix(in srgb, var(--color-signal) 12%, transparent), transparent 50%)",
          }}
        />
        <div
          aria-hidden
          className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-accent/50 to-transparent"
        />

        <div className="relative grid gap-8 p-6 sm:p-8 lg:grid-cols-[1.15fr_0.85fr] lg:items-end lg:gap-10">
          <div className="min-w-0">
            <p className="meta mb-4">Профиль · {user.id}</p>

            <div className="flex flex-wrap items-start gap-5">
              <motion.div
                initial={{ opacity: 0, scale: 0.9, rotate: -6 }}
                animate={{ opacity: 1, scale: 1, rotate: 0 }}
                transition={springSoft}
                className="relative grid size-[4.5rem] shrink-0 place-items-center rounded-xl border border-hairline-strong bg-ink sm:size-20"
              >
                <span className="font-display text-2xl tracking-tight text-paper sm:text-3xl">
                  {user.initials}
                </span>
                <motion.span
                  aria-hidden
                  className="absolute -right-1 -bottom-1 size-3 rotate-45 rounded-[1px] bg-accent"
                  animate={{ rotate: [45, 135, 45] }}
                  transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }}
                />
              </motion.div>

              <div className="min-w-0 flex-1">
                <h1 className="font-display text-[clamp(2rem,4vw,2.75rem)] leading-[1.05] text-paper">
                  {user.firstName} {user.lastName}
                </h1>
                <p className="mt-2 text-[15px] text-mute">{user.title}</p>
                <div className="mt-4 flex flex-wrap gap-2">
                  <span className="inline-flex items-center gap-1.5 rounded-full border border-hairline bg-accent-soft px-3 py-1 text-[12px] font-medium text-accent">
                    <Shield size={12} />
                    {roleLabel}
                  </span>
                  <span className="inline-flex items-center gap-1.5 rounded-full border border-hairline bg-panel-2/60 px-3 py-1 text-[12px] text-dim">
                    <Building2 size={12} />
                    {user.department}
                  </span>
                </div>
              </div>
            </div>

            <p className="mt-6 max-w-xl text-[15px] leading-relaxed text-dim">{user.bio}</p>

            <div className="mt-6 flex flex-wrap gap-2">
              <Button
                data-cursor-label="Сценарий"
                onClick={() => navigate("/scenario")}
              >
                К оптимизации
                <ArrowUpRight size={14} />
              </Button>
              <Button
                variant="line"
                data-cursor-label="Реестр"
                onClick={() => navigate("/projects")}
              >
                <LayoutGrid size={14} />
                Реестр
              </Button>
              <Button
                variant="danger"
                data-cursor-label="Выйти"
                onClick={() => void logout()}
              >
                <LogOut size={14} />
                Выйти
              </Button>
            </div>
          </div>

          <motion.div
            variants={staggerFast}
            initial="hidden"
            animate="show"
            className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1"
          >
            {[
              { icon: Mail, label: "Email", value: user.email },
              { icon: Phone, label: "Телефон", value: user.phone },
              { icon: MapPin, label: "Локация", value: user.location },
              { icon: Calendar, label: "В системе с", value: user.joined },
            ].map((item) => (
              <motion.div
                key={item.label}
                variants={metricPop}
                className="flex items-start gap-3 rounded-lg border border-hairline bg-black/25 px-3.5 py-3 backdrop-blur-[2px]"
              >
                <item.icon className="mt-0.5 size-3.5 shrink-0 text-accent" strokeWidth={1.75} />
                <div className="min-w-0">
                  <p className="meta !normal-case !tracking-normal">{item.label}</p>
                  <p className="mt-0.5 truncate text-[13px] font-medium text-paper">{item.value}</p>
                </div>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </motion.section>

      {/* Stats strip */}
      <motion.div
        variants={staggerFast}
        initial="hidden"
        animate="show"
        className="grid grid-cols-2 overflow-hidden rounded-lg border border-hairline bg-panel sm:grid-cols-4"
      >
        {user.stats.map((stat) => (
          <motion.div
            key={stat.label}
            variants={metricPop}
            className="border-r border-hairline px-4 py-4 last:border-r-0"
          >
            <p className="meta">{stat.label}</p>
            <p className="num mt-2 text-2xl text-paper">{stat.value}</p>
            <p className="mt-1 text-[12px] text-mute">{stat.hint}</p>
          </motion.div>
        ))}
      </motion.div>

      <div className="grid gap-5 xl:grid-cols-[1.05fr_0.95fr]">
        {/* Competences */}
        <motion.section
          variants={fadeUp}
          className="overflow-hidden rounded-xl border border-hairline bg-panel"
        >
          <header className="border-b border-hairline px-5 py-4">
            <p className="meta mb-1">Компетенции</p>
            <h2 className="font-display text-2xl text-paper">Рабочий профиль</h2>
          </header>
          <div className="space-y-5 p-5">
            {user.competences.map((c, i) => (
              <motion.div
                key={c.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.12 + i * 0.06, duration: 0.4, ease: easeOutExpo }}
              >
                <div className="mb-2 flex items-end justify-between gap-3">
                  <div>
                    <p className="text-[14px] font-medium text-paper">{c.label}</p>
                    <p className="mt-0.5 text-[12px] text-mute">{c.hint}</p>
                  </div>
                  <span className="num text-[13px] text-accent">{Math.round(c.level * 100)}%</span>
                </div>
                <div className="h-1.5 overflow-hidden rounded-full bg-panel-2">
                  <motion.div
                    className="h-full rounded-full bg-accent"
                    initial={{ width: 0 }}
                    animate={{ width: `${c.level * 100}%` }}
                    transition={{
                      delay: 0.2 + i * 0.07,
                      duration: 0.7,
                      ease: easeOutExpo,
                    }}
                  />
                </div>
              </motion.div>
            ))}
          </div>
        </motion.section>

        {/* Activity */}
        <motion.section
          variants={fadeUp}
          className="overflow-hidden rounded-xl border border-hairline bg-panel"
        >
          <header className="border-b border-hairline px-5 py-4">
            <p className="meta mb-1">Активность</p>
            <h2 className="font-display text-2xl text-paper">Недавние события</h2>
          </header>
          <ul className="divide-y divide-hairline">
            {user.activity.map((event, i) => (
              <motion.li
                key={event.id}
                variants={listItem}
                initial="hidden"
                animate="show"
                transition={{ delay: 0.1 + i * 0.05 }}
                className="flex gap-3 px-5 py-4"
              >
                <span
                  className={cn("mt-1.5 size-2 shrink-0 rounded-full", toneDot[event.tone])}
                  aria-hidden
                />
                <div className="min-w-0">
                  <p className="font-mono text-[10px] tracking-[0.06em] text-mute uppercase">
                    {event.time}
                  </p>
                  <p className="mt-1 text-[14px] font-medium text-paper">{event.title}</p>
                  <p className="mt-0.5 text-[13px] text-mute">{event.detail}</p>
                </div>
              </motion.li>
            ))}
          </ul>
        </motion.section>
      </div>

      <div className="grid gap-5 lg:grid-cols-2">
        {/* Access */}
        <motion.section
          variants={fadeUp}
          className="overflow-hidden rounded-xl border border-hairline bg-panel"
        >
          <header className="border-b border-hairline px-5 py-4">
            <p className="meta mb-1">Доступ</p>
            <h2 className="text-[15px] font-medium text-paper">Права в PORTFEL</h2>
          </header>
          <ul className="grid gap-2 p-4 sm:grid-cols-2">
            {user.access.map((item, i) => (
              <motion.li
                key={item}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.15 + i * 0.04, duration: 0.35, ease: easeOutExpo }}
                className="flex items-center gap-2.5 rounded-lg border border-hairline bg-panel-2/40 px-3 py-2.5"
              >
                <span className="grid size-5 place-items-center rounded-full bg-signal/15 text-signal">
                  <Check size={12} strokeWidth={2.5} />
                </span>
                <span className="text-[13px] text-paper">{item}</span>
              </motion.li>
            ))}
          </ul>
        </motion.section>

        {/* Preferences */}
        <motion.section
          variants={fadeUp}
          className="overflow-hidden rounded-xl border border-hairline bg-panel"
        >
          <header className="border-b border-hairline px-5 py-4">
            <p className="meta mb-1">Предпочтения</p>
            <h2 className="text-[15px] font-medium text-paper">Рабочая среда</h2>
          </header>
          <dl className="divide-y divide-hairline">
            {(
              [
                { icon: Globe2, label: "Тема", value: user.preferences.theme },
                { icon: LayoutGrid, label: "Вид по умолчанию", value: user.preferences.defaultView },
                { icon: Bell, label: "Уведомления", value: user.preferences.notifications },
                { icon: Globe2, label: "Локаль", value: user.preferences.locale },
              ] as const
            ).map((row) => (
              <div
                key={row.label}
                className="flex items-center justify-between gap-4 px-5 py-3.5"
              >
                <dt className="flex items-center gap-2 text-[13px] text-mute">
                  <row.icon size={14} className="text-accent" />
                  {row.label}
                </dt>
                <dd className="text-right text-[13px] font-medium text-paper">{row.value}</dd>
              </div>
            ))}
          </dl>
        </motion.section>
      </div>
    </motion.div>
  );
}
