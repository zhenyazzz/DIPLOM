import type { ReactNode } from "react";
import { motion, type HTMLMotionProps } from "motion/react";
import { cn } from "../../lib/cn";
import { easeOutExpo } from "../../lib/motion";

type Props = HTMLMotionProps<"div"> & {
  children: ReactNode;
  /** Stagger delay in seconds */
  delay?: number;
  /** Drop distance when appearing */
  from?: number;
};

/**
 * Down scroll → drops into view. Up scroll (out of view) → hides again.
 */
export function RevealOnScroll({
  children,
  className,
  delay = 0,
  from = 56,
  ...props
}: Props) {
  return (
    <motion.div
      className={cn(className)}
      initial="hidden"
      whileInView="show"
      viewport={{ once: false, amount: 0.28, margin: "0px 0px -8% 0px" }}
      variants={{
        hidden: {
          opacity: 0,
          y: -from,
          filter: "blur(6px)",
          transition: { duration: 0.35, ease: easeOutExpo },
        },
        show: {
          opacity: 1,
          y: 0,
          filter: "blur(0px)",
          transition: {
            delay,
            type: "spring",
            stiffness: 280,
            damping: 26,
            mass: 0.85,
          },
        },
      }}
      {...props}
    >
      {children}
    </motion.div>
  );
}
