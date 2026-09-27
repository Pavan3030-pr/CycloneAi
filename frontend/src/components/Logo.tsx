import { useId } from 'react';
import { cn } from '@/lib/cn';

/**
 * Brand mark.
 *
 * A spiral wound inside a rounded square: the spiral reads as a cyclone, the square as the built
 * environment it is measured against, and the teal tail is the advisory that comes out the other
 * side. Drawn rather than imported so it stays crisp at 20px in a navbar and at 96px on the landing
 * page, and so the mark inherits the brand gradient instead of shipping as a raster.
 */

interface LogoMarkProps {
  className?: string;
  /** Rounded-square size, as a Tailwind size utility. */
  size?: string;
}

export function LogoMark({ className, size = 'size-9' }: LogoMarkProps) {
  // useId keeps the gradient ids unique, which matters because the mark appears several times per page.
  const gradientId = `mark-${useId().replace(/[^a-zA-Z0-9]/g, '')}`;

  return (
    <svg viewBox="0 0 32 32" aria-hidden="true" className={cn(size, 'shrink-0', className)}>
      <defs>
        <linearGradient id={gradientId} x1="4" y1="2" x2="28" y2="30" gradientUnits="userSpaceOnUse">
          <stop stopColor="#4F7BF5" />
          <stop offset="0.55" stopColor="#2346C4" />
          <stop offset="1" stopColor="#1C3479" />
        </linearGradient>
      </defs>
      <rect width="32" height="32" rx="8.5" fill={`url(#${gradientId})`} />
      <path
        d="M16.4 6.4c4.7 0 8.2 3.2 8.2 7.6 0 3.6-2.6 6.2-6.2 6.2-2.9 0-5-1.8-5-4.4 0-2.2 1.6-3.8 3.7-3.8 1.6 0 2.8 1 2.8 2.4"
        fill="none"
        stroke="#ffffff"
        strokeWidth="2.4"
        strokeLinecap="round"
        opacity="0.95"
      />
      <path
        d="M15.6 25.6c-4.7 0-8.2-3.2-8.2-7.6"
        fill="none"
        stroke="#2FCBBB"
        strokeWidth="2.4"
        strokeLinecap="round"
      />
    </svg>
  );
}

interface LogoProps {
  className?: string;
  size?: string;
  /** `light` renders the wordmark for use on a dark or photographic surface. */
  tone?: 'dark' | 'light';
  /** Hides the wordmark, leaving only the mark. */
  markOnly?: boolean;
}

export function Logo({ className, size = 'size-9', tone = 'dark', markOnly = false }: LogoProps) {
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <LogoMark size={size} />
      {markOnly ? null : (
        <span
          className={cn(
            'font-display text-[1.0625rem] font-extrabold tracking-tightest',
            tone === 'light' ? 'text-white' : 'text-ink-900',
          )}
        >
          Cyclone
          <span className={tone === 'light' ? 'text-accent-300' : 'text-brand-600'}>AI</span>
        </span>
      )}
    </span>
  );
}
