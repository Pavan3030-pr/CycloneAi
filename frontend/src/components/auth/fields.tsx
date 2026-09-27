import { Eye, EyeOff, type LucideIcon } from 'lucide-react';
import { useId, useState, type InputHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

/**
 * Form fields for the auth screens.
 *
 * Slightly taller and softer than the console inputs, with a leading icon: on an authentication page
 * the field is the whole interface, so it carries more of the visual weight. Labels are always
 * visible rather than floating, because a floating label that has animated out is a label a
 * distracted user cannot re-find.
 */

interface AuthFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  icon: LucideIcon;
  hint?: string;
}

export function AuthField({ label, icon: Icon, hint, className, ...rest }: AuthFieldProps) {
  const id = useId();

  return (
    <div>
      <label htmlFor={id} className="block text-sm font-semibold text-ink-800">
        {label}
      </label>
      <div className="relative mt-2">
        <Icon className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-ink-400" />
        <input
          id={id}
          {...rest}
          className={cn(
            'w-full rounded-xl border border-ink-200 bg-white py-3 pl-10.5 pr-3.5 text-sm text-ink-900 shadow-hair transition',
            'placeholder:text-ink-400 hover:border-ink-300',
            'focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/10',
            className,
          )}
        />
      </div>
      {hint !== undefined ? <p className="mt-1.5 text-xs text-ink-500">{hint}</p> : null}
    </div>
  );
}

export function PasswordField({ label, icon: Icon, ...rest }: Omit<AuthFieldProps, 'type'>) {
  const id = useId();
  const [visible, setVisible] = useState(false);

  return (
    <div>
      <label htmlFor={id} className="block text-sm font-semibold text-ink-800">
        {label}
      </label>
      <div className="relative mt-2">
        <Icon className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-ink-400" />
        <input
          id={id}
          {...rest}
          type={visible ? 'text' : 'password'}
          className="w-full rounded-xl border border-ink-200 bg-white py-3 pl-10.5 pr-11 text-sm text-ink-900 shadow-hair transition placeholder:text-ink-400 hover:border-ink-300 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/10"
        />
        <button
          type="button"
          onClick={() => setVisible((current) => !current)}
          aria-label={visible ? 'Hide password' : 'Show password'}
          className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-2 text-ink-400 transition hover:bg-ink-100 hover:text-ink-700"
        >
          {visible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
        </button>
      </div>
    </div>
  );
}
