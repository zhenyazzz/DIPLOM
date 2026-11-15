import type { Transition, Variants } from "motion/react";

/** Shared UI motion language — not used by the Three.js scene. */
export const easeOutExpo: [number, number, number, number] = [0.22, 1, 0.36, 1];

export const springSoft: Transition = {
  type: "spring",
  stiffness: 420,
  damping: 34,
  mass: 0.85,
};

export const springSnappy: Transition = {
  type: "spring",
  stiffness: 520,
  damping: 36,
  mass: 0.7,
};

export const tweenSoft: Transition = {
  duration: 0.55,
  ease: easeOutExpo,
};

export const fadeUp: Variants = {
  hidden: { opacity: 0, y: 14 },
  show: {
    opacity: 1,
    y: 0,
    transition: tweenSoft,
  },
};

export const fadeIn: Variants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { duration: 0.4, ease: easeOutExpo },
  },
};

export const scaleIn: Variants = {
  hidden: { opacity: 0, scale: 0.96, y: 10 },
  show: {
    opacity: 1,
    scale: 1,
    y: 0,
    transition: springSoft,
  },
};

export const staggerContainer: Variants = {
  hidden: {},
  show: {
    transition: {
      staggerChildren: 0.055,
      delayChildren: 0.04,
    },
  },
};

export const staggerFast: Variants = {
  hidden: {},
  show: {
    transition: {
      staggerChildren: 0.035,
      delayChildren: 0.02,
    },
  },
};

export const listItem: Variants = {
  hidden: { opacity: 0, y: 10 },
  show: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.34, ease: easeOutExpo },
  },
};

export const pageTransition = {
  initial: { opacity: 0, y: 10 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -6 },
  transition: { duration: 0.32, ease: easeOutExpo },
};

export const slideInRight: Variants = {
  hidden: { opacity: 0, x: 16 },
  show: {
    opacity: 1,
    x: 0,
    transition: tweenSoft,
  },
};

export const metricPop: Variants = {
  hidden: { opacity: 0, y: 8, scale: 0.98 },
  show: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: springSoft,
  },
};
