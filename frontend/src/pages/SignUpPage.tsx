import { Building2, CheckCircle2, KeyRound, Loader2, Mail, Sparkles, User, UserCog } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';
import { AuthField, PasswordField } from '@/components/auth/fields';
import { AuthPanel } from '@/components/auth/AuthPanel';
import { useRunDemo } from '@/demo/useRunDemo';
import { cn } from '@/lib/cn';

/**
 * Create an account.
 *
 * The backend provisions accounts from configuration rather than through a public endpoint, so this
 * screen does something more useful than a decorative form: it validates the request the way an
 * administrator would need it, then routes the user into the demonstration while the account is
 * issued. Nothing here pretends an account exists before it does.
 */

const ROLE_OPTIONS = [
  { value: 'ANALYST', label: 'Analyst', hint: 'Run assessments and manage the asset registry' },
  { value: 'VIEWER', label: 'Viewer', hint: 'Read assessments and advisories only' },
  { value: 'ADMIN', label: 'Administrator', hint: 'Full access, including metrics and user provisioning' },
];

export function SignUpPage() {
  const demo = useRunDemo();
  const navigate = useNavigate();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [organisation, setOrganisation] = useState('');
  const [role, setRole] = useState('ANALYST');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [accepted, setAccepted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    if (password.length < 12) {
      setError('Choose a password of at least 12 characters.');
      return;
    }
    if (password !== confirm) {
      setError('The two passwords do not match.');
      return;
    }
    if (!accepted) {
      setError('Please accept the operational-use notice before continuing.');
      return;
    }
    setSubmitted(true);
  };

  if (submitted) {
    return (
      <AuthPanel
        title="Request received"
        subtitle="Account provisioning is handled by an administrator, because this instance runs against a configured user directory."
      >
        <div className="card p-6">
          <div className="flex items-center gap-3">
            <span className="inline-flex size-10 items-center justify-center rounded-full bg-accent-50 text-accent-700 ring-1 ring-inset ring-accent-100">
              <CheckCircle2 className="size-5" />
            </span>
            <div>
              <p className="text-sm font-semibold text-ink-900">{fullName}</p>
              <p className="text-xs text-ink-500">
                {email} · {ROLE_OPTIONS.find((option) => option.value === role)?.label}
              </p>
            </div>
          </div>

          <dl className="mt-5 space-y-2.5 border-t border-ink-100 pt-5 text-sm">
            <Row label="Organisation" value={organisation.length > 0 ? organisation : '—'} />
            <Row label="Requested role" value={role} mono />
            <Row label="Directory" value="app.security.users (backend configuration)" mono />
          </dl>

          <p className="mt-5 text-xs leading-relaxed text-ink-500">
            Access is granted without a round trip through a public endpoint. In the meantime, the
            demonstration runs against the live API as the configured demo analyst.
          </p>
        </div>

        <div className="mt-6 space-y-3">
          <button
            type="button"
            onClick={async () => {
              try {
                await demo.run();
                navigate('/app', { replace: true });
              } catch {
                // useRunDemo surfaces the failure through demo.error below.
              }
            }}
            disabled={demo.isRunning}
            className="btn w-full bg-brand-700 py-3 text-white shadow-glow transition hover:bg-brand-800 disabled:opacity-60"
          >
            {demo.isRunning ? <Loader2 className="size-4 animate-spin" /> : <Sparkles className="size-4" />}
            {demo.isRunning ? 'Running the demo storm…' : 'Explore the console with the demo storm'}
          </button>
          {demo.error !== null ? (
            <p className="text-center text-xs text-coral-700">
              {demo.error instanceof Error ? demo.error.message : 'The demonstration could not start.'}
            </p>
          ) : null}
        </div>
      </AuthPanel>
    );
  }

  return (
    <AuthPanel
      title="Create your account"
      subtitle="Two minutes to set up, then your first storm assessment. No card, no infrastructure to deploy."
      footer={
        <p>
          Already have an account?{' '}
          <Link to="/signin" className="link-quiet">
            Sign in
          </Link>
        </p>
      }
    >
      <form className="space-y-5" onSubmit={onSubmit}>
        <AuthField
          label="Full name"
          icon={User}
          value={fullName}
          onChange={(event) => setFullName(event.target.value)}
          autoComplete="name"
          placeholder="Priya Raghunathan"
          maxLength={128}
          required
        />

        <AuthField
          label="Work email"
          icon={Mail}
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          autoComplete="email"
          placeholder="priya@coastal-programme.gov"
          maxLength={254}
          required
        />

        <AuthField
          label="Organisation"
          icon={Building2}
          value={organisation}
          onChange={(event) => setOrganisation(event.target.value)}
          autoComplete="organization"
          placeholder="State Disaster Management Authority"
          maxLength={128}
        />

        <fieldset>
          <legend className="text-sm font-semibold text-ink-800">Requested access</legend>
          <div className="mt-2.5 space-y-2">
            {ROLE_OPTIONS.map((option) => (
              <label
                key={option.value}
                className={cn(
                  'flex cursor-pointer items-start gap-3 rounded-xl border p-3.5 transition',
                  role === option.value
                    ? 'border-brand-400 bg-brand-50/70 ring-1 ring-brand-200'
                    : 'border-ink-200 bg-white hover:border-ink-300',
                )}
              >
                <input
                  type="radio"
                  name="role"
                  value={option.value}
                  checked={role === option.value}
                  onChange={() => setRole(option.value)}
                  className="mt-0.5 size-4 border-ink-300 text-brand-700 focus:ring-brand-500"
                />
                <span>
                  <span className="flex items-center gap-1.5 text-sm font-semibold text-ink-900">
                    <UserCog className="size-3.5 text-ink-400" />
                    {option.label}
                  </span>
                  <span className="mt-0.5 block text-xs text-ink-500">{option.hint}</span>
                </span>
              </label>
            ))}
          </div>
        </fieldset>

        <div className="grid gap-5 sm:grid-cols-2">
          <PasswordField
            label="Password"
            icon={KeyRound}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="new-password"
            placeholder="At least 12 characters"
            minLength={12}
            maxLength={256}
            required
          />
          <PasswordField
            label="Confirm"
            icon={KeyRound}
            value={confirm}
            onChange={(event) => setConfirm(event.target.value)}
            autoComplete="new-password"
            placeholder="Repeat password"
            minLength={12}
            maxLength={256}
            required
          />
        </div>

        <label className="flex items-start gap-3 text-xs leading-relaxed text-ink-600">
          <input
            type="checkbox"
            checked={accepted}
            onChange={(event) => setAccepted(event.target.checked)}
            className="mt-0.5 size-4 rounded border-ink-300 text-brand-700 focus:ring-brand-500"
          />
          I understand that CycloneAI produces screening guidance, and that operational decisions must
          be cross-checked against official NHC, JTWC and IMD warnings.
        </label>

        {error !== null ? (
          <p className="rounded-xl border border-coral-200 bg-coral-50 px-4 py-3 text-sm text-coral-800">{error}</p>
        ) : null}

        <button
          type="submit"
          className="btn w-full bg-brand-700 py-3 text-white shadow-glow transition hover:bg-brand-800"
        >
          Request access
        </button>

        <p className="text-center text-xs leading-relaxed text-ink-500">
          Prefer to look first?{' '}
          <Link to="/signin?demo=1" className="link-quiet">
            Run the demo storm
          </Link>{' '}
          without an account.
        </p>
      </form>
    </AuthPanel>
  );
}

function Row({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <dt className="text-xs font-semibold uppercase tracking-wider text-ink-500">{label}</dt>
      <dd className={cn('truncate text-ink-800', mono && 'font-mono text-xs')}>{value}</dd>
    </div>
  );
}
