import type { AssetType, RiskLevel } from '@/api/types';

/**
 * Presentation of the two domain enums, kept in one place so a level reads the same in a badge, a
 * table row and a map marker. The hex values are the same ones the Tailwind palette uses, which is
 * what lets Leaflet markers (which cannot take a Tailwind class) match the surrounding UI.
 */

/** Most severe first, the order every list in the UI uses. */
export const RISK_ORDER: RiskLevel[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

export interface RiskStyle {
  label: string;
  hex: string;
  badge: string;
  dot: string;
}

export const RISK_STYLES: Record<RiskLevel, RiskStyle> = {
  CRITICAL: {
    label: 'Critical',
    hex: '#ef4444',
    badge: 'bg-red-500/15 text-red-600 ring-red-500/30 dark:text-red-400',
    dot: 'bg-red-500',
  },
  HIGH: {
    label: 'High',
    hex: '#f97316',
    badge: 'bg-orange-500/15 text-orange-600 ring-orange-500/30 dark:text-orange-400',
    dot: 'bg-orange-500',
  },
  MEDIUM: {
    label: 'Medium',
    hex: '#f59e0b',
    badge: 'bg-amber-500/15 text-amber-600 ring-amber-500/30 dark:text-amber-400',
    dot: 'bg-amber-500',
  },
  LOW: {
    label: 'Low',
    hex: '#10b981',
    badge: 'bg-emerald-500/15 text-emerald-600 ring-emerald-500/30 dark:text-emerald-400',
    dot: 'bg-emerald-500',
  },
};

export const ASSET_TYPE_LABELS: Record<AssetType, string> = {
  POWER_GRID: 'Power grid',
  ARTERIAL_ROAD: 'Arterial road',
  MEDICAL_SHELTER: 'Medical shelter',
};

export const ASSET_TYPE_SHORT: Record<AssetType, string> = {
  POWER_GRID: 'GRID',
  ARTERIAL_ROAD: 'ROAD',
  MEDICAL_SHELTER: 'SHELTER',
};

export const ASSET_TYPES: AssetType[] = ['POWER_GRID', 'ARTERIAL_ROAD', 'MEDICAL_SHELTER'];

/** Category colour ramp, from depression through to category five. */
export const CATEGORY_STYLES: Record<string, string> = {
  TD: 'bg-sky-500/15 text-sky-600 ring-sky-500/30 dark:text-sky-400',
  TS: 'bg-cyan-500/15 text-cyan-600 ring-cyan-500/30 dark:text-cyan-400',
  CAT1: 'bg-amber-500/15 text-amber-600 ring-amber-500/30 dark:text-amber-400',
  CAT2: 'bg-orange-500/15 text-orange-600 ring-orange-500/30 dark:text-orange-400',
  CAT3: 'bg-red-500/15 text-red-600 ring-red-500/30 dark:text-red-400',
  CAT4: 'bg-red-600/20 text-red-700 ring-red-600/40 dark:text-red-400',
  CAT5: 'bg-fuchsia-600/20 text-fuchsia-700 ring-fuchsia-600/40 dark:text-fuchsia-400',
};
