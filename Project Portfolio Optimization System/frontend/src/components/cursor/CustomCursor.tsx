import { useEffect, useRef, useState } from "react";
import { useTheme } from "../../theme/ThemeProvider";

type CursorMode = "default" | "hover" | "press" | "text" | "hidden";

const FINE_POINTER = "(hover: hover) and (pointer: fine) and (min-width: 768px)";
const INTERACTIVE =
  'a[href], button:not(:disabled), [role="button"], summary, label[for], [data-cursor="hover"], .cursor-hover';
const TEXT_TARGETS =
  'input, textarea, select, [contenteditable]:not([contenteditable="false"]), [role="textbox"], [data-cursor="text"]';
const NATIVE_TARGETS = '[data-cursor="native"], .cursor-native';

const ARROW_PATH =
  "M5.08814 5.36283L10.4791 20.7254C10.781 21.5857 11.9824 21.6263 12.3417 20.7884L15.0192 14.5442C15.1203 14.3083 15.3083 14.1203 15.5442 14.0192L21.7884 11.3417C22.6263 10.9824 22.5857 9.78096 21.7254 9.47907L6.36285 4.08812C5.57154 3.81044 4.81046 4.57152 5.08814 5.36283Z";

function ringForTheme(theme: "light" | "dark"): Record<CursorMode, { size: number; border: string; bg: string }> {
  /* Anthropic ink: gray-950 / gray-050 — warm, not cool slate */
  const ink = theme === "light" ? "20 20 19" : "250 249 245";
  return {
    default: { size: 10, border: `rgb(${ink} / 0.35)`, bg: "transparent" },
    hover: { size: 46, border: `rgb(${ink} / 0.55)`, bg: `rgb(${ink} / 0.08)` },
    press: { size: 26, border: `rgb(${ink} / 0.85)`, bg: `rgb(${ink} / 0.16)` },
    text: { size: 0, border: "transparent", bg: "transparent" },
    hidden: { size: 0, border: "transparent", bg: "transparent" },
  };
}

function resolveMode(target: Element | null, pressed: boolean): CursorMode {
  if (!target) return "default";
  if (target.closest(NATIVE_TARGETS)) return "hidden";
  if (target.closest(TEXT_TARGETS)) return "text";
  if (target.closest(INTERACTIVE)) return pressed ? "press" : "hover";
  return pressed ? "press" : "default";
}

