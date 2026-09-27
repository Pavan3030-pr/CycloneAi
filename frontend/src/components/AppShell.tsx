import { AnimatePresence, motion } from 'framer-motion';
import {
  BellRing,
  ChevronDown,
  LayoutDashboard,
  Loader2,
  LogOut,
  Map,
  Menu,
  ServerCog,
  ShieldAlert,
  Sparkles,
  X,
} from 'lucide-react';
import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useApiHealth } from '@/api/hooks';
import { Logo } from '@/components/Logo';
import { ErrorNote } from '@/components/ui';
import { useAuth } from '@/auth/AuthProvider';
import { useRunDemo } from '@/demo/useRunDemo';
import { cn } from '@/lib/cn';

/**
 * The authenticated console frame.
 *
 * Top navigation rather than a sidebar: this product is read on a laptop during a briefing and on a
 * phone on a beach, and a horizontal bar survives both. The shell owns the session gate, the API
 * reachability badge and the demo control, so every route inside it is protected by construction.
 */

const NAV_ITEMS = [
  { to: '/app', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/app/track', label: 'Track', icon: Map, end: false },
  { to: '/app/assessment', label: 'Assessment', icon: ShieldAlert, end: false },
  { to: '/app/assets', label: 'Assets', icon: ServerCog, end: false },
  { to: '/app/advisory', label: 'Advisory', icon: BellRing, end: false },
];

export function AppShell() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <SignInRequired />;
  }
  return <Console />;
}

/** Shown when a route is opened without a session, with the target kept for after sign-in. */
function SignInRequired() {
  const location = useLocation();

  return (
    <div className="grid min-h-screen place-items-center bg-ink-50 px-5">
      <div className="card w-full max-w-md p-8 text-center">
        <Logo className="justify-center" />
        <h1 className="mt-6 font-display text-xl font-bold tracking-tight text-ink-900">
          Your session has ended
        </h1>
        <p className="mt-2 text-sm text-ink-600">
          Sign in again to return to the console, or run the demonstration storm without an account.
        </p>
        <div className="mt-6 flex flex-col gap-2.5">
          <Link to="/signin" state={{ from: location.pathname }} className="btn bg-brand-700 py-3 text-white shadow-glow hover:bg-brand-800">
            Sign in
          </Link>
          <Link to="/signin?demo=1" className="btn border border-ink-200 bg-white text-ink-800 hover:bg-ink-50">
            Run the demo storm
          </Link>
        </div>
      </div>
    </div>
  );
}

function Console() {
  const { principal, logout } = useAuth();
  const health = useApiHealth();
  const demo = useRunDemo();
  const navigate = useNavigate();
  const location = useLocation();
  const [demoError, setDemoError] = useState<unknown>(null);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const backendUp = health.data?.status === 'UP';

  const runDemo = async () => {
    setDemoError(null);
    try {
      await demo.run();
      navigate('/app');
    } catch (cause) {
      setDemoError(cause);
    }
  };

  // Close the mobile sheet on navigation, so tapping a link does not leave a panel covering the page.
  useEffect(() => {
    setMobileNavOpen(false);
  }, [location.pathname]);

  return (
    <div className="flex min-h-screen flex-col bg-ink-50">
      <header className="sticky top-0 z-40 border-b border-ink-200/80 bg-white/85 backdrop-blur-xl">
        <div className="mx-auto flex h-16 w-full max-w-[100rem] items-center gap-4 px-4 sm:px-6">
          <Link to="/app" className="rounded-lg" aria-label="CycloneAI console home">
            <Logo size="size-8" />
          </Link>

          <nav className="ml-2 hidden items-center gap-1 lg:flex" aria-label="Console sections">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) => cn('nav-link', isActive && 'nav-link-active')}
              >
                <item.icon className="size-4" />
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-2">
            <StatusPill up={backendUp} pending={health.isPending} />

            <button
              type="button"
              onClick={runDemo}
              disabled={demo.isRunning}
              className="btn hidden bg-brand-700 px-3.5 py-2 text-white shadow-glow transition hover:bg-brand-800 disabled:opacity-70 sm:inline-flex"
            >
              {demo.isRunning ? <Loader2 className="size-4 animate-spin" /> : <Sparkles className="size-4" />}
              <span className="hidden md:inline">{demo.isRunning ? 'Running demo…' : 'Run demo'}</span>
            </button>

            <UserMenu
              username={principal?.username ?? 'unknown'}
              roles={principal?.roles ?? []}
              onSignOut={() => {
                logout();
                navigate('/');
              }}
            />

            <button
              type="button"
              onClick={() => setMobileNavOpen((open) => !open)}
              aria-expanded={mobileNavOpen}
              aria-label={mobileNavOpen ? 'Close navigation' : 'Open navigation'}
              className="btn border border-ink-200 bg-white px-3 py-2 text-ink-700 lg:hidden"
            >
              {mobileNavOpen ? <X className="size-4" /> : <Menu className="size-4" />}
            </button>
          </div>
        </div>

        <AnimatePresence>
          {mobileNavOpen ? (
            <motion.nav
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.24, ease: [0.22, 1, 0.36, 1] }}
              className="overflow-hidden border-t border-ink-200/80 bg-white lg:hidden"
              aria-label="Console sections"
            >
              <div className="flex flex-col gap-1 px-4 py-3">
                {NAV_ITEMS.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) => cn('nav-link', isActive && 'nav-link-active')}
                  >
                    <item.icon className="size-4" />
                    {item.label}
                  </NavLink>
                ))}
                <button
                  type="button"
                  onClick={runDemo}
                  disabled={demo.isRunning}
                  className="btn mt-1 bg-brand-700 py-2.5 text-white sm:hidden"
                >
                  {demo.isRunning ? <Loader2 className="size-4 animate-spin" /> : <Sparkles className="size-4" />}
                  Run demo storm
                </button>
              </div>
            </motion.nav>
          ) : null}
        </AnimatePresence>
      </header>

      {demoError !== null ? (
        <div className="mx-auto w-full max-w-[100rem] px-4 pt-4 sm:px-6">
          <ErrorNote error={demoError} />
        </div>
      ) : null}

      <AnimatePresence mode="wait">
        <motion.main
          key={location.pathname}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -4 }}
          transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
          className="mx-auto w-full max-w-[100rem] flex-1 px-4 py-6 sm:px-6 sm:py-8"
        >
          <Outlet />
        </motion.main>
      </AnimatePresence>

      <footer className="border-t border-ink-200/80 bg-white px-4 py-4 text-[11px] leading-relaxed text-ink-500 sm:px-6">
        <div className="mx-auto flex max-w-[100rem] flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <p>
            Screening model: linear interpolation between published fixes with 75 nm exponential wind-field decay.
          </p>
          <p>Cross-check against official NHC / JTWC / IMD products before operational use.</p>
        </div>
      </footer>
    </div>
  );
}

