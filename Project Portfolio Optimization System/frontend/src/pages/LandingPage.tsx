import { useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import {
  AnimatePresence,
  motion,
  useMotionValueEvent,
  useScroll,
  useTransform,
} from "motion/react";
import { RevealOnScroll } from "../components/motion/RevealOnScroll";
import { Button } from "../components/ui/Button";
import { ThemeToggle } from "../components/ui/ThemeToggle";
import { PortfolioScene } from "../components/viz/PortfolioScene";
import {
  BUDGET_CAP,
  STAGE_META,
  portfolioProjects,
  runLocalOptimization,
  type SimNode,
  type SimStage,
} from "../data/portfolio";
import { easeOutExpo, fadeUp, staggerContainer } from "../lib/motion";

const DEMO_STAGES: Exclude<SimStage, "idle" | "done">[] = [
  "normalize",
  "knapsack",
  "dependencies",
  "final",
];

function useDemoFrame(progress: ReturnType<typeof useScroll>["scrollYProgress"]) {
  const result = useMemo(
    () => runLocalOptimization(portfolioProjects, BUDGET_CAP),
    [],
  );
  const [frame, setFrame] = useState(() => buildFrame(0, result.nodes));

  useMotionValueEvent(progress, "change", (p) => {
    setFrame(buildFrame(p, result.nodes));
  });

  return frame;
}

function buildFrame(progress: number, optimized: SimNode[]) {
  const clamped = Math.min(0.999, Math.max(0, progress));
  const idx = Math.min(
    DEMO_STAGES.length - 1,
    Math.floor(clamped * DEMO_STAGES.length),
  );
  const stage = DEMO_STAGES[idx];
  const local = (clamped * DEMO_STAGES.length) % 1;

  const nodes: SimNode[] = portfolioProjects.map((p) => {
    const opt = optimized.find((n) => n.id === p.id);
    const selected = Boolean(opt?.selected);

    if (stage === "normalize") {
      return {
        ...p,
        evaluated: local > 0.15,
        selected: false,
      };
    }
    if (stage === "knapsack") {
      return {
        ...p,
        evaluated: true,
        selected: selected && local > 0.2,
        rejectReason: !selected && local > 0.55 ? "бюджет" : undefined,
      };
    }
    if (stage === "dependencies") {
      return {
        ...p,
        evaluated: true,
        selected,
        rejectReason: !selected ? opt?.rejectReason ?? "зависимость" : undefined,
      };
    }
    return {
      ...p,
      evaluated: true,
      selected,
      rejectReason: selected ? undefined : opt?.rejectReason ?? "вне портфеля",
    };
  });

  const picked = nodes.filter((n) => n.selected);
  return {
    stage,
    stageIndex: idx,
    meta: STAGE_META[stage],
    nodes,
    selectedCount: picked.length,
    budgetUsed: picked.reduce((s, n) => s + n.budget, 0),
    scoreSum: picked.reduce((s, n) => s + n.score, 0),
  };
}

function formatBudget(v: number) {
  return `₽${(v / 1_000_000).toFixed(1)} млн`;
}

export function LandingPage() {
  const navigate = useNavigate();
  const heroRef = useRef<HTMLElement>(null);
  const demoRef = useRef<HTMLElement>(null);

  const { scrollYProgress: heroProgress } = useScroll({
    target: heroRef,
    offset: ["start start", "end start"],
  });
  const sceneY = useTransform(heroProgress, [0, 1], [0, 90]);
  const sceneOpacity = useTransform(heroProgress, [0, 0.9], [1, 0.4]);
  const copyY = useTransform(heroProgress, [0, 1], [0, 36]);

  const { scrollYProgress: demoProgress } = useScroll({
    target: demoRef,
    offset: ["start start", "end end"],
  });
  const demoBar = useTransform(demoProgress, [0, 1], ["0%", "100%"]);
  const frame = useDemoFrame(demoProgress);

  const heroNodes = useMemo<SimNode[]>(
    () =>
      portfolioProjects.slice(0, 20).map((p) => ({
        ...p,
        selected: p.score > 0.74,
        evaluated: true,
      })),
    [],
  );

  return (
    <div className="bg-black text-paper">
      {/* Hero */}
      <section ref={heroRef} className="relative min-h-svh overflow-hidden">
        <motion.div className="absolute inset-0" style={{ y: sceneY, opacity: sceneOpacity }}>
          <PortfolioScene
            nodes={heroNodes}
            stage="normalize"
            selectedId={null}
            onSelect={() => undefined}
            lite
            className="h-full min-h-svh w-full"
          />
        </motion.div>
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-r from-black via-black/86 to-black/28" />
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black via-transparent to-black/45" />

        <motion.header
          className="relative z-20 mx-auto flex h-16 max-w-[1200px] items-center justify-between gap-4 px-5 lg:px-8"
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: easeOutExpo }}
        >
          <Link to="/" className="flex items-center gap-2.5">
            <motion.span
              aria-hidden
              className="inline-block size-2.5 rotate-45 rounded-[1px] bg-accent"
              animate={{ rotate: [45, 135, 45] }}
              transition={{ duration: 7, repeat: Infinity, ease: "easeInOut" }}
            />
            <span className="text-[15px] font-medium tracking-[-0.02em] text-paper">
              PORTFEL
            </span>
          </Link>
          <div className="flex items-center gap-2 sm:gap-3">
            <ThemeToggle />
            <Button
              variant="ghost"
              className="h-10 px-3 text-[14px]"
              data-cursor-label="Войти"
              onClick={() => navigate("/login")}
            >
              Войти
            </Button>
            <Button
              className="h-10 px-4 text-[14px]"
              data-cursor-label="Регистрация"
              onClick={() => navigate("/register")}
            >
              Регистрация
            </Button>
          </div>
        </motion.header>

        <motion.div
          className="relative z-10 mx-auto flex min-h-[calc(100svh-4rem)] max-w-[1200px] flex-col justify-end px-5 pb-20 pt-10 lg:justify-center lg:px-8 lg:pb-28"
          style={{ y: copyY }}
        >
          <motion.div
            className="max-w-xl"
            variants={staggerContainer}
            initial="hidden"
            animate="show"
          >
            <motion.p variants={fadeUp} className="meta mb-5 text-accent">
              B2B · инвестиционный комитет
            </motion.p>
            <motion.h1
              variants={fadeUp}
              className="font-display text-[clamp(2.75rem,7vw,5.25rem)] leading-[1.02] text-paper"
            >
              PORTFEL
            </motion.h1>
            <motion.p
              variants={fadeUp}
              className="mt-5 max-w-md text-[17px] leading-relaxed text-dim"
            >
              Пространственная модель отбора проектов: SAW, бюджет и зависимости —
              в одном сценарии для комитета.
            </motion.p>
            <motion.div variants={fadeUp} className="mt-8 flex flex-wrap gap-3">
              <Button data-cursor-label="Начать" onClick={() => navigate("/register")}>
                Создать аккаунт
                <ArrowUpRight size={14} />
              </Button>
              <Button
                variant="line"
                data-cursor-label="Демо"
                onClick={() =>
                  demoRef.current?.scrollIntoView({ behavior: "smooth", block: "start" })
                }
              >
                Смотреть оптимизацию
              </Button>
            </motion.div>
          </motion.div>
        </motion.div>
      </section>

      {/* Problem → product */}
      <section className="border-t border-hairline">
        <div className="mx-auto grid max-w-[1200px] gap-12 px-5 py-24 lg:grid-cols-[0.95fr_1.05fr] lg:items-end lg:gap-20 lg:px-8 lg:py-28">
          <RevealOnScroll>
            <p className="meta mb-4">Для предприятия</p>
            <h2 className="font-display text-[clamp(2rem,3.6vw,3rem)] leading-[1.06] text-paper">
              От Excel к воспроизводимому решению комитета
            </h2>
          </RevealOnScroll>
          <RevealOnScroll delay={0.08}>
            <p className="max-w-xl text-[16px] leading-relaxed text-dim">
              PORTFEL собирает инициативы, веса критериев и финансовые снимки в единый
              портфельный сценарий. Комитет видит, почему проект вошёл или остался за
              бюджетом — без ручного пересчёта таблиц.
            </p>
            <div className="mt-8 flex flex-wrap gap-2">
              {["SAW", "бюджет", "зависимости", "аудит"].map((tag) => (
                <span
                  key={tag}
                  className="border border-hairline px-3 py-1.5 text-[12px] text-mute"
                >
                  {tag}
                </span>
              ))}
            </div>
          </RevealOnScroll>
        </div>
      </section>

      {/* Scroll-driven product story — the interesting part */}
      <section ref={demoRef} className="relative border-t border-hairline">
        <div className="h-[320vh]">
          <div className="sticky top-0 flex h-svh flex-col overflow-hidden bg-black">
            <div className="mx-auto flex w-full max-w-[1200px] items-end justify-between gap-6 px-5 pt-6 lg:px-8">
              <div className="min-w-0">
                <p className="meta mb-2 text-accent">Живой сценарий</p>
                <AnimatePresence mode="wait">
                  <motion.h2
                    key={frame.stage}
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    transition={{ duration: 0.35, ease: easeOutExpo }}
                    className="font-display text-[clamp(1.7rem,3vw,2.6rem)] leading-[1.08] text-paper"
                  >
                    {frame.meta.label}
                  </motion.h2>
                </AnimatePresence>
                <AnimatePresence mode="wait">
                  <motion.p
                    key={`${frame.stage}-d`}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.3 }}
                    className="mt-2 max-w-lg text-[14px] text-mute"
                  >
                    {frame.meta.detail}
                  </motion.p>
                </AnimatePresence>
              </div>

              <div className="hidden shrink-0 grid-cols-3 gap-6 sm:grid">
                <Metric label="Отобрано" value={String(frame.selectedCount)} />
                <Metric label="Бюджет" value={formatBudget(frame.budgetUsed)} />
                <Metric label="Σ SAW" value={frame.scoreSum.toFixed(2)} />
              </div>
            </div>

            <div className="relative mt-4 min-h-0 flex-1">
              <PortfolioScene
                nodes={frame.nodes}
                stage={frame.stage}
                selectedId={null}
                onSelect={() => undefined}
                lite
                className="h-full w-full"
              />
              <div className="pointer-events-none absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black to-transparent" />
            </div>

            <div className="mx-auto w-full max-w-[1200px] px-5 pb-6 lg:px-8">
              <div className="mb-3 flex items-center justify-between gap-4">
                <div className="flex gap-2">
                  {DEMO_STAGES.map((s, i) => (
                    <span
                      key={s}
                      className={
                        i === frame.stageIndex
                          ? "font-mono text-[11px] tracking-wider text-accent"
                          : "font-mono text-[11px] tracking-wider text-mute/50"
                      }
                    >
                      {String(i + 1).padStart(2, "0")}
                    </span>
                  ))}
                </div>
                <p className="text-[12px] text-mute">листайте вниз — сценарий идёт дальше</p>
              </div>
              <div className="h-px overflow-hidden bg-hairline">
                <motion.div className="h-full origin-left bg-accent" style={{ width: demoBar }} />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Method */}
      <section className="border-t border-hairline bg-ink/35">
        <div className="mx-auto max-w-[1200px] px-5 py-24 lg:px-8 lg:py-28">
          <RevealOnScroll className="mb-14 max-w-lg">
            <p className="meta mb-4">Метод</p>
            <h2 className="font-display text-[clamp(2rem,3.6vw,3rem)] leading-[1.06] text-paper">
              Три шага до утверждённого портфеля
            </h2>
          </RevealOnScroll>

          <ol className="grid gap-10 md:grid-cols-3 md:gap-8">
            {[
              {
                step: "01",
                title: "Оценить",
                text: "Критерии и веса SAW фиксируются один раз — балл проекта считается прозрачно.",
              },
              {
                step: "02",
                title: "Ограничить",
                text: "Бюджет периода и зависимости между инициативами задают допустимую область.",
              },
              {
                step: "03",
                title: "Оптимизировать",
                text: "Бинарный отбор 0–1 максимизирует ценность портфеля под ваши ограничения.",
              },
            ].map((item, i) => (
              <li key={item.step} className="relative list-none">
                <RevealOnScroll delay={i * 0.08}>
                  {i < 2 && (
                    <span
                      aria-hidden
                      className="absolute top-3 left-[calc(100%+0.5rem)] hidden h-px w-[calc(100%-1rem)] bg-hairline-strong md:block"
                    />
                  )}
                  <span className="font-mono text-[12px] tracking-[0.1em] text-accent">
                    {item.step}
                  </span>
                  <h3 className="mt-3 font-display text-2xl text-paper">{item.title}</h3>
                  <p className="mt-3 text-[15px] leading-relaxed text-mute">{item.text}</p>
                </RevealOnScroll>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* CTA */}
      <section className="border-t border-hairline">
        <div className="mx-auto flex max-w-[1200px] flex-col items-start justify-between gap-8 px-5 py-24 sm:flex-row sm:items-end lg:px-8 lg:py-28">
          <RevealOnScroll>
            <p className="meta mb-4">Доступ</p>
            <h2 className="font-display max-w-[16ch] text-[clamp(1.85rem,3vw,2.5rem)] leading-[1.1] text-paper">
              Откройте рабочее пространство команды
            </h2>
          </RevealOnScroll>
          <RevealOnScroll delay={0.1}>
            <div className="flex flex-wrap gap-3">
              <Button data-cursor-label="Регистрация" onClick={() => navigate("/register")}>
                Регистрация
                <ArrowUpRight size={14} />
              </Button>
              <Button variant="line" data-cursor-label="Войти" onClick={() => navigate("/login")}>
                Уже есть аккаунт
              </Button>
            </div>
          </RevealOnScroll>
        </div>
      </section>

      <footer className="border-t border-hairline">
        <RevealOnScroll>
          <div className="mx-auto flex max-w-[1200px] items-center justify-between gap-4 px-5 py-6 lg:px-8">
            <p className="text-[13px] text-mute">PORTFEL · B2B portfolio optimization</p>
            <p className="text-[12px] text-mute">SAW · NPV · 0-1</p>
          </div>
        </RevealOnScroll>
      </footer>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="text-right">
      <p className="meta !normal-case !tracking-normal">{label}</p>
      <p className="num mt-1 text-[18px] text-paper">{value}</p>
    </div>
  );
}