export function CustomCursor() {
  const { theme } = useTheme();
  const [enabled, setEnabled] = useState(false);
  const tipRef = useRef<HTMLDivElement>(null);
  const ringRef = useRef<HTMLDivElement>(null);
  const labelRef = useRef<HTMLSpanElement>(null);
  const pathRef = useRef<SVGPathElement>(null);
  const pos = useRef({ x: -120, y: -120 });
  const tip = useRef({ x: -120, y: -120 });
  const ring = useRef({ x: -120, y: -120 });
  const modeRef = useRef<CursorMode>("hidden");
  const visibleRef = useRef(false);
  const pressedRef = useRef(false);
  const reduceMotion = useRef(false);
  const raf = useRef(0);

  useEffect(() => {
    const mq = window.matchMedia(FINE_POINTER);
    const motionMq = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => {
      reduceMotion.current = motionMq.matches;
      setEnabled(mq.matches);
    };
    sync();
    mq.addEventListener("change", sync);
    motionMq.addEventListener("change", sync);
    return () => {
      mq.removeEventListener("change", sync);
      motionMq.removeEventListener("change", sync);
    };
  }, []);

  useEffect(() => {
    if (!enabled) return;

    const root = document.documentElement;
    root.classList.add("has-custom-cursor");

    const tipEl = tipRef.current;
    const ringEl = ringRef.current;
    const labelEl = labelRef.current;
    const pathEl = pathRef.current;
    if (!tipEl || !ringEl || !labelEl || !pathEl) return;

    const paintRing = (mode: CursorMode) => {
      const cfg = ringForTheme(theme)[mode];
      ringEl.style.width = `${cfg.size}px`;
      ringEl.style.height = `${cfg.size}px`;
      ringEl.style.borderColor = cfg.border;
      ringEl.style.background = cfg.bg;
      const fill = theme === "light" ? "#141413" : "#faf9f5";
      const stroke = theme === "light" ? "#faf9f5" : "#141413";
      pathEl.style.fill = fill;
      pathEl.setAttribute("stroke", stroke);
    };

    const applyMode = (mode: CursorMode) => {
      modeRef.current = mode;
      root.dataset.cursorMode = mode;
      paintRing(mode);

      const showGfx = visibleRef.current && mode !== "hidden" && mode !== "text";
      tipEl.style.opacity = showGfx ? "1" : "0";
      ringEl.style.opacity = showGfx ? "1" : "0";

      const raw = document.elementFromPoint(pos.current.x, pos.current.y);
      const label =
        mode === "hover" || mode === "press"
          ? (raw instanceof Element
              ? raw.closest("[data-cursor-label]")?.getAttribute("data-cursor-label")
              : null) ?? ""
          : "";
      labelEl.textContent = label;
      labelEl.style.opacity = showGfx && label ? "1" : "0";
      labelEl.style.transform = `translate3d(${pos.current.x + 18}px, ${pos.current.y + 18}px, 0)`;
    };

    const setVisible = (v: boolean) => {
      visibleRef.current = v;
      applyMode(modeRef.current);
    };

    const readTarget = (x: number, y: number) => {
      const el = document.elementFromPoint(x, y);
      return el instanceof Element ? el : null;
    };

    const onMove = (e: PointerEvent) => {
      if (e.pointerType !== "mouse" && e.pointerType !== "pen") return;
      pos.current.x = e.clientX;
      pos.current.y = e.clientY;
      if (!visibleRef.current) setVisible(true);
      applyMode(resolveMode(readTarget(e.clientX, e.clientY), pressedRef.current));
    };

    const onOver = (e: PointerEvent) => {
      if (e.pointerType !== "mouse" && e.pointerType !== "pen") return;
      pos.current.x = e.clientX;
      pos.current.y = e.clientY;
      setVisible(true);
      applyMode(resolveMode(readTarget(e.clientX, e.clientY), pressedRef.current));
    };

    const onDown = (e: PointerEvent) => {
      if (e.pointerType !== "mouse" && e.pointerType !== "pen") return;
      pressedRef.current = true;
      applyMode(resolveMode(readTarget(e.clientX, e.clientY), true));
    };

    const onUp = (e: PointerEvent) => {
      pressedRef.current = false;
      applyMode(resolveMode(readTarget(e.clientX, e.clientY), false));
    };

    const onOut = (e: PointerEvent) => {
      if (e.relatedTarget) return;
      setVisible(false);
      applyMode("hidden");
    };

    const onBlur = () => {
      pressedRef.current = false;
      setVisible(false);
      applyMode("hidden");
    };

    const onVisibility = () => {
      if (document.visibilityState === "hidden") onBlur();
    };

    const tick = () => {
      const easeTip = reduceMotion.current ? 1 : 0.45;
      const easeRing = reduceMotion.current ? 1 : 0.14;
      tip.current.x += (pos.current.x - tip.current.x) * easeTip;
      tip.current.y += (pos.current.y - tip.current.y) * easeTip;
      ring.current.x += (pos.current.x - ring.current.x) * easeRing;
      ring.current.y += (pos.current.y - ring.current.y) * easeRing;

      tipEl.style.transform = `translate3d(${tip.current.x}px, ${tip.current.y}px, 0)`;
      ringEl.style.transform = `translate3d(${ring.current.x}px, ${ring.current.y}px, 0)`;
      if (labelEl.style.opacity !== "0") {
        labelEl.style.transform = `translate3d(${tip.current.x + 18}px, ${tip.current.y + 18}px, 0)`;
      }
      raf.current = requestAnimationFrame(tick);
    };

    tip.current = { ...pos.current };
    ring.current = { ...pos.current };
    paintRing("default");
    raf.current = requestAnimationFrame(tick);

    document.addEventListener("pointermove", onMove, { capture: true, passive: true });
    document.addEventListener("pointerover", onOver, { capture: true, passive: true });
    document.addEventListener("pointerdown", onDown, { capture: true, passive: true });
    document.addEventListener("pointerup", onUp, { capture: true, passive: true });
    document.addEventListener("pointerout", onOut, true);
    document.addEventListener("visibilitychange", onVisibility);
    window.addEventListener("blur", onBlur);

    return () => {
      cancelAnimationFrame(raf.current);
      document.removeEventListener("pointermove", onMove, true);
      document.removeEventListener("pointerover", onOver, true);
      document.removeEventListener("pointerdown", onDown, true);
      document.removeEventListener("pointerup", onUp, true);
      document.removeEventListener("pointerout", onOut, true);
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("blur", onBlur);
      root.classList.remove("has-custom-cursor");
      delete root.dataset.cursorMode;
    };
  }, [enabled, theme]);

  if (!enabled) return null;

  return (
    <div className="pointer-events-none fixed inset-0 z-[9999]" aria-hidden>
      <div
        ref={ringRef}
        className="absolute top-0 left-0 -translate-x-1/2 -translate-y-1/2 rounded-full border opacity-0 will-change-transform"
        style={{
          width: 10,
          height: 10,
          transition:
            "width 180ms cubic-bezier(0.22, 1, 0.36, 1), height 180ms cubic-bezier(0.22, 1, 0.36, 1), border-color 160ms ease, background-color 160ms ease, opacity 120ms ease",
        }}
      />
      <div
        ref={tipRef}
        className="absolute top-0 left-0 opacity-0 will-change-transform"
        style={{ marginLeft: -5, marginTop: -4 }}
      >
        <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
          <path
            ref={pathRef}
            d={ARROW_PATH}
            fill={theme === "light" ? "#141413" : "#faf9f5"}
            stroke={theme === "light" ? "#faf9f5" : "#141413"}
            strokeWidth="2.05"
            strokeLinejoin="round"
            style={{ transition: "fill 140ms ease" }}
          />
        </svg>
      </div>
      <span
        ref={labelRef}
        className="absolute top-0 left-0 border border-hairline bg-ink/95 px-2 py-1 font-mono text-[10px] tracking-[0.12em] text-paper uppercase opacity-0 will-change-transform"
        style={{ transition: "opacity 140ms ease" }}
      />
      <style>{`
        .has-custom-cursor,
        .has-custom-cursor * {
          cursor: none !important;
        }
        .has-custom-cursor[data-cursor-mode="text"],
        .has-custom-cursor[data-cursor-mode="text"] * {
          cursor: text !important;
        }
        .has-custom-cursor[data-cursor-mode="hidden"],
        .has-custom-cursor[data-cursor-mode="hidden"] * {
          cursor: auto !important;
        }
      `}</style>
    </div>
  );
}
