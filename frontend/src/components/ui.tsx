import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react';
import { ApiError } from '@/api/client';
import { cn } from '@/lib/cn';

/**
 * The small component set the console is built from.
 *
 * Written by hand rather than pulled from a component library: the whole system is six screens, and
 * owning the primitives means the risk palette, spacing and focus states are defined once in
 * `index.css` and reused, with no dependency to keep in step with React.
 */

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  busy?: boolean;
}

const BUTTON_VARIANTS: Record<ButtonVariant, string> = {
  primary: 'bg-sky-600 text-white hover:bg-sky-500 focus-visible:outline-sky-500',
  secondary:
    'border border-slate-300 bg-white text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800',
  ghost: 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800',
  danger: 'bg-red-600 text-white hover:bg-red-500 focus-visible:outline-red-500',
};

export function Button({ variant = 'primary', busy = false, className, children, ...rest }: ButtonProps) {
  return (
    <button
      {...rest}
      disabled={rest.disabled || busy}
      className={cn('btn focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2', BUTTON_VARIANTS[variant], className)}
    >
      {busy ? <Spinner /> : null}
      {children}
    </button>
  );
}

export function Card({
  title,
  subtitle,
  actions,
  children,
  className,
}: {
  title?: string;
  subtitle?: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={cn('card animate-fade-in', className)}>
      {title !== undefined ? (
        <header className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 px-5 py-3.5 dark:border-slate-800">
          <div>
            <h2 className="text-sm font-semibold text-slate-800 dark:text-slate-100">{title}</h2>
            {subtitle !== undefined ? (
              <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{subtitle}</p>
            ) : null}
          </div>
          {actions !== undefined ? <div className="flex items-center gap-2">{actions}</div> : null}
        </header>
      ) : null}
      <div className="px-5 py-4">{children}</div>
    </section>
  );
}

export function Badge({ className, children }: { className?: string; children: ReactNode }) {
  return <span className={cn('badge', className)}>{children}</span>;
}

export function Spinner({ className }: { className?: string }) {
  return (
    <span
      role="status"
      aria-label="Loading"
      className={cn(
        'inline-block size-3.5 animate-spin rounded-full border-2 border-current border-t-transparent',
        className,
      )}
    />
  );
}

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="label">{label}</span>
      <span className="mt-1.5 block">{children}</span>
      {hint !== undefined ? <span className="mt-1 block text-xs text-slate-500 dark:text-slate-400">{hint}</span> : null}
    </label>
  );
}

export function Input(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={cn('input', props.className)} />;
}

export function Select(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={cn('input', props.className)} />;
}

export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 px-6 py-10 text-center dark:border-slate-700">
      <p className="text-sm font-semibold text-slate-700 dark:text-slate-200">{title}</p>
      <p className="mx-auto mt-1 max-w-md text-xs text-slate-500 dark:text-slate-400">{description}</p>
      {action !== undefined ? <div className="mt-4 flex justify-center">{action}</div> : null}
    </div>
  );
}

export function StatusDot({ ok, label }: { ok: boolean; label: string }) {
  return (
    <span className="inline-flex items-center gap-2 text-xs text-slate-600 dark:text-slate-300">
      <span className={cn('size-2 rounded-full', ok ? 'bg-emerald-500' : 'bg-red-500')} />
      {label}
    </span>
  );
}

/**
 * Renders any thrown value as a readable error, including the server's field-level messages and the
 * correlation id, which is the one string a user can quote back to an operator.
 */
export function ErrorNote({ error }: { error: unknown }) {
  if (error === null || error === undefined) {
    return null;
  }

  if (error instanceof ApiError) {
    return (
      <div className="rounded-lg border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-200">
        <p className="font-medium">{error.message}</p>
        {error.fieldErrors.length > 0 ? (
          <ul className="mt-1.5 list-inside list-disc text-xs">
            {error.fieldErrors.map((fieldError) => (
              <li key={fieldError}>{fieldError}</li>
            ))}
          </ul>
        ) : null}
        {error.correlationId !== undefined ? (
          <p className="mt-1.5 font-mono text-[11px] opacity-80">correlation id {error.correlationId}</p>
        ) : null}
      </div>
    );
  }

  return (
    <div className="rounded-lg border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-200">
      {error instanceof Error ? error.message : 'Something went wrong'}
    </div>
  );
}
