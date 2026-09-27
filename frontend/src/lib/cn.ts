/**
 * Joins class names, dropping falsy entries so conditional classes can be written inline.
 */
export function cn(...values: Array<string | false | null | undefined>): string {
  return values.filter(Boolean).join(' ');
}
