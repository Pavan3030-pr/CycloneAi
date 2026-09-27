/**
 * Display formatting.
 *
 * Everything is pinned to UTC rather than the browser's zone. Cyclone fixes are published in UTC and
 * an advisory that silently renders in local time is how an operational team ends up briefing the
 * wrong landfall window.
 */

const DATE_TIME = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'UTC',
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

const TIME_ONLY = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'UTC',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

export function formatInstant(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return `${DATE_TIME.format(parsed)} UTC`;
}

export function formatTimeOnly(value: string): string {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : `${TIME_ONLY.format(parsed)} UTC`;
}

export function formatDecimal(value: number, digits = 0): string {
  if (!Number.isFinite(value)) {
    return '—';
  }
  return value.toFixed(digits);
}

export function formatCount(value: number): string {
  return new Intl.NumberFormat('en-GB').format(value);
}
