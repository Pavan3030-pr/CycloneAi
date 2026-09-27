import type { AssetType, RiskLevel } from '@/api/types';

/**
 * Presentation of the two domain enums, kept in one place so a level reads the same in a badge, a
 * table row and a map marker. The hex values are the same ones the Tailwind palette uses, which is
 * what lets Leaflet markers (which cannot take a Tailwind class) match the surrounding UI.
 *
 * The light theme uses tinted fills with a saturated ring rather than solid colour blocks: at a
 * glance the hue still carries the severity, but a table of twenty rows stays readable.
 */

/** Most severe first, the order every list in the UI uses. */
export const RISK_ORDER: RiskLevel[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

export interface RiskStyle {
  label: string;
  hex: string;
  badge: string;
  dot: string;
  /** Tint used behind a metric tile of this level. */
  tile: string;
  /** One-line reading of what the level means for an operator. */
  action: string;
}

export const RISK_STYLES: Record<RiskLevel, RiskStyle> = {
  CRITICAL: {
    label: 'Critical',
    hex: '#e11d48',
    badge: 'bg-coral-50 text-coral-700 ring-coral-200',
    dot: 'bg-coral-500',
    tile: 'border-coral-200 bg-coral-50/60',
    action: 'Act now — pre-position and evacuate',
  },
  HIGH: {
    label: 'High',
    hex: '#f97316',
    badge: 'bg-orange-50 text-orange-700 ring-orange-200',
    dot: 'bg-orange-500',
    tile: 'border-orange-200 bg-orange-50/60',
    action: 'Prepare — crews on standby',
  },
  MEDIUM: {
    label: 'Medium',
    hex: '#f59e0b',
    badge: 'bg-amber-50 text-amber-700 ring-amber-200',
    dot: 'bg-amber-500',
    tile: 'border-amber-200 bg-amber-50/60',
    action: 'Monitor — re-screen next fix',
  },
  LOW: {
    label: 'Low',
    hex: '#10b981',
    badge: 'bg-accent-50 text-accent-700 ring-accent-200',
    dot: 'bg-accent-500',
    tile: 'border-accent-200 bg-accent-50/60',
    action: 'No action beyond routine watch',
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

/** Category ramp, from depression through to category five. */
export const CATEGORY_STYLES: Record<string, string> = {
  TD: 'bg-sky-50 text-sky-700 ring-sky-200',
  TS: 'bg-accent-50 text-accent-700 ring-accent-200',
  CAT1: 'bg-amber-50 text-amber-700 ring-amber-200',
  CAT2: 'bg-orange-50 text-orange-700 ring-orange-200',
  CAT3: 'bg-coral-50 text-coral-700 ring-coral-200',
  CAT4: 'bg-coral-100 text-coral-800 ring-coral-300',
  CAT5: 'bg-fuchsia-50 text-fuchsia-700 ring-fuchsia-200',
};
