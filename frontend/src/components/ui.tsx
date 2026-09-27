import type {
  ButtonHTMLAttributes,
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react';
import { AlertCircle } from 'lucide-react';
import { ApiError } from '@/api/client';
import { cn } from '@/lib/cn';

/**
 * The small component set both the marketing site and the console are built from.
 *
 * Written by hand rather than pulled from a component library: owning the primitives means the brand
 * palette, spacing and focus states are defined once in `index.css` and reused, with no dependency to
 * keep in step with React.
 */

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'accent';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  busy?: boolean;
}

const BUTTON_VARIANTS: Record<ButtonVariant, string> = {
  primary: 'bg-brand-700 text-white shadow-glow hover:bg-brand-800',
  secondary: 'border border-ink-200 bg-white text-ink-800 shadow-hair hover:border-ink-300 hover:bg-ink-50',
  ghost: 'text-ink-600 hover:bg-ink-100 hover:text-ink-900',
  danger: 'bg-coral-600 text-white hover:bg-coral-700',
  accent: 'bg-accent-600 text-white hover:bg-accent-700',
};

export function Button({ variant = 'primary', busy = false, className, children, ...rest }: ButtonProps) {
  return (
    <button {...rest} disabled={rest.disabled || busy} className={cn('btn', BUTTON_VARIANTS[variant], className)}>
      {busy ? <Spinner /> : null}
      {children}
    </button>
  );
}

/**
 * Console page header.
 *
 * Every authenticated screen opens with the same three-part block — eyebrow, title, actions — which
 * is what makes five separate pages feel like one product.
 */
export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <header className="mb-6 flex flex-col gap-4 border-b border-ink-200/80 pb-5 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        {eyebrow !== undefined ? <span className="eyebrow">{eyebrow}</span> : null}
        <h1 className="mt-1.5 font-display text-2xl font-extrabold tracking-tightest text-ink-900 sm:text-[1.75rem]">
          {title}
        </h1>
        {description !== undefined ? (
          <p className="mt-1.5 max-w-2xl text-sm leading-relaxed text-ink-600">{description}</p>
        ) : null}
      </div>
      {actions !== undefined ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </header>
  );
}

export function Card({
  title,
  subtitle,
  actions,
  children,
  className,
  bodyClassName,
  padded = true,
}: {
  title?: string;
  subtitle?: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
  bodyClassName?: string;
  padded?: boolean;
}) {
  return (
    <section className={cn('card animate-fade-in overflow-hidden', className)}>
      {title !== undefined ? (
        <header className="flex flex-wrap items-start justify-between gap-3 border-b border-ink-100 bg-white px-5 py-4">
          <div className="min-w-0">
            <h2 className="font-display text-[0.95rem] font-bold tracking-tight text-ink-900">{title}</h2>
            {subtitle !== undefined ? <p className="mt-0.5 text-xs text-ink-500">{subtitle}</p> : null}
          </div>
          {actions !== undefined ? <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div> : null}
        </header>
      ) : null}
      <div className={cn(padded && 'px-5 py-4', bodyClassName)}>{children}</div>
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
      {hint !== undefined ? <span className="mt-1.5 block text-xs text-ink-500">{hint}</span> : null}
    </label>
  );
}

export function Input(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={cn('input', props.className)} />;
}

export function Select(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={cn('input', props.className)} />;
}

export function Textarea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={cn('input font-mono text-xs leading-relaxed', props.className)} />;
}

export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon?: ReactNode;
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-dashed border-ink-300 bg-ink-50/50 px-6 py-12 text-center">
      {icon !== undefined ? (
        <span className="mx-auto mb-4 grid size-11 place-items-center rounded-xl bg-white text-brand-700 shadow-hair ring-1 ring-inset ring-ink-200">
          {icon}
        </span>
      ) : null}
      <p className="font-display text-base font-bold text-ink-900">{title}</p>
      <p className="mx-auto mt-1.5 max-w-md text-sm leading-relaxed text-ink-600">{description}</p>
      {action !== undefined ? <div className="mt-5 flex flex-wrap justify-center gap-2.5">{action}</div> : null}
    </div>
  );
}

export function StatusDot({ ok, label }: { ok: boolean; label: string }) {
  return (
    <span className="inline-flex items-center gap-2 text-xs text-ink-600">
      <span className={cn('size-2 rounded-full', ok ? 'bg-accent-500' : 'bg-coral-500')} />
      {label}
    </span>
  );
}

/**
 * Renders any thrown value as a readable error, including the server's field-level messages and the
 * correlation id, which is the one string a user can quote back to an operator.
 */
export function ErrorNote({ error, className }: { error: unknown; className?: string }) {
  if (error === null || error === undefined) {
    return null;
  }

  const message =
    error instanceof ApiError
      ? error.message
      : error instanceof Error
        ? error.message
        : 'Something went wrong';

  const fieldErrors = error instanceof ApiError ? error.fieldErrors : [];
  const correlationId = error instanceof ApiError ? error.correlationId : undefined;
  const offline = error instanceof ApiError && error.isNetworkFailure;

  return (
    <div
      role="alert"
      className={cn(
        'animate-fade-in rounded-xl border border-coral-200 bg-coral-50 px-4 py-3.5 text-sm text-coral-800',
        className,
      )}
    >
      <div className="flex gap-2.5">
        <AlertCircle className="mt-0.5 size-4 shrink-0" />
        <div className="min-w-0">
          <p className="font-semibold">{message}</p>
          {offline ? (
            <p className="mt-1 text-xs leading-relaxed text-coral-700">
              The console calls the Spring Boot API directly, so the service has to be running and the browser
              origin has to be allowed by its CORS policy.
            </p>
          ) : null}
          {fieldErrors.length > 0 ? (
            <ul className="mt-2 list-inside list-disc space-y-0.5 text-xs">
              {fieldErrors.map((fieldError) => (
                <li key={fieldError}>{fieldError}</li>
              ))}
            </ul>
          ) : null}
          {correlationId !== undefined ? (
            <p className="mt-2 font-mono text-[11px] text-coral-700/80">correlation id {correlationId}</p>
          ) : null}
        </div>
      </div>
    </div>
  );
}
