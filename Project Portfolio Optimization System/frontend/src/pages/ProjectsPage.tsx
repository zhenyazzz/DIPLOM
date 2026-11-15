import { useMemo, useState } from "react";
import { Plus, Search, Upload } from "lucide-react";
import { AnimatePresence, LayoutGroup, motion } from "motion/react";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";
import { StatusBadge } from "../components/ui/Badge";
import { PortfolioScene } from "../components/viz/PortfolioScene";
import {
  formatMoney,
  formatScore,
  portfolioProjects,
  statusLabel,
  type SimNode,
} from "../data/portfolio";
import { cn } from "../lib/cn";
import {
  easeOutExpo,
  fadeUp,
  listItem,
  springSoft,
  staggerContainer,
  staggerFast,
} from "../lib/motion";

export function ProjectsPage() {
  const [query, setQuery] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return portfolioProjects;
    return portfolioProjects.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        p.id.toLowerCase().includes(q) ||
        p.department.toLowerCase().includes(q),
    );
  }, [query]);

  const nodes = useMemo<SimNode[]>(
    () =>
      portfolioProjects.map((p) => ({
        ...p,
        selected: p.score >= 0.72,
        evaluated: true,
      })),
    [],
  );

  const budgetSum = portfolioProjects.reduce((s, p) => s + p.budget, 0);

  return (
    <div className="grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">
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
            <p className="meta mb-3">Реестр</p>
            <h1 className="font-display text-4xl text-paper sm:text-5xl">Проекты</h1>
            <p className="mt-2 text-[15px] text-mute">
              {portfolioProjects.length} кандидатов в пространстве
            </p>
          </div>
          <div className="flex gap-2">
            <Button variant="line">
              <Upload size={14} />
              Импорт
            </Button>
            <Button>
              <Plus size={14} />
              Новый
            </Button>
          </div>
        </motion.div>

        <motion.div
          variants={staggerFast}
          initial="hidden"
          animate="show"
          className="mb-4 grid grid-cols-3 overflow-hidden rounded-lg border border-hairline bg-panel"
        >
          {[
            { label: "Реестр", value: String(portfolioProjects.length) },
            { label: "Σ бюджет", value: formatMoney(budgetSum) },
            {
              label: "Ср. SAW",
              value: formatScore(
                portfolioProjects.reduce((s, p) => s + p.score, 0) / portfolioProjects.length,
              ),
            },
          ].map((item) => (
            <motion.div
              key={item.label}
              variants={fadeUp}
              className="border-r border-hairline px-4 py-3.5 last:border-r-0"
            >
              <p className="meta">{item.label}</p>
              <motion.p
                className="num mt-2 text-xl text-paper"
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.45, ease: easeOutExpo }}
              >
                {item.value}
              </motion.p>
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
            placeholder="Код, название, подразделение"
          />
        </motion.div>

        <motion.div
          variants={fadeUp}
          className="overflow-hidden rounded-lg border border-hairline bg-panel"
        >
          <LayoutGroup>
            <div className="max-h-[54vh] overflow-auto">
              <AnimatePresence mode="popLayout" initial={false}>
                {rows.map((project) => {
                  const active = selectedId === project.id;
                  return (
                    <motion.button
                      key={project.id}
                      type="button"
                      layout
                      variants={listItem}
                      initial="hidden"
                      animate="show"
                      exit={{ opacity: 0, height: 0, margin: 0, paddingTop: 0, paddingBottom: 0 }}
                      transition={springSoft}
                      whileHover={{ backgroundColor: "color-mix(in srgb, var(--color-panel-2) 70%, transparent)" }}
                      onClick={() => setSelectedId(active ? null : project.id)}
                      className={cn(
                        "relative flex w-full items-center gap-4 border-b border-hairline px-4 py-3.5 text-left last:border-b-0",
                        active ? "bg-accent-soft" : "",
                      )}
                    >
                      {active && (
                        <motion.span
                          layoutId="project-active-bar"
                          className="absolute top-2 bottom-2 left-0 w-0.5 rounded-full bg-accent"
                          transition={springSoft}
                        />
                      )}
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-mono text-[11px] text-accent">{project.id}</span>
                          <StatusBadge status={project.status} label={statusLabel[project.status]} />
                        </div>
                        <p className="mt-1 truncate text-[15px] font-medium text-paper">
                          {project.name}
                        </p>
                        <p className="mt-0.5 text-[12px] text-mute">
                          {project.department} · {project.owner}
                        </p>
                      </div>
                      <div className="hidden shrink-0 text-right sm:block">
                        <p className="num text-[13px] text-paper">{formatMoney(project.budget)}</p>
                        <p className="num mt-1 text-[11px] text-mute">
                          SAW {formatScore(project.score)} · NPV {formatMoney(project.npv)}
                        </p>
                      </div>
                    </motion.button>
                  );
                })}
              </AnimatePresence>
            </div>
          </LayoutGroup>
        </motion.div>
      </motion.div>

      <motion.aside
        initial={{ opacity: 0, x: 18 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ delay: 0.12, duration: 0.55, ease: easeOutExpo }}
        className="relative min-h-[440px] overflow-hidden rounded-xl border border-hairline bg-ink xl:min-h-[680px]"
      >
        <motion.div
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.35, duration: 0.4 }}
          className="absolute top-3 left-3 z-10 rounded-md bg-black/70 px-2.5 py-1 backdrop-blur-sm"
        >
          <p className="meta !normal-case !tracking-normal">Пространственный реестр</p>
        </motion.div>
        <PortfolioScene
          nodes={nodes}
          stage="normalize"
          selectedId={selectedId}
          onSelect={setSelectedId}
          className="h-full min-h-[440px] xl:min-h-[680px]"
        />
      </motion.aside>
    </div>
  );
}
