/**
 * Compatibility layer — canonical data lives in portfolio.ts
 */
export {
  portfolioProjects as projects,
  statusLabel,
  formatMoney,
  formatScore,
  BUDGET_CAP as SCENARIO_BUDGET,
  type ProjectStatus,
  type PortfolioProject as Project,
  type SimStage as OptPhase,
  type SimNode,
} from "./portfolio";

import {
  portfolioProjects,
  type PortfolioProject,
  type SimNode,
  nodeSize,
  spatialBase,
} from "./portfolio";

export type ConstellationNode = {
  id: string;
  name: string;
  budget: number;
  score: number;
  npv: number;
  selected: boolean;
  rejectReason?: string;
  dependsOn: string[];
  department: string;
  base: [number, number, number];
  size: number;
  owner?: string;
  criteria?: PortfolioProject["criteria"];
};

const maxBudget = Math.max(...portfolioProjects.map((p) => p.budget));
const maxNpv = Math.max(...portfolioProjects.map((p) => p.npv));

export function projectToNode(p: PortfolioProject): ConstellationNode {
  return {
    id: p.id,
    name: p.name,
    budget: p.budget,
    score: p.score,
    npv: p.npv,
    selected: p.status === "SELECTED",
    dependsOn: p.dependsOn,
    department: p.department,
    base: spatialBase(p, maxNpv),
    size: nodeSize(p.budget, maxBudget),
    owner: p.owner,
    criteria: p.criteria,
  };
}

export function simNodeToConstellation(n: SimNode): ConstellationNode {
  return {
    id: n.id,
    name: n.name,
    budget: n.budget,
    score: n.score,
    npv: n.npv,
    selected: n.selected,
    rejectReason: n.rejectReason,
    dependsOn: n.dependsOn,
    department: n.department,
    base: spatialBase(n, maxNpv),
    size: nodeSize(n.budget, maxBudget),
    owner: n.owner,
    criteria: n.criteria,
  };
}

export const OPT_PHASES = [
  { id: "normalize", label: "Нормализация", detail: "SAW и вход в пространство" },
  { id: "knapsack", label: "Knapsack 0-1", detail: "Ограничение бюджета" },
  { id: "dependencies", label: "Зависимости", detail: "Валидация связей" },
  { id: "final", label: "Финал", detail: "Сходимость портфеля" },
] as const;
