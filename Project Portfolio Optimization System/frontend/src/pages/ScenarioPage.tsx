import { useMemo, useState } from "react";
import {
  Check,
  Focus,
  Pause,
  Play,
  RotateCcw,
  SkipForward,
} from "lucide-react";
import { AnimatePresence, LayoutGroup, motion } from "motion/react";
import { Button } from "../components/ui/Button";
import { PortfolioScene } from "../components/viz/PortfolioScene";
import { useOptimizationSim } from "../hooks/useOptimizationSim";
import { STAGE_META, formatMoney, formatScore, type SimStage } from "../data/portfolio";
import { cn } from "../lib/cn";
import {
  easeOutExpo,
  fadeUp,
  listItem,
  metricPop,
  slideInRight,
  springSoft,
  staggerContainer,
  staggerFast,
} from "../lib/motion";

const STAGE_ORDER: Exclude<SimStage, "idle" | "done">[] = [
  "normalize",
  "knapsack",
  "dependencies",
  "final",
];

export function ScenarioPage() {
  const { snap, paused, start, pause, resume, reset } = useOptimizationSim();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [focusToken, setFocusToken] = useState(0);

  const selected = useMemo(
    () => snap.nodes.find((n) => n.id === selectedId) ?? null,
    [snap.nodes, selectedId],
  );

  const stageIndex = STAGE_ORDER.indexOf(
    snap.stage === "done" ? "final" : (snap.stage as (typeof STAGE_ORDER)[number]),
  );

  function focusSelected() {
    if (!selectedId) return;
    setFocusToken((t) => t + 1);
  }

  return (
    <motion.div
      className="flex min-h-[calc(100svh-5.5rem)] flex-col gap-4"
      variants={staggerContainer}
      initial="hidden"
      animate="show"
    >
      <motion.div
        variants={fadeUp}
        className="flex flex-wrap items-end justify-between gap-4 border-b border-hairline pb-4"
      >
        <div>
          <p className="meta mb-2">Сценарий · Инвест-цикл Q4</p>
          <h1 className="font-display text-3xl text-paper sm:text-4xl">
            Оптимизация портфеля
          </h1>
          <p className="mt-2 max-w-xl text-[15px] text-mute">
            Локальная симуляция: нормализация SAW → Knapsack 0-1 → зависимости → финальный набор.
          </p>
        </div>
        <motion.div
          className="flex flex-wrap items-center gap-2"
          variants={staggerFast}
          initial="hidden"
          animate="show"
        >
          <motion.span
            layout
            variants={metricPop}
            className={cn(
              "rounded-full px-3 py-1.5 text-[12px] font-medium tracking-[0.02em]",
              snap.status === "OPTIMAL"
                ? "bg-accent-soft text-accent"
                : snap.status === "RUNNING"
                  ? "bg-signal/15 text-signal"
                  : snap.status === "PAUSED"
                    ? "bg-amber/15 text-amber"
                    : "bg-panel-2 text-mute",
            )}
          >
            {snap.status}
          </motion.span>
          <motion.div variants={fadeUp}>
            <Button variant="ghost" onClick={reset} data-cursor-label="Сброс">
              <RotateCcw size={14} />
              Сброс
            </Button>
          </motion.div>
          <motion.div variants={fadeUp}>
            {snap.status === "RUNNING" ? (
              <Button variant="line" onClick={pause} data-cursor-label="Пауза">
                <Pause size={14} />
                Пауза
              </Button>
            ) : snap.status === "PAUSED" ? (
              <Button variant="line" onClick={resume} data-cursor-label="Продолжить">
                <SkipForward size={14} />
                Продолжить
              </Button>
            ) : (
              <Button onClick={start} data-cursor-label="Оптимизировать">
                <Play size={14} />
                Оптимизировать
              </Button>
            )}
          </motion.div>
          <motion.div variants={fadeUp}>
            <Button
              variant="line"
              disabled={snap.status !== "OPTIMAL"}
              data-cursor-label="Утвердить"
            >
              <Check size={14} />
              Утвердить
            </Button>
          </motion.div>
        </motion.div>
      </motion.div>

      <motion.div
        variants={staggerFast}
        initial="hidden"
        animate="show"
        className="grid grid-cols-2 overflow-hidden rounded-lg border border-hairline bg-panel sm:grid-cols-4"
      >
        {[
          { label: "Общий бюджет", value: formatMoney(snap.budgetCap) },
          {
            label: "Использовано",
            value:
              snap.stage === "idle"
                ? "—"
                : `${formatMoney(snap.budgetUsed)} (${Math.round((snap.budgetUsed / snap.budgetCap) * 100)}%)`,
          },
          {
            label: "Сумма баллов SAW",
            value: snap.stage === "idle" ? "—" : formatScore(snap.scoreSum),
          },
          {
            label: "Выбрано проектов",
            value:
              snap.stage === "idle"
                ? `0 / ${snap.nodes.length}`
                : `${snap.selectedCount} / ${snap.nodes.length}`,
          },
        ].map((m) => (
          <motion.div
            key={m.label}
            variants={metricPop}
            className="border-r border-hairline px-4 py-3.5 last:border-r-0"
          >
            <p className="meta">{m.label}</p>
            <AnimatePresence mode="wait" initial={false}>
              <motion.p
                key={m.value}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -4 }}
                transition={{ duration: 0.22, ease: easeOutExpo }}
                className="num mt-2 text-lg text-paper"
              >
                {m.value}
              </motion.p>
            </AnimatePresence>
          </motion.div>
        ))}
      </motion.div>

      <motion.div
        variants={fadeUp}
        className="grid min-h-0 flex-1 gap-4 xl:grid-cols-[1fr_320px]"
      >
        <motion.div
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.08, duration: 0.5, ease: easeOutExpo }}
          className="flex min-h-[480px] flex-col overflow-hidden rounded-xl border border-hairline bg-ink"
        >
          <PortfolioScene
            nodes={snap.nodes}
            stage={snap.stage}
            selectedId={selectedId}
            onSelect={setSelectedId}
            focusToken={focusToken}
            className="min-h-[480px] flex-1 xl:min-h-[560px]"
          />

          <div className="border-t border-hairline bg-panel">
            <div className="flex items-center gap-3 border-b border-hairline px-4 py-2.5">
              <p className="meta shrink-0">Этап</p>
              <AnimatePresence mode="wait" initial={false}>
                <motion.p
                  key={snap.stageLabel}
                  initial={{ opacity: 0, x: 6 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -4 }}
                  transition={{ duration: 0.25, ease: easeOutExpo }}
                  className="text-[14px] text-paper"
                >
                  {snap.stageLabel}
                </motion.p>
              </AnimatePresence>
              <p className="hidden text-[13px] text-mute sm:block">· {snap.stageDetail}</p>
              <div className="ml-auto flex items-center gap-2">
                <div className="h-1.5 w-28 overflow-hidden rounded-full bg-panel-2 sm:w-40">
                  <motion.div
                    className="h-full rounded-full bg-accent"
                    animate={{ width: `${Math.round(snap.progress * 100)}%` }}
                    transition={{ ease: "linear", duration: 0.15 }}
                  />
                </div>
                <span className="num text-[11px] text-mute">{Math.round(snap.progress * 100)}%</span>
              </div>
            </div>
            <LayoutGroup id="scenario-stages">
              <div className="grid grid-cols-2 sm:grid-cols-4">
                {STAGE_ORDER.map((id, i) => {
                  const meta = STAGE_META[id];
                  const active =
                    snap.stage === id || (snap.stage === "done" && id === "final");
                  const passed = stageIndex > i || snap.stage === "done";
                  return (
                    <motion.div
                      key={id}
                      layout
                      className={cn(
                        "relative border-r border-hairline px-3 py-3 last:border-r-0",
                        active ? "bg-accent-soft" : passed ? "bg-panel-2/50" : "",
                      )}
                      animate={{ opacity: active || passed ? 1 : 0.72 }}
                      transition={{ duration: 0.28, ease: easeOutExpo }}
                    >
                      {active && (
                        <motion.span
                          layoutId="stage-active-bar"
                          className="absolute inset-x-0 top-0 h-0.5 bg-accent"
                          transition={springSoft}
                        />
                      )}
                      <p className={cn("meta", active && "!text-accent")}>
                        0{i + 1} · {meta.label}
                      </p>
                      <p className="mt-1 text-[12px] leading-snug text-mute">{meta.detail}</p>
                    </motion.div>
                  );
                })}
              </div>
            </LayoutGroup>
          </div>
        </motion.div>

        <motion.aside
          className="flex flex-col gap-4"
          variants={slideInRight}
          initial="hidden"
          animate="show"
        >
          <section className="overflow-hidden rounded-xl border border-hairline bg-panel">
            <header className="flex items-center justify-between border-b border-hairline px-4 py-3">
              <h2 className="text-[14px] font-medium text-paper">Карточка проекта</h2>
              <Button
                variant="ghost"
                className="h-8 px-2"
                disabled={!selectedId}
                onClick={focusSelected}
                data-cursor-label="Фокус"
              >
                <Focus size={14} />
              </Button>
            </header>
            <AnimatePresence mode="wait">
              {selected ? (
                <motion.div
                  key={selected.id}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -4 }}
                  transition={{ duration: 0.28, ease: easeOutExpo }}
                  className="space-y-3 px-4 py-4"
                >
                  <div>
                    <p className="font-mono text-[11px] text-accent">{selected.id}</p>
                    <p className="font-display mt-1 text-xl text-paper">{selected.name}</p>
                    <p className="mt-1 text-xs text-mute">
                      {selected.department} · {selected.owner}
                    </p>
                  </div>
                  <div className="grid grid-cols-2 gap-2 border-t border-hairline pt-3">
                    <Metric label="Бюджет" value={formatMoney(selected.budget)} />
                    <Metric label="NPV" value={formatMoney(selected.npv)} />
                    <Metric label="SAW" value={formatScore(selected.score)} />
                    <Metric
                      label="Статус"
                      value={selected.selected ? "IN" : selected.evaluated ? "OUT" : "—"}
                      accent={selected.selected}
                    />
                  </div>
                  <div className="border-t border-hairline pt-3">
                    <p className="meta mb-2">Критерии (mock)</p>
                    <div className="space-y-1.5">
                      {(
                        [
                          ["Стратегия", selected.criteria.strategy],
                          ["Риск*", selected.criteria.risk],
                          ["Готовность", selected.criteria.readiness],
                        ] as const
                      ).map(([label, v], i) => (
                        <div key={label} className="flex items-center gap-2">
                          <span className="w-24 text-[11px] text-mute">{label}</span>
                          <div className="h-1 flex-1 overflow-hidden bg-paper/10">
                            <motion.div
                              className="h-full bg-signal"
                              initial={{ width: 0 }}
                              animate={{ width: `${v * 100}%` }}
                              transition={{
                                delay: 0.08 + i * 0.05,
                                duration: 0.45,
                                ease: easeOutExpo,
                              }}
                            />
                          </div>
                          <span className="num w-8 text-right text-[11px] text-paper">
                            {v.toFixed(2)}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                  <div className="border-t border-hairline pt-3">
                    <p className="meta mb-2">Зависимости</p>
                    {selected.dependsOn.length ? (
                      <div className="flex flex-wrap gap-1.5">
                        {selected.dependsOn.map((d) => (
                          <motion.button
                            key={d}
                            type="button"
                            whileHover={{ y: -1 }}
                            whileTap={{ scale: 0.96 }}
                            onClick={() => setSelectedId(d)}
                            className="rounded-md border border-hairline px-2 py-1 font-mono text-[10px] text-signal hover:border-signal/40"
                          >
                            {d}
                          </motion.button>
                        ))}
                      </div>
                    ) : (
                      <p className="text-xs text-mute">Нет входящих зависимостей</p>
                    )}
                  </div>
                  {selected.rejectReason && (
                    <motion.p
                      initial={{ opacity: 0, y: 4 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="rounded-md border border-reject/25 bg-reject/10 px-3 py-2 text-[13px] text-reject"
                    >
                      {selected.rejectReason}
                    </motion.p>
                  )}
                  {selected.selected && (snap.stage === "final" || snap.stage === "done") && (
                    <motion.p
                      initial={{ opacity: 0, y: 4 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="rounded-md border border-accent/25 bg-accent-soft px-3 py-2 text-[13px] text-accent"
                    >
                      Включён: высокая плотность SAW/бюджет при соблюдении лимита и зависимостей.
                    </motion.p>
                  )}
                </motion.div>
              ) : (
                <motion.p
                  key="empty"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="px-4 py-8 text-sm text-mute"
                >
                  Наведите или выберите объект в сцене.
                </motion.p>
              )}
            </AnimatePresence>
          </section>

          <section className="min-h-0 flex-1 overflow-hidden rounded-xl border border-hairline bg-panel">
            <header className="border-b border-hairline px-4 py-3">
              <h2 className="text-[14px] font-medium text-paper">Журнал событий</h2>
              <p className="mt-1 font-mono text-[10px] text-mute">
                Оценено: {snap.evaluatedCount} · {paused ? "пауза" : snap.status.toLowerCase()}
              </p>
            </header>
            <div className="max-h-[220px] overflow-auto xl:max-h-[280px]">
              {snap.events.length === 0 ? (
                <p className="px-4 py-6 text-sm text-mute">Нажмите «Оптимизировать».</p>
              ) : (
                <AnimatePresence initial={false}>
                  {snap.events.map((e) => (
                    <motion.div
                      key={e.t + e.text}
                      variants={listItem}
                      initial="hidden"
                      animate="show"
                      className="border-b border-hairline px-4 py-2.5 last:border-b-0"
                    >
                      <p className="font-mono text-[10px] text-mute">
                        {new Date(e.t).toLocaleTimeString("ru-RU")}
                      </p>
                      <p className="mt-0.5 text-xs text-paper">{e.text}</p>
                    </motion.div>
                  ))}
                </AnimatePresence>
              )}
            </div>
          </section>
        </motion.aside>
      </motion.div>
    </motion.div>
  );
}

function Metric({
  label,
  value,
  accent,
}: {
  label: string;
  value: string;
  accent?: boolean;
}) {
  return (
    <motion.div
      layout
      className="rounded-md border border-hairline bg-panel-2/40 px-2.5 py-2"
      initial={{ opacity: 0, scale: 0.97 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.28, ease: easeOutExpo }}
    >
      <p className="meta">{label}</p>
      <p className={cn("num mt-1 text-sm", accent ? "text-accent" : "text-paper")}>{value}</p>
    </motion.div>
  );
}
