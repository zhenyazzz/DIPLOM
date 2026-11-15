import { Monitor, Moon, Sun } from "lucide-react";
import { motion } from "motion/react";
import { useTheme, type ThemePreference } from "../../theme/ThemeProvider";
import { cn } from "../../lib/cn";
import { springSoft } from "../../lib/motion";

const OPTIONS: { value: ThemePreference; label: string; icon: typeof Sun }[] = [
  { value: "light", label: "Светлая", icon: Sun },
  { value: "dark", label: "Тёмная", icon: Moon },
  { value: "system", label: "Система", icon: Monitor },
];

export function ThemeToggle({ className }: { className?: string }) {
  const { preference, setPreference, cyclePreference } = useTheme();

  return (
    <div
      className={cn(
        "inline-flex items-center rounded-md border-[length:var(--theme-border-width)] border-hairline bg-panel-2/60 p-0.5",
        className,
      )}
      role="group"
      aria-label="Тема оформления"
    >
      {OPTIONS.map(({ value, label, icon: Icon }) => {
        const active = preference === value;
        return (
          <motion.button
            key={value}
            type="button"
            onClick={() => setPreference(value)}
            onDoubleClick={cyclePreference}
            aria-label={label}
            aria-pressed={active}
            title={label}
            data-cursor-label={label}
            whileTap={{ scale: 0.92 }}
            className={cn(
              "relative inline-flex h-8 w-8 items-center justify-center rounded-[6px]",
              active ? "text-paper" : "text-mute hover:text-paper",
            )}
          >
            {active && (
              <motion.span
                layoutId="theme-toggle-pill"
                className="absolute inset-0 rounded-[6px] bg-panel shadow-card"
                transition={springSoft}
              />
            )}
            <Icon size={14} strokeWidth={1.75} className="relative z-10" />
          </motion.button>
        );
      })}
    </div>
  );
}
