import { useMemo, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowUpRight, LoaderCircle } from "lucide-react";
import { motion } from "motion/react";
import { Button } from "../components/ui/Button";
import { Field, Input } from "../components/ui/Input";
import { ThemeToggle } from "../components/ui/ThemeToggle";
import { PortfolioScene } from "../components/viz/PortfolioScene";
import { portfolioProjects, type SimNode } from "../data/portfolio";
import {
  easeOutExpo,
  fadeUp,
  scaleIn,
  staggerContainer,
} from "../lib/motion";

export function LoginPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const nodes = useMemo<SimNode[]>(
    () =>
      portfolioProjects.slice(0, 18).map((p) => ({
        ...p,
        selected: p.score > 0.7,
        evaluated: true,
      })),
    [],
  );

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    await new Promise((r) => setTimeout(r, 600));
    navigate("/scenario");
  }

  return (
    <div className="relative min-h-svh overflow-hidden bg-black text-paper">
      <motion.div
        className="absolute inset-0 opacity-80"
        initial={{ opacity: 0, scale: 1.04 }}
        animate={{ opacity: 0.8, scale: 1 }}
        transition={{ duration: 1.1, ease: easeOutExpo }}
      >
        <PortfolioScene
          nodes={nodes}
          stage="normalize"
          selectedId={null}
          onSelect={() => undefined}
          lite
          className="h-full min-h-svh w-full"
        />
      </motion.div>
      <div className="pointer-events-none absolute inset-0 bg-gradient-to-r from-black via-black/88 to-black/35" />
      <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black via-transparent to-black/40" />

      <motion.div
        className="absolute top-5 right-5 z-20"
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.35, duration: 0.45, ease: easeOutExpo }}
      >
        <ThemeToggle />
      </motion.div>

      <div className="relative z-10 mx-auto grid min-h-svh max-w-[1200px] items-center gap-12 px-5 py-16 lg:grid-cols-[1.2fr_0.8fr] lg:px-8">
        <motion.div
          variants={staggerContainer}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={fadeUp} className="mb-8 flex items-center gap-3">
            <motion.span
              aria-hidden
              className="inline-block size-2.5 rotate-45 rounded-[1px] bg-accent"
              animate={{ rotate: [45, 135, 45] }}
              transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
            />
            <span className="text-[13px] font-medium text-accent">PORTFEL</span>
            <motion.span
              className="h-px max-w-20 flex-1 origin-left bg-hairline-strong"
              initial={{ scaleX: 0 }}
              animate={{ scaleX: 1 }}
              transition={{ delay: 0.25, duration: 0.6, ease: easeOutExpo }}
            />
            <span className="text-[12px] text-mute">SAW · NPV · 0-1</span>
          </motion.div>
          <motion.h1
            variants={fadeUp}
            className="font-display max-w-[14ch] text-[clamp(2.8rem,6.5vw,5rem)] leading-[1.02] text-paper"
          >
            Система оптимизации портфеля
          </motion.h1>
          <motion.p
            variants={fadeUp}
            className="mt-6 max-w-md text-[16px] leading-relaxed text-dim"
          >
            Интерактивная пространственная модель инвестиционных проектов: бюджет, ценность и
            зависимости — в одной симуляции.
          </motion.p>
        </motion.div>

        <motion.form
          onSubmit={onSubmit}
          variants={scaleIn}
          initial="hidden"
          animate="show"
          className="surface shadow-card w-full max-w-md justify-self-end bg-panel/95 p-7 backdrop-blur-md sm:p-8"
        >
          <p className="text-[12px] font-medium text-mute">Access</p>
          <h2 className="mt-2 font-display text-[1.75rem] text-paper">Вход</h2>
          <p className="mt-2 text-[14px] text-mute">
            Демо без backend — любые данные откроют интерфейс.
          </p>

          <motion.div
            className="mt-6 grid gap-4"
            variants={staggerContainer}
            initial="hidden"
            animate="show"
          >
            <motion.div variants={fadeUp}>
              <Field label="Email" htmlFor="email">
                <Input id="email" type="email" defaultValue="analyst@enterprise.local" required />
              </Field>
            </motion.div>
            <motion.div variants={fadeUp}>
              <Field label="Пароль" htmlFor="password">
                <Input id="password" type="password" defaultValue="password" required />
              </Field>
            </motion.div>
          </motion.div>

          <Button type="submit" className="mt-6 w-full" disabled={loading} data-cursor-label="Войти">
            {loading ? (
              <>
                <LoaderCircle className="animate-spin" size={14} />
                Вход…
              </>
            ) : (
              <>
                Продолжить
                <ArrowUpRight size={14} />
              </>
            )}
          </Button>
        </motion.form>
      </div>
    </div>
  );
}
