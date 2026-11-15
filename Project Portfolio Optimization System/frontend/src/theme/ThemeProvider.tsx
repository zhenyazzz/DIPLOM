import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  useSyncExternalStore,
  type ReactNode,
} from "react";

/** Resolved appearance applied to the document. */
export type Theme = "light" | "dark";

/** User preference, including follow-system. */
export type ThemePreference = Theme | "system";

type ThemeContextValue = {
  /** Resolved light/dark used by UI and scene chrome. */
  theme: Theme;
  /** Stored preference: light | dark | system. */
  preference: ThemePreference;
  setPreference: (preference: ThemePreference) => void;
  /** Cycles light → dark → system. */
  cyclePreference: () => void;
};

const STORAGE_KEY = "portfolio-theme";

const ThemeContext = createContext<ThemeContextValue | null>(null);

function applyTheme(theme: Theme) {
  const root = document.documentElement;
  root.dataset.theme = theme;
  root.style.colorScheme = theme;
}

function readStoredPreference(): ThemePreference | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw === "light" || raw === "dark" || raw === "system") return raw;
  } catch {
    /* ignore */
  }
  return null;
}

function subscribeSystem(onStoreChange: () => void) {
  const mq = window.matchMedia("(prefers-color-scheme: light)");
  mq.addEventListener("change", onStoreChange);
  return () => mq.removeEventListener("change", onStoreChange);
}

function getSystemTheme(): Theme {
  return window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark";
}

function getServerSystemTheme(): Theme {
  return "light";
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [preference, setPreferenceState] = useState<ThemePreference>(() => {
    if (typeof document === "undefined") return "system";
    return readStoredPreference() ?? "system";
  });

  const system = useSyncExternalStore(subscribeSystem, getSystemTheme, getServerSystemTheme);
  const theme: Theme = preference === "system" ? system : preference;

  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, preference);
    } catch {
      /* ignore */
    }
  }, [preference]);

  const setPreference = useCallback((next: ThemePreference) => setPreferenceState(next), []);
  const cyclePreference = useCallback(() => {
    setPreferenceState((p) => (p === "light" ? "dark" : p === "dark" ? "system" : "light"));
  }, []);

  const value = useMemo(
    () => ({ theme, preference, setPreference, cyclePreference }),
    [theme, preference, setPreference, cyclePreference],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme must be used within ThemeProvider");
  return ctx;
}
