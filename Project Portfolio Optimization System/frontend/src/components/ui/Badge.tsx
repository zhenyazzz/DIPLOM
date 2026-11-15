import { cn } from "../../lib/cn";
import type { ProjectStatus } from "../../data/mock";

const tone: Record<ProjectStatus, string> = {
  DRAFT: "bg-panel-2 text-mute",
  SUBMITTED: "bg-amber/15 text-amber",
  SCORED: "bg-signal/15 text-signal",
  SELECTED: "bg-accent-soft text-accent",
  REJECTED: "bg-reject/10 text-reject",
};

export function StatusBadge({ status, label }: { status: ProjectStatus; label: string }) {
  return (
    <span
      className={cn(
        "inline-flex h-6 items-center rounded-full px-2.5 text-[11px] font-medium tracking-[0.02em] border-[length:var(--theme-border-width)] border-transparent",
        tone[status],
      )}
    >
      {label}
    </span>
  );
}
