import { useState, type FormEvent } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useApiHealth } from '@/api/hooks';
import { useAuth } from '@/auth/AuthProvider';
import { useRunDemo } from '@/demo/useRunDemo';
import { useTheme } from '@/hooks/useTheme';
import { cn } from '@/lib/cn';
import { Button, Card, ErrorNote, Field, Input, Spinner, StatusDot } from './ui';

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', description: 'Storm status and risk totals' },
  { to: '/track', label: 'Track visualizer', description: 'Track, centre and assets on the map' },
  { to: '/assessment', label: 'Impact assessment', description: 'Run an assessment' },
  { to: '/assets', label: 'Asset management', description: 'Critical infrastructure registry' },
  { to: '/advisory', label: 'Advisory panel', description: 'Generated early-warning text' },
];

/**
 * Frame around every screen: navigation, session state, backend reachability and the demo control.
 *
 * The API health badge is a real call to the unauthenticated health probe, so "connected" means the
 * backend answered, not that a request succeeded once. When the session is missing the shell renders
 * the sign-in screen instead of the console, which keeps every route protected in one place rather
 * than per page.
 */
export function AppShell() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <SignInScreen />;
  }
  return <AuthenticatedShell />;
}

function AuthenticatedShell() {
  const { principal, logout } = useAuth();
  const { theme, toggle } = useTheme();
  const health = useApiHealth();
  const demo = useRunDemo();
  const navigate = useNavigate();
  const [demoError, setDemoError] = useState<unknown>(null);

  const runDemo = async () => {
    setDemoError(null);
    try {
      await demo.run();
      navigate('/');
    } catch (cause) {
      setDemoError(cause);
    }
  };

  const backendUp = health.data?.status === 'UP';

  return (
    <div className="min-h-screen lg:flex">
      <aside className="border-b border-slate-200 bg-white/70 backdrop-blur lg:w-64 lg:shrink-0 lg:border-b-0 lg:border-r dark:border-slate-800 dark:bg-slate-900/60">
        <div className="flex items-center justify-between gap-3 px-5 py-4 lg:block">
          <div>
            <p className="text-sm font-semibold tracking-tight text-slate-900 dark:text-slate-50">
              Cyclone Impact Forecaster
            </p>
            <p className="mt-0.5 text-[11px] text-slate-500 dark:text-slate-400">
              Infrastructure vulnerability console
            </p>
          </div>
          <Button variant="secondary" className="lg:hidden" onClick={toggle} aria-label="Toggle theme">
            {theme === 'dark' ? 'Light' : 'Dark'}
          </Button>
        </div>

        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 lg:flex-col lg:overflow-visible lg:pb-0">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) => cn('nav-link whitespace-nowrap', isActive && 'nav-link-active')}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="space-y-3 px-5 py-4 lg:mt-4 lg:border-t lg:border-slate-200 dark:lg:border-slate-800">
          <Button onClick={runDemo} busy={demo.isRunning} className="w-full">
            {demo.isRunning ? 'Running demo' : 'Demo mode'}
          </Button>
          {demoError !== null ? <ErrorNote error={demoError} /> : null}
          <p className="text-[11px] leading-relaxed text-slate-500 dark:text-slate-400">
            Loads a Bay of Bengal track and seven coastal assets through the live API.
          </p>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 bg-white/70 px-5 py-3 backdrop-blur dark:border-slate-800 dark:bg-slate-900/60">
          <div className="flex items-center gap-4">
            {health.isPending ? (
              <span className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                <Spinner /> checking API
              </span>
            ) : (
              <StatusDot
                ok={backendUp}
                label={backendUp ? `API ${health.data?.status}` : 'API unreachable'}
              />
            )}
            {principal !== null ? (
              <span className="text-xs text-slate-500 dark:text-slate-400">
                Signed in as <span className="font-medium text-slate-700 dark:text-slate-200">{principal.username}</span>{' '}
                <span className="font-mono">[{principal.roles.join(', ')}]</span>
              </span>
            ) : null}
          </div>

          <div className="flex items-center gap-2">
            <Button variant="secondary" className="hidden lg:inline-flex" onClick={toggle}>
              {theme === 'dark' ? 'Light theme' : 'Dark theme'}
            </Button>
            <Button
              variant="ghost"
              onClick={() => {
                logout();
                navigate('/');
              }}
            >
              Sign out
            </Button>
          </div>
        </header>

        <main className="min-w-0 flex-1 px-5 py-5">
          <Outlet />
        </main>

        <footer className="border-t border-slate-200 px-5 py-3 text-[11px] text-slate-500 dark:border-slate-800 dark:text-slate-400">
          Screening model: linear interpolation between published fixes with a 75 nm exponential wind-field decay.
          Cross-check against official NHC/JTWC/IMD products before operational use.
        </footer>
      </div>
    </div>
  );
}

function SignInScreen() {
  const { login } = useAuth();
  const demo = useRunDemo();
  const navigate = useNavigate();
  const [username, setUsername] = useState(import.meta.env.VITE_DEMO_USERNAME ?? 'analyst');
  const [password, setPassword] = useState(import.meta.env.VITE_DEMO_PASSWORD ?? 'cyclone-demo-analyst');
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(username, password);
      navigate('/');
    } catch (cause) {
      setError(cause);
    } finally {
      setBusy(false);
    }
  };

  const runDemo = async () => {
    setError(null);
    try {
      await demo.run();
      navigate('/');
    } catch (cause) {
      setError(cause);
    }
  };

  return (
    <div className="grid min-h-screen place-items-center px-5 py-10">
      <div className="w-full max-w-md space-y-4">
        <div className="text-center">
          <h1 className="text-xl font-semibold tracking-tight text-slate-900 dark:text-slate-50">
            Cyclone Impact &amp; Infrastructure Vulnerability Forecaster
          </h1>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            Sign in with a configured account, or jump straight into the demonstration.
          </p>
        </div>

        <Card title="Sign in" subtitle="Bearer token issued by the backend">
          <form className="space-y-3" onSubmit={onSubmit}>
            <Field label="Username">
              <Input
                value={username}
                autoComplete="username"
                onChange={(event) => setUsername(event.target.value)}
                required
                maxLength={64}
              />
            </Field>
            <Field label="Password">
              <Input
                type="password"
                value={password}
                autoComplete="current-password"
                onChange={(event) => setPassword(event.target.value)}
                required
                maxLength={256}
              />
            </Field>
            <Button type="submit" busy={busy} className="w-full">
              Sign in
            </Button>
          </form>

          <div className="mt-4 border-t border-slate-200 pt-4 dark:border-slate-800">
            <Button variant="secondary" className="w-full" onClick={runDemo} busy={demo.isRunning}>
              Demo mode (sign in as demo analyst)
            </Button>
            <p className="mt-2 text-[11px] leading-relaxed text-slate-500 dark:text-slate-400">
              Signs in with the configured demo account, registers the sample assets, then runs a real assessment.
            </p>
          </div>

          {error !== null ? <div className="mt-3"><ErrorNote error={error} /></div> : null}
        </Card>

        <p className="text-center text-[11px] text-slate-500 dark:text-slate-400">
          Demo credentials come from the backend's app.security.users and the frontend's VITE_DEMO_* variables.
        </p>
      </div>
    </div>
  );
}
