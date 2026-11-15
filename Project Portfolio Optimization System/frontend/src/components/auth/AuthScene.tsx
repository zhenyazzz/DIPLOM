import { useMemo, type ReactNode } from "react";
import { Link } from "react-router-dom";
import { motion } from "motion/react";
import { ThemeToggle } from "../ui/ThemeToggle";
import { PortfolioScene } from "../viz/PortfolioScene";
import { portfolioProjects, type SimNode } from "../../data/portfolio";
import { easeOutExpo, fadeUp, staggerContainer } from "../../lib/motion";

export function AuthScene({
  children,
  title,
  subtitle,
}: {
  children: ReactNode;
  title: string;
  subtitle: string;
}) {
  const nodes = useMemo<SimNode[]>(
    () =>
      portfolioProjects.slice(0, 18).map((p) => ({
        ...p,
        selected: p.score > 0.7,
        evaluated: true,
      })),
    [],
  );

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
        className="absolute top-5 right-5 z-20 flex items-center gap-3"
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.35, duration: 0.45, ease: easeOutExpo }}
      >
        <Link
          to="/"
          className="text-[13px] text-mute transition-colors hover:text-paper"
        >
          На главную
        </Link>
        <ThemeToggle />
      </motion.div>

      <div className="relative z-10 mx-auto grid min-h-svh max-w-[1200px] items-center gap-12 px-5 py-16 lg:grid-cols-[1.2fr_0.8fr] lg:px-8">
        <motion.div variants={staggerContainer} initial="hidden" animate="show">
          <motion.div variants={fadeUp} className="mb-8 flex items-center gap-3">
            <Link to="/" className="flex items-center gap-3">
              <motion.span
                aria-hidden
                className="inline-block size-2.5 rotate-45 rounded-[1px] bg-accent"
                animate={{ rotate: [45, 135, 45] }}
                transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
              />
              <span className="text-[13px] font-medium text-accent">PORTFEL</span>
            </Link>
            <motion.span
              className="h-px max-w-20 flex-1 origin-left bg-hairline-strong"
              initial={{ scaleX: 0 }}
              animate={{ scaleX: 1 }}
              transition={{ delay: 0.25, duration: 0.6, ease: easeOutExpo }}
            />
            <span className="text-[12px] text-mute">B2B</span>
          </motion.div>
          <motion.h1
            variants={fadeUp}
            className="font-display max-w-[14ch] text-[clamp(2.4rem,5.5vw,4rem)] leading-[1.02] text-paper"
          >
            {title}
          </motion.h1>
          <motion.p
            variants={fadeUp}
            className="mt-6 max-w-md text-[16px] leading-relaxed text-dim"
          >
            {subtitle}
          </motion.p>
        </motion.div>

        {children}
      </div>
    </div>
  );
}
