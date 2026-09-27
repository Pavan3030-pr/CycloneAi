import type { AssessmentLanguage, LanguageOption } from '@/api/types';

/**
 * Advisory languages, as offered to an operator.
 *
 * Names are given in the language itself, because a selector that offers "Telugu" to a Telugu
 * speaker is a selector designed for the developer. Each option states what stays in English, which
 * is not a caveat but the truth: rule citations come from the domain model and unit symbols are what
 * an operator reads off the instruments, so neither is translated.
 */
export const ADVISORY_LANGUAGES: LanguageOption[] = [
  {
    code: 'en',
    label: 'English',
    note: 'Full advisory text, including rule citations.',
  },
  {
    code: 'hi',
    label: 'हिन्दी — Hindi',
    note: 'Headline, summary, priorities, instruction and CAP text in Hindi.',
  },
  {
    code: 'te',
    label: 'తెలుగు — Telugu',
    note: 'Headline, summary, priorities, instruction and CAP text in Telugu.',
  },
];

/** Language used when nothing has been chosen. */
export const DEFAULT_ADVISORY_LANGUAGE: AssessmentLanguage = 'en';

const STORAGE_KEY = 'cyclone.advisoryLanguage';

/** Reads the operator's last choice, so a district that works in Telugu is not asked every time. */
export function readPreferredLanguage(): AssessmentLanguage {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (stored !== null && ADVISORY_LANGUAGES.some((option) => option.code === stored)) {
      return stored as AssessmentLanguage;
    }
  } catch {
    // Storage being unavailable only costs the preference.
  }
  return DEFAULT_ADVISORY_LANGUAGE;
}

export function rememberPreferredLanguage(language: AssessmentLanguage): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, language);
  } catch {
    // As above.
  }
}

export function languageOption(code: AssessmentLanguage): LanguageOption {
  return ADVISORY_LANGUAGES.find((option) => option.code === code) ?? ADVISORY_LANGUAGES[0];
}
