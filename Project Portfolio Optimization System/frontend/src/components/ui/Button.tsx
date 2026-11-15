import type { ButtonHTMLAttributes, ReactNode } from "react";
import { motion, type HTMLMotionProps } from "motion/react";
import { cn } from "../../lib/cn";
import { springSnappy } from "../../lib/motion";

type Variant = "primary" | "ghost" | "line" | "danger";

const styles: Record<Variant, string> = {
  primary: "bg-cta text-cta-fg hover:bg-cta-hover shadow-none",
  line: "bg-transparent text-paper border-[length:var(--theme-border-width)] border-hairline-strong hover:bg-cta hover:text-cta-fg hover:border-cta",
  ghost: "bg-transparent text-mute hover:text-paper hover:bg-panel-2",
  danger:
    "bg-reject/10 text-reject border-[length:var(--theme-border-width)] border-reject/25 hover:bg-reject/15",
};

type Props = Omit<HTMLMotionProps<"button">, "children"> & {
  variant?: Variant;
  children: ReactNode;
  type?: ButtonHTMLAttributes<HTMLButtonElement>["type"];
};

export function Button({
  variant = "primary",
  className,
  children,
  type = "button",
  disabled,
  ...props
}: Props) {
  return (
    <motion.button
      type={type}
      disabled={disabled}
      whileHover={disabled ? undefined : { y: -1 }}
      whileTap={disabled ? undefined : { scale: 0.97 }}
      transition={springSnappy}
      className={cn(
        "inline-flex h-11 items-center justify-center gap-2 rounded-md px-5 text-[15px] font-medium tracking-[-0.01em] transition-[background-color,color,border-color] duration-150 disabled:pointer-events-none disabled:opacity-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[color:var(--theme-ring)]",
        styles[variant],
        className,
      )}
      {...props}
    >
      {children}
    </motion.button>
  );
}
