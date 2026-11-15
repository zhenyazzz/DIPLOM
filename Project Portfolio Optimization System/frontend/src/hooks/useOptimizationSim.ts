import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import gsap from "gsap";
import {
  BUDGET_CAP,
  STAGE_META,
  portfolioProjects,
  runLocalOptimization,
  type SimNode,
  type SimSnapshot,
  type SimStage,
} from "../data/portfolio";

const STAGES: Exclude<SimStage, "idle" | "done">[] = [
  "normalize",
  "knapsack",
  "dependencies",
  "final",
];

const STAGE_DURATION = 2.2;
const SNAP_MIN_MS = 80;

function blankNodes(): SimNode[] {
  return portfolioProjects.map((p) => ({
    ...p,
    selected: false,
    evaluated: false,
    rejectReason: undefined,
  }));
}

function snapshotFrom(
  stage: SimStage,
  nodes: SimNode[],
  progress: number,
  status: SimSnapshot["status"],
  events: SimSnapshot["events"],
): SimSnapshot {
  const showTotals =
    stage === "knapsack" || stage === "dependencies" || stage === "final" || stage === "done";
  const picked = showTotals ? nodes.filter((n) => n.selected) : [];
  const meta =
    stage === "idle" || stage === "done"
      ? {
          label: stage === "done" ? "Завершено" : "Ожидание",
          detail: stage === "done" ? "Портфель OPTIMAL" : "Готов к запуску",
        }
      : STAGE_META[stage];

  return {
    stage,
    stageLabel: meta.label,
    stageDetail: meta.detail,
    progress,
    nodes,
    budgetCap: BUDGET_CAP,
    budgetUsed: picked.reduce((s, n) => s + n.budget, 0),
    scoreSum: picked.reduce((s, n) => s + n.score, 0),
    selectedCount: picked.length,
    evaluatedCount: nodes.filter((n) => n.evaluated).length,
    status,
    events,
  };
}

export function useOptimizationSim() {
  const result = useMemo(() => runLocalOptimization(portfolioProjects, BUDGET_CAP), []);
  const [snap, setSnap] = useState<SimSnapshot>(() =>
    snapshotFrom("idle", blankNodes(), 0, "STANDBY", []),
  );
  const [paused, setPaused] = useState(false);
  const tlRef = useRef<gsap.core.Timeline | null>(null);
  const eventsRef = useRef<{ t: number; text: string }[]>([]);
  const nodesRef = useRef<SimNode[]>(blankNodes());
  const lastSnapAt = useRef(0);

  const publish = useCallback(
    (
      stage: SimStage,
      progress: number,
      status: SimSnapshot["status"],
      force = false,
    ) => {
      const now = performance.now();
      if (!force && now - lastSnapAt.current < SNAP_MIN_MS) return;
      lastSnapAt.current = now;
      setSnap(snapshotFrom(stage, nodesRef.current, progress, status, eventsRef.current));
    },
    [],
  );

  const kill = useCallback(() => {
    tlRef.current?.kill();
    tlRef.current = null;
  }, []);

  useEffect(() => () => kill(), [kill]);

  const pushEvent = (text: string) => {
    eventsRef.current = [{ t: Date.now(), text }, ...eventsRef.current].slice(0, 14);
  };

  const start = useCallback(() => {
    kill();
    setPaused(false);
    eventsRef.current = [];
    nodesRef.current = blankNodes();
    pushEvent("Запуск локальной симуляции оптимизации");
    publish("normalize", 0.01, "RUNNING", true);

    const tl = gsap.timeline({
      onComplete: () => {
        nodesRef.current = result.nodes;
        pushEvent(
          `OPTIMAL · ${result.selectedCount} проектов · бюджет ${result.budgetUsed.toLocaleString("ru-RU")}`,
        );
        publish("done", 1, "OPTIMAL", true);
      },
    });

    STAGES.forEach((stage, i) => {
      const local = { t: 0 };

      tl.call(() => {
        if (stage === "normalize") {
          pushEvent("Нормализация SAW · вход объектов в пространство");
          nodesRef.current = blankNodes();
        }
        if (stage === "knapsack") {
          pushEvent("Knapsack 0-1 · оценка плотности SAW/бюджет");
          nodesRef.current = result.nodes.map((n) => ({
            ...n,
            selected: false,
            evaluated: false,
            rejectReason: undefined,
          }));
        }
        if (stage === "dependencies") {
          pushEvent("Проверка зависимостей и ремонт комбинаций");
          nodesRef.current = result.nodes;
        }
        if (stage === "final") {
          pushEvent("Сходимость финального портфеля");
          nodesRef.current = result.nodes;
        }
        publish(stage, i / STAGES.length, "RUNNING", true);
      });

      tl.to(local, {
        t: 1,
        duration: STAGE_DURATION,
        ease: "none",
        onUpdate: () => {
          const overall = (i + local.t) / STAGES.length;

          if (stage === "normalize") {
            const count = Math.max(1, Math.floor(local.t * portfolioProjects.length));
            nodesRef.current = blankNodes().map((n, idx) => ({
              ...n,
              evaluated: idx < count,
            }));
          }

          if (stage === "knapsack") {
            const ranked = [...result.nodes].sort(
              (a, b) => b.score / b.budget - a.score / a.budget,
            );
            const count = Math.max(1, Math.floor(local.t * ranked.length));
            const seen = new Set(ranked.slice(0, count).map((n) => n.id));
            nodesRef.current = result.nodes.map((n) => ({
              ...n,
              evaluated: seen.has(n.id),
              selected: n.selected && seen.has(n.id),
              rejectReason: !n.selected && seen.has(n.id) ? n.rejectReason : undefined,
            }));
          }

          if (stage === "dependencies" || stage === "final") {
            nodesRef.current = result.nodes;
          }

          publish(stage, overall, "RUNNING");
        },
      });
    });

    tlRef.current = tl;
  }, [kill, publish, result]);

  const pause = useCallback(() => {
    tlRef.current?.pause();
    setPaused(true);
    setSnap((s) => ({ ...s, status: "PAUSED" }));
  }, []);

  const resume = useCallback(() => {
    tlRef.current?.resume();
    setPaused(false);
    setSnap((s) => ({ ...s, status: "RUNNING" }));
  }, []);

  const reset = useCallback(() => {
    kill();
    setPaused(false);
    eventsRef.current = [];
    nodesRef.current = blankNodes();
    setSnap(snapshotFrom("idle", nodesRef.current, 0, "STANDBY", []));
  }, [kill]);

  return { snap, paused, start, pause, resume, reset, result };
}
