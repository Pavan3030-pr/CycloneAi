import { useSyncExternalStore } from 'react';
import type { AssetInput, ImpactAssessment, TrackPointInput } from '@/api/types';

/**
 * The scenario currently on screen: the track that was assessed, the assets it was assessed
 * against, and the resulting assessment.
 *
 * All three are kept together because the API returns only the assessment. Without the inputs, the
 * map could draw the storm's interpolated position but not the track it came from, and the advisory
 * panel could show text with no way to see what produced it. Persisted to `localStorage` so a reload
 * during a demo does not lose the screen.
 */

const STORAGE_KEY = 'cyclone.activeScenario';

export interface ActiveScenario {
  stormId: string;
  points: TrackPointInput[];
  assets: AssetInput[];
  assessment: ImpactAssessment;
}

const listeners = new Set<() => void>();

function readStored(): ActiveScenario | null {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (raw === null) {
      return null;
    }
    const parsed = JSON.parse(raw) as Partial<ActiveScenario>;
    return parsed.assessment === undefined ? null : (parsed as ActiveScenario);
  } catch {
    return null;
  }
}

let snapshot: ActiveScenario | null = readStored();

function emit(): void {
  for (const listener of listeners) {
    listener();
  }
}

export function publishScenario(scenario: ActiveScenario): void {
  snapshot = scenario;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(scenario));
  } catch {
    // A blocked or full storage only costs persistence across reloads.
  }
  emit();
}

export function clearScenario(): void {
  snapshot = null;
  try {
    window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    // As above.
  }
  emit();
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function getSnapshot(): ActiveScenario | null {
  return snapshot;
}

export function useActiveScenario(): ActiveScenario | null {
  return useSyncExternalStore(subscribe, getSnapshot);
}
