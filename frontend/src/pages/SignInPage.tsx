import { KeyRound, Loader2, Sparkles, User } from 'lucide-react';
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { AuthField, PasswordField } from '@/components/auth/fields';
import { AuthPanel } from '@/components/auth/AuthPanel';
import { ErrorNote } from '@/components/ui';
import { useAuth } from '@/auth/AuthProvider';
import { useRunDemo } from '@/demo/useRunDemo';

/**
 * Sign in.
 *
 * Three ways in, in the order a real user wants them: the demonstration (no credentials needed
 * because the demo account is configured on the server), a password, or a new account. Arriving with
 * `?demo=1` — which is what the landing page's primary call to action links to — runs the
 * demonstration immediately, since asking someone to click twice after they already said "start"
 * is how a good demo loses its audience.
 */

const DEMO_USERNAME = import.meta.env.VITE_DEMO_USERNAME ?? 'analyst';
const DEMO_PASSWORD = import.meta.env.VITE_DEMO_PASSWORD ?? 'cyclone-demo-analyst';

export function SignInPage() {
  const { login } = useAuth();
  const demo = useRunDemo();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const startedDemo = useRef(false);

  const wantsDemo = searchParams.get('demo') === '1';

  const afterAuth = () => {
    const requested = (location.state as { from?: string } | null)?.from;
    navigate(requested ?? '/app', { replace: true });
  };

  const runDemo = async () => {
    setError(null);
    try {
      await demo.run();
      afterAuth();
    } catch (cause) {
      setError(cause);
    }
  };

  useEffect(() => {
    if (wantsDemo && !startedDemo.current) {
      startedDemo.current = true;
      void runDemo();
    }
    // Runs once per mount: re-running on every render would restart the assessment mid-flight.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [wantsDemo]);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(username, password);
      afterAuth();
    } catch (cause) {
      setError(cause);
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthPanel
      title="Welcome back"
      subtitle="Sign in to the impact console, or run the demonstration storm without an account."
      aside={
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <span className="h-px flex-1 bg-ink-200" />
            <span className="text-[11px] font-semibold uppercase tracking-[0.14em] text-ink-400">or</span>
            <span className="h-px flex-1 bg-ink-200" />
          </div>

          <button
            type="button"
            onClick={runDemo}
            disabled={demo.isRunning}
            className="btn w-full border border-brand-200 bg-brand-50 py-3 text-brand-800 transition hover:border-brand-300 hover:bg-brand-100 disabled:opacity-70"
          >
            {demo.isRunning ? <Loader2 className="size-4 animate-spin" /> : <Sparkles className="size-4" />}
            {demo.isRunning ? 'Running the demo storm…' : 'Run the Bay of Bengal demo'}
          </button>

          <p className="text-center text-xs leading-relaxed text-ink-500">
            Signs in as the configured demo analyst, registers seven coastal assets and runs a real
            assessment through the API.
          </p>
        </div>
      }
      footer={
        <p>
          Not sure which account to use?{' '}
          <Link to="/signup" className="link-quiet">
            Create one
          </Link>{' '}
          · Demo credentials:{' '}
          <span className="font-mono text-ink-600">
            {DEMO_USERNAME} / {DEMO_PASSWORD}
          </span>
        </p>
      }
    >
      <form className="space-y-5" onSubmit={onSubmit}>
        <AuthField
          label="Username"
          icon={User}
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          autoComplete="username"
          placeholder="analyst"
          maxLength={64}
          required
        />

        <PasswordField
          label="Password"
          icon={KeyRound}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="current-password"
          placeholder="••••••••••••"
          maxLength={256}
          required
        />

        <button
          type="submit"
          disabled={busy || demo.isRunning}
          className="btn w-full bg-brand-700 py-3 text-white shadow-glow transition hover:bg-brand-800 disabled:opacity-60"
        >
          {busy ? <Loader2 className="size-4 animate-spin" /> : null}
          Sign in
        </button>
      </form>

      <ErrorNote error={error ?? demo.error} />
    </AuthPanel>
  );
}
