import { lazy, Suspense } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/layout/AppShell";
import { CustomCursor } from "./components/cursor/CustomCursor";
import { ThemeProvider } from "./theme/ThemeProvider";

const LoginPage = lazy(() =>
  import("./pages/LoginPage").then((m) => ({ default: m.LoginPage })),
);
const ProjectsPage = lazy(() =>
  import("./pages/ProjectsPage").then((m) => ({ default: m.ProjectsPage })),
);
const ScenarioPage = lazy(() =>
  import("./pages/ScenarioPage").then((m) => ({ default: m.ScenarioPage })),
);
const ProfilePage = lazy(() =>
  import("./pages/ProfilePage").then((m) => ({ default: m.ProfilePage })),
);

function Boot() {
  return (
    <div className="flex min-h-svh items-center justify-center bg-black">
      <p className="text-[14px] font-medium text-mute">Loading…</p>
    </div>
  );
}

export default function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <CustomCursor />
        <Suspense fallback={<Boot />}>
          <Routes>
            <Route path="/" element={<LoginPage />} />
            <Route element={<AppShell />}>
              <Route path="/projects" element={<ProjectsPage />} />
              <Route path="/scenario" element={<ScenarioPage />} />
              <Route path="/profile" element={<ProfilePage />} />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </ThemeProvider>
  );
}
