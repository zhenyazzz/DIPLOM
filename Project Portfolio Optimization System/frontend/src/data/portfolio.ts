export type ProjectStatus = "DRAFT" | "SUBMITTED" | "SCORED" | "SELECTED" | "REJECTED";

export type SimStage =
  | "idle"
  | "normalize"
  | "knapsack"
  | "dependencies"
  | "final"
  | "done";

export type PortfolioProject = {
  id: string;
  name: string;
  department: string;
  owner: string;
  budget: number;
  npv: number;
  score: number;
  dependsOn: string[];
  status: ProjectStatus;
  /** criteria mock for SAW narrative */
  criteria: { strategy: number; risk: number; readiness: number };
};

export type SimNode = PortfolioProject & {
  selected: boolean;
  rejectReason?: string;
  evaluated: boolean;
};

export type SimSnapshot = {
  stage: SimStage;
  stageLabel: string;
  stageDetail: string;
  progress: number;
  nodes: SimNode[];
  budgetCap: number;
  budgetUsed: number;
  scoreSum: number;
  selectedCount: number;
  evaluatedCount: number;
  status: "STANDBY" | "RUNNING" | "PAUSED" | "OPTIMAL";
  events: { t: number; text: string }[];
};

export const BUDGET_CAP = 28_000_000;

export const STAGE_META: Record<
  Exclude<SimStage, "idle" | "done">,
  { label: string; detail: string }
> = {
  normalize: {
    label: "Нормализация",
    detail: "SAW-оценки фиксируются, объекты входят в пространство ценности",
  },
  knapsack: {
    label: "Knapsack 0-1",
    detail: "Перебор кандидатов при ограничении бюджета",
  },
  dependencies: {
    label: "Зависимости",
    detail: "Проверка связей и устранение невалидных комбинаций",
  },
  final: {
    label: "Финальный портфель",
    detail: "Сходимость отобранных проектов в оптимальную композицию",
  },
};

const DEPTS = [
  "Производство",
  "Логистика",
  "ИТ",
  "R&D",
  "Энергетика",
  "Качество",
  "HR",
  "Закупки",
] as const;

const OWNERS = [
  "Иванова А.",
  "Петров К.",
  "Сидорова М.",
  "Орлов Д.",
  "Ким Е.",
  "Белова Н.",
  "Гришин В.",
  "Новак Л.",
];

const NAMES = [
  "Модернизация линии упаковки",
  "Цифровой склад WMS",
  "ERP-модуль закупок",
  "SCADA линия B",
  "Лаборатория качества",
  "Энергоаудит корпусов 3–5",
  "R&D композитных панелей",
  "Обучение операторов ЧПУ",
  "Робот-паллетайзер",
  "Предиктивное ТО насосов",
  "Модернизация компрессоров",
  "Система прослеживаемости",
  "Рекуперация тепла цеха 2",
  "Цифровой двойник линии A",
  "Автоматизация ОТК",
  "Модернизация АСУТП",
  "Складской AGV-парк",
  "Платформа MES",
  "Замена КИПиА",
  "Солнечная станция крыши",
  "Модуль ТОиР",
  "Линия фасовки фасовки soft",
  "Контроль вибрации турбин",
  "Интеграция EDI поставщиков",
  "Цех аддитивного производства",
  "Система энергомониторинга",
  "Обновление серверного кластера",
  "Программа lean-трансформации",
];

function money(n: number) {
  return n.toLocaleString("ru-RU");
}

function seeded(i: number) {
  const x = Math.sin(i * 12.9898 + 78.233) * 43758.5453;
  return x - Math.floor(x);
}

function buildProjects(): PortfolioProject[] {
  return NAMES.map((name, i) => {
    const r = seeded(i + 1);
    const r2 = seeded(i + 40);
    const r3 = seeded(i + 80);
    const budget = Math.round((0.6 + r * 5.8) * 1_000_000 / 10_000) * 10_000;
    const score = Math.round((0.42 + r2 * 0.5) * 1000) / 1000;
    const npv = Math.round(budget * (0.08 + r3 * 0.28) / 1000) * 1000;
    const dept = DEPTS[i % DEPTS.length];
    const dependsOn: string[] = [];
    if (i > 0 && r > 0.52) {
      dependsOn.push(`P-${1042 + Math.floor(r2 * i)}`);
    }
    if (i > 4 && r3 > 0.78) {
      dependsOn.push(`P-${1042 + Math.floor(r * (i - 1))}`);
    }
    const uniq = [...new Set(dependsOn)].filter(
      (d) => d !== `P-${1042 + i}` && Number(d.slice(2)) >= 1042 && Number(d.slice(2)) < 1042 + i,
    );
    return {
      id: `P-${1042 + i}`,
      name,
      department: dept,
      owner: OWNERS[i % OWNERS.length],
      budget,
      npv,
      score,
      dependsOn: uniq.slice(0, 2),
      status: "SCORED" as const,
      criteria: {
        strategy: Math.round((0.3 + r * 0.7) * 100) / 100,
        risk: Math.round((0.2 + r2 * 0.7) * 100) / 100,
        readiness: Math.round((0.25 + r3 * 0.7) * 100) / 100,
      },
    };
  });
}

