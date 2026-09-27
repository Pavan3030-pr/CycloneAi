import type { TrackPointInput } from '@/api/types';

/**
 * Client-side helpers for the track a user is about to submit.
 *
 * These parse and validate, but they never soften anything: a malformed point is reported to the
 * user before the request is sent, and the backend's own validation remains the authority. The
 * mirror of the server's property aliases exists so that the map can be drawn from a pasted GeoJSON
 * document, which the server does not echo back in its response.
 */

const WIND_FIELDS = ['maxwind', 'intensity', 'vmax', 'windSpeedKnots'] as const;
const PRESSURE_FIELDS = ['mslp', 'pressure', 'minpressure', 'centralPressureMb'] as const;
const TIME_FIELDS = ['validtime', 'valid', 'timestamp', 'time'] as const;

/** Parses the editable array of track points. Throws a user-facing Error on any problem. */
export function parseTrackPoints(raw: string): TrackPointInput[] {
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    throw new Error('Track points must be valid JSON.');
  }
  if (!Array.isArray(parsed) || parsed.length === 0) {
    throw new Error('Track points must be a non-empty array.');
  }
  return parsed.map((entry, index) => toTrackPoint(entry, `point ${index}`));
}

function toTrackPoint(entry: unknown, where: string): TrackPointInput {
  if (typeof entry !== 'object' || entry === null) {
    throw new Error(`${where} must be an object.`);
  }
  const record = entry as Record<string, unknown>;
  const latitude = requireNumber(record.latitude, `${where}.latitude`);
  const longitude = requireNumber(record.longitude, `${where}.longitude`);
  const windSpeedKnots = requireNumber(record.windSpeedKnots, `${where}.windSpeedKnots`);
  const centralPressureMb = requireNumber(record.centralPressureMb, `${where}.centralPressureMb`);
  const timestamp = record.timestamp;
  if (typeof timestamp !== 'string' || Number.isNaN(Date.parse(timestamp))) {
    throw new Error(`${where}.timestamp must be an RFC 3339 instant such as 2026-09-27T06:00:00Z.`);
  }
  return { latitude, longitude, windSpeedKnots, centralPressureMb, timestamp: new Date(timestamp).toISOString() };
}

function requireNumber(value: unknown, where: string): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`${where} must be a finite number.`);
  }
  return value;
}

/**
 * Best-effort extraction of point fixes from a GeoJSON feature collection, so a pasted document can
 * be plotted. Returns an empty array when nothing recognisable is present; the assessment itself has
 * already succeeded by then, so a missing track only costs the map overlay.
 */
export function trackPointsFromFeatureCollection(document: unknown): TrackPointInput[] {
  const features = readFeatures(document);
  const points: TrackPointInput[] = [];

  for (const feature of features) {
    if (typeof feature !== 'object' || feature === null) {
      continue;
    }
    const candidate = feature as { geometry?: unknown; properties?: unknown };
    const geometry = candidate.geometry as { type?: unknown; coordinates?: unknown } | undefined;
    if (geometry?.type !== 'Point' || !Array.isArray(geometry.coordinates) || geometry.coordinates.length < 2) {
      continue;
    }
    const [longitude, latitude] = geometry.coordinates as unknown[];
    if (typeof latitude !== 'number' || typeof longitude !== 'number') {
      continue;
    }
    const properties = (candidate.properties ?? {}) as Record<string, unknown>;
    const wind = firstNumeric(properties, WIND_FIELDS);
    const pressure = firstNumeric(properties, PRESSURE_FIELDS);
    const time = firstText(properties, TIME_FIELDS);
    const timestamp = time === null ? null : toInstant(time);
    if (wind === null || pressure === null || timestamp === null) {
      continue;
    }
    points.push({ latitude, longitude, windSpeedKnots: wind, centralPressureMb: pressure, timestamp });
  }
  return points;
}

function readFeatures(document: unknown): unknown[] {
  if (typeof document !== 'object' || document === null) {
    return [];
  }
  const features = (document as { features?: unknown }).features;
  return Array.isArray(features) ? features : [];
}

function firstNumeric(properties: Record<string, unknown>, names: readonly string[]): number | null {
  for (const name of names) {
    const value = properties[name];
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value;
    }
  }
  return null;
}

function firstText(properties: Record<string, unknown>, names: readonly string[]): string | null {
  for (const name of names) {
    const value = properties[name];
    if (typeof value === 'string' && value.trim().length > 0) {
      return value.trim();
    }
  }
  return null;
}

/** Accepts an RFC 3339 instant or the ATCF compact UTC forms yyyyMMddHH and yyyyMMddHHmm. */
function toInstant(value: string): string | null {
  if (value.includes('T')) {
    const parsed = Date.parse(value);
    return Number.isNaN(parsed) ? null : new Date(parsed).toISOString();
  }
  if (value.length === 10 || value.length === 12) {
    const year = Number(value.slice(0, 4));
    const month = Number(value.slice(4, 6));
    const day = Number(value.slice(6, 8));
    const hour = Number(value.slice(8, 10));
    const minute = value.length === 12 ? Number(value.slice(10, 12)) : 0;
    const parsed = Date.UTC(year, month - 1, day, hour, minute);
    return Number.isNaN(parsed) ? null : new Date(parsed).toISOString();
  }
  return null;
}

export interface EvaluationOption {
  label: string;
  value: string;
}

/**
 * Candidate evaluation instants: every published fix, plus the midpoint of each interval. The
 * midpoints are the interesting ones, because they are where the track aggregate interpolates rather
 * than returning a published fix.
 */
export function evaluationOptions(points: TrackPointInput[]): EvaluationOption[] {
  const ordered = [...points].sort((left, right) => left.timestamp.localeCompare(right.timestamp));
  const options: EvaluationOption[] = [];

  ordered.forEach((point, index) => {
    options.push({ label: `Published fix · ${point.timestamp}`, value: point.timestamp });
    const next = ordered[index + 1];
    if (next !== undefined) {
      const midpoint = new Date((Date.parse(point.timestamp) + Date.parse(next.timestamp)) / 2).toISOString();
      options.push({ label: `Mid-interval · ${midpoint} (interpolated)`, value: midpoint });
    }
  });
  return options;
}
