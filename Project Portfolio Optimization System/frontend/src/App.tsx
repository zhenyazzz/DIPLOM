import { lazy, Suspense } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/layout/AppShell";
import { CustomCursor } from "./components/cursor/CustomCursor";
import { ThemeProvider } from "./theme/ThemeProvider";

const LandingPage = lazy(() =>
  import("./pages/LandingPage").then((m) => ({ default: m.LandingPage })),
);
const LoginPage = lazy(() =>
  import("./pages/LoginPage").then((m) => ({ default: m.LoginPage })),
);
const RegisterPage = lazy(() =>
  import("./pages/RegisterPage").then((m) => ({ default: m.RegisterPage })),
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
const UsersPage = lazy(() =>
  import("./pages/UsersPage").then((m) => ({ default: m.UsersPage })),
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
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route element={<AppShell />}>
              <Route path="/projects" element={<ProjectsPage />} />
              <Route path="/scenario" element={<ScenarioPage />} />
              <Route path="/users" element={<UsersPage />} />
              <Route path="/profile" element={<ProfilePage />} />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </ThemeProvider>
  );
}
