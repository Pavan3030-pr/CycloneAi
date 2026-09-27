import { ArrowLeft, CheckCircle2, Radar, ShieldCheck, Timer } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Logo } from '@/components/Logo';
import { cn } from '@/lib/cn';

/**
 * Shared frame for the sign-in and sign-up screens.
 *
 * A two-column split: the brand column carries the promise and the reassurance, the form column does
 * one job. On small screens the brand column collapses to a compact header so the form is never more
 * than a thumb's reach away.
 */

const PROMISES = [
  'Asset-level risk for grids, roads and shelters',
  'Advisories generated from the assessment itself',
  'Deterministic, UTC-pinned and fully traceable',
];

export function AuthPanel({
  title,
  subtitle,
  children,
  aside,
  footer,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
  aside?: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <div className="grid min-h-screen bg-white lg:grid-cols-[1.05fr_1fr]">
      <aside className="relative hidden overflow-hidden bg-brand-sheen p-12 lg:flex lg:flex-col lg:justify-between">
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.16]"
          style={{
            backgroundImage:
              'radial-gradient(circle at 30% 20%, #2fcbbb 0%, transparent 45%), radial-gradient(circle at 75% 70%, #779ffb 0%, transparent 50%)',
          }}
        />
        <div className="relative">
          <Link to="/" className="rounded-lg">
            <Logo tone="light" />
          </Link>
        </div>

        <div className="relative max-w-md">
          <span className="inline-flex items-center gap-2 rounded-full bg-white/12 px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-accent-200">
            <Radar className="size-3.5" />
            Coastal impact screening
          </span>
          <h2 className="mt-6 font-display text-3xl font-extrabold leading-tight tracking-tightest text-white">
            Protect what matters, before landfall.
          </h2>
          <ul className="mt-8 space-y-3.5">
            {PROMISES.map((promise) => (
              <li key={promise} className="flex items-start gap-3 text-sm text-brand-100">
                <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-accent-300" />
                {promise}
              </li>
            ))}
          </ul>
        </div>

        <div className="relative grid grid-cols-2 gap-4">
          <Stat icon={Timer} value="< 1 s" label="Per assessment" />
          <Stat icon={ShieldCheck} value="RFC 9457" label="Typed error contract" />
        </div>

        {/* Decorative spiral, echoing the logo mark at architectural scale. */}
        <svg
          aria-hidden="true"
          viewBox="0 0 200 200"
          className="pointer-events-none absolute -bottom-20 -right-16 size-72 text-white/10"
        >
          <path
            d="M100 18c34 0 62 24 62 58 0 28-20 50-47 50-22 0-39-15-39-35 0-17 13-29 28-29 12 0 21 8 21 18"
            fill="none"
            stroke="currentColor"
            strokeWidth="10"
            strokeLinecap="round"
          />
        </svg>
      </aside>

      <div className="flex flex-col">
        <header className="flex items-center justify-between px-5 py-6 sm:px-8">
          <Link to="/" className="rounded-lg lg:invisible">
            <Logo />
          </Link>
          <Link
            to="/"
            className="inline-flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-sm font-medium text-ink-500 transition hover:bg-ink-100 hover:text-ink-800"
          >
            <ArrowLeft className="size-4" />
            Back to home
          </Link>
        </header>

        <div className="flex flex-1 items-center justify-center px-5 pb-10 sm:px-8">
          <div className={cn('w-full max-w-md', 'animate-fade-in')}>
            <h1 className="font-display text-3xl font-extrabold tracking-tightest text-ink-900">{title}</h1>
            <p className="mt-3 text-sm leading-relaxed text-ink-600">{subtitle}</p>

            <div className="mt-8">{children}</div>

            {aside !== undefined ? <div className="mt-8">{aside}</div> : null}
          </div>
        </div>

        {footer !== undefined ? (
          <footer className="px-5 pb-8 text-center text-xs text-ink-500 sm:px-8">{footer}</footer>
        ) : null}
      </div>
    </div>
  );
}

function Stat({ icon: Icon, value, label }: { icon: typeof Timer; value: string; label: string }) {
  return (
    <div className="rounded-2xl border border-white/15 bg-white/8 p-4 backdrop-blur-sm">
      <Icon className="size-4 text-accent-300" />
      <p className="mt-2.5 font-display text-lg font-bold text-white">{value}</p>
      <p className="text-[11px] text-brand-100">{label}</p>
    </div>
  );
}