function StatusPill({ up, pending }: { up: boolean; pending: boolean }) {
  const label = pending ? 'Checking API' : up ? 'API live' : 'API unreachable';
  const colour = pending ? 'bg-ink-400' : up ? 'bg-accent-500' : 'bg-coral-500';

  return (
    <span
      title={label}
      className="hidden items-center gap-2 rounded-full border border-ink-200 bg-white px-3 py-1.5 text-[11px] font-semibold text-ink-600 sm:inline-flex"
    >
      <span className={cn('size-2 rounded-full', colour, pending && 'animate-pulse')} />
      {label}
    </span>
  );
}

function UserMenu({
  username,
  roles,
  onSignOut,
}: {
  username: string;
  roles: string[];
  onSignOut: () => void;
}) {
  const [open, setOpen] = useState(false);
  const container = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    const onPointerDown = (event: PointerEvent) => {
      if (container.current !== null && !container.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setOpen(false);
      }
    };
    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('pointerdown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  return (
    <div ref={container} className="relative">
      <button
        type="button"
        onClick={() => setOpen((current) => !current)}
        aria-haspopup="menu"
        aria-expanded={open}
        className={cn(
          'flex items-center gap-2 rounded-xl border px-2 py-1.5 transition',
          open ? 'border-brand-300 bg-brand-50' : 'border-ink-200 bg-white hover:border-ink-300',
        )}
      >
        <span className="grid size-7 place-items-center rounded-lg bg-brand-700 text-xs font-bold uppercase text-white">
          {username.slice(0, 1)}
        </span>
        <span className="hidden max-w-[7rem] truncate text-sm font-semibold text-ink-800 sm:block">
          {username}
        </span>
        <ChevronDown className={cn('size-4 text-ink-400 transition', open && 'rotate-180')} />
      </button>

      <AnimatePresence>
        {open ? (
          <motion.div
            role="menu"
            initial={{ opacity: 0, y: -6, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.98 }}
            transition={{ duration: 0.16, ease: [0.22, 1, 0.36, 1] }}
            className="absolute right-0 mt-2 w-60 origin-top-right overflow-hidden rounded-2xl border border-ink-200 bg-white shadow-lift"
          >
            <div className="border-b border-ink-100 px-4 py-3">
              <p className="truncate text-sm font-semibold text-ink-900">{username}</p>
              <div className="mt-1.5 flex flex-wrap gap-1">
                {roles.map((role) => (
                  <span key={role} className="badge bg-brand-50 text-brand-700 ring-brand-100">
                    {role}
                  </span>
                ))}
              </div>
            </div>

            <MenuLink to="/app/assets" icon={<ServerCog className="size-4" />} label="Asset registry" onSelect={() => setOpen(false)} />
            <MenuLink to="/app/advisory" icon={<BellRing className="size-4" />} label="Latest advisory" onSelect={() => setOpen(false)} />

            <button
              type="button"
              role="menuitem"
              onClick={onSignOut}
              className="flex w-full items-center gap-2.5 border-t border-ink-100 px-4 py-3 text-sm font-medium text-coral-700 transition hover:bg-coral-50"
            >
              <LogOut className="size-4" />
              Sign out
            </button>
          </motion.div>
        ) : null}
      </AnimatePresence>
    </div>
  );
}

function MenuLink({
  to,
  icon,
  label,
  onSelect,
}: {
  to: string;
  icon: ReactNode;
  label: string;
  onSelect: () => void;
}) {
  return (
    <Link
      to={to}
      role="menuitem"
      onClick={onSelect}
      className="flex items-center gap-2.5 px-4 py-2.5 text-sm font-medium text-ink-700 transition hover:bg-ink-50"
    >
      {icon}
      {label}
    </Link>
  );
}