export const portfolioProjects: PortfolioProject[] = buildProjects();

export function formatMoney(n: number) {
  return money(n);
}

export function formatScore(n: number) {
  return n.toFixed(3);
}

/** Deterministic greedy 0-1 knapsack by score/budget density + dependency repair */
export function runLocalOptimization(projects: PortfolioProject[], cap = BUDGET_CAP) {
  const byId = new Map(projects.map((p) => [p.id, p]));
  const ranked = [...projects].sort((a, b) => b.score / b.budget - a.score / a.budget);
  const chosen = new Set<string>();
  let used = 0;

  for (const p of ranked) {
    if (used + p.budget > cap) continue;
    const depsOk = p.dependsOn.every((d) => chosen.has(d) || !byId.has(d));
    if (!depsOk) continue;
    chosen.add(p.id);
    used += p.budget;
  }

  // Force-include dependency parents if child selected but parent missing (repair pass)
  let changed = true;
  while (changed) {
    changed = false;
    for (const id of [...chosen]) {
      const p = byId.get(id)!;
      for (const dep of p.dependsOn) {
        if (!chosen.has(dep) && byId.has(dep)) {
          const parent = byId.get(dep)!;
          if (used + parent.budget <= cap) {
            chosen.add(dep);
            used += parent.budget;
            changed = true;
          } else {
            chosen.delete(id);
            used -= p.budget;
            changed = true;
          }
        }
      }
    }
  }

  const nodes: SimNode[] = projects.map((p) => {
    const selected = chosen.has(p.id);
    let rejectReason: string | undefined;
    if (!selected) {
      if (p.dependsOn.some((d) => !chosen.has(d) && byId.has(d))) {
        rejectReason = "Нарушение зависимости";
      } else if (p.budget > cap * 0.45) {
        rejectReason = "Превышение доли бюджета";
      } else {
        rejectReason = "Низкий вклад при лимите бюджета";
      }
    }
    return { ...p, selected, rejectReason, evaluated: true };
  });

  const selected = nodes.filter((n) => n.selected);
  return {
    nodes,
    budgetUsed: selected.reduce((s, n) => s + n.budget, 0),
    scoreSum: selected.reduce((s, n) => s + n.score, 0),
    selectedCount: selected.length,
  };
}

export type Vec3 = [number, number, number];

export function nodeSize(budget: number, maxBudget: number) {
  return 0.22 + (budget / maxBudget) * 0.55;
}

export function spatialBase(p: PortfolioProject, maxNpv: number): Vec3 {
  return [(p.score - 0.5) * 11, 0, ((p.npv / maxNpv) - 0.5) * 9];
}

export function positionForStage(
  node: SimNode,
  stage: SimStage,
  index: number,
  total: number,
  selectedIndex: number,
  selectedCount: number,
  maxNpv: number,
): Vec3 {
  const [bx, , bz] = spatialBase(node, maxNpv);
  const angle = (index / Math.max(total, 1)) * Math.PI * 2;

  switch (stage) {
    case "idle":
      return [(index - (total - 1) / 2) * 0.85, 0, 5.5];
    case "normalize":
      return [bx * 0.55, 0, bz * 0.55];
    case "knapsack": {
      // Split into candidate lanes while evaluating
      const lane = node.selected ? -2.2 : 2.8;
      return [bx * 0.7 + lane, 0, bz * 0.7 + (index % 5) * 0.15];
    }
    case "dependencies":
      if (node.selected) return [bx * 0.85, 0, bz * 0.85];
      return [bx * 1.4 + Math.cos(angle) * 1.5, 0, bz * 1.4 + Math.sin(angle) * 1.5];
    case "final":
    case "done":
      if (node.selected) {
        const a = (selectedIndex / Math.max(selectedCount, 1)) * Math.PI * 2 - Math.PI / 2;
        const radius = 2.1 + selectedCount * 0.05;
        return [Math.cos(a) * radius, 0, Math.sin(a) * radius];
      }
      return [bx * 2.8 + Math.cos(angle) * 3.2, 0, bz * 2.8 + Math.sin(angle) * 3.2];
    default:
      return [bx, 0, bz];
  }
}

// Compatibility aliases for older pages
export type Project = PortfolioProject;
export const projects = portfolioProjects;
export const statusLabel: Record<ProjectStatus, string> = {
  DRAFT: "Черновик",
  SUBMITTED: "На рассмотрении",
  SCORED: "Оценён",
  SELECTED: "В портфеле",
  REJECTED: "Отклонён",
};
