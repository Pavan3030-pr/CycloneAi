import type { AssetInput, ImpactAssessmentInput, TrackPointInput } from '@/api/types';

/**
 * The one-click demonstration scenario.
 *
 * A Bay of Bengal storm resembling Cyclone Michaung (December 2023), which formed south-east of Sri
 * Lanka, intensified over the southern Bay and made landfall near Bapatla on the Andhra Pradesh
 * coast. The fixes below are a synthetic reconstruction on a six-hourly cadence, not an official
 * best track, and the identifier says so. The assets are real coastal locations chosen because they
 * sit in the storm's path: two grid nodes, two stretches of the coastal highway, and two medical
 * shelters on either side of the expected landfall.
 *
 * Nothing here is fetched by default. This data is only sent when a user clicks Demo Mode, so the
 * console still runs entirely against the API in normal use.
 */

export const DEMO_STORM_ID = 'IO-DEMO-01';

export interface DemoScenario {
  stormId: string;
  label: string;
  summary: string;
  evaluationTime: string;
  points: TrackPointInput[];
  assets: AssetInput[];
}

export const bayOfBengalScenario: DemoScenario = {
  stormId: DEMO_STORM_ID,
  label: 'Bay of Bengal — Michaung-like storm',
  summary: 'Synthetic six-hourly track from the southern Bay to landfall near Bapatla, Andhra Pradesh.',
  // Deliberately mid-interval and at the coast, so the demonstration both exercises interpolation
  // rather than returning a published fix verbatim, and lands the storm on populated infrastructure.
  evaluationTime: '2023-12-05T06:00:00Z',
  points: [
    { latitude: 8.4, longitude: 87.1, windSpeedKnots: 30, centralPressureMb: 1004, timestamp: '2023-12-01T00:00:00Z' },
    { latitude: 10.3, longitude: 85.4, windSpeedKnots: 40, centralPressureMb: 998, timestamp: '2023-12-02T00:00:00Z' },
    { latitude: 12.1, longitude: 83.6, windSpeedKnots: 50, centralPressureMb: 992, timestamp: '2023-12-03T00:00:00Z' },
    { latitude: 13.9, longitude: 81.8, windSpeedKnots: 60, centralPressureMb: 986, timestamp: '2023-12-04T00:00:00Z' },
    { latitude: 15.4, longitude: 80.5, windSpeedKnots: 55, centralPressureMb: 990, timestamp: '2023-12-05T00:00:00Z' },
    { latitude: 16.1, longitude: 80.1, windSpeedKnots: 40, centralPressureMb: 998, timestamp: '2023-12-05T12:00:00Z' },
  ],
  assets: [
    {
      id: 'DEMO-GRID-ENNORE',
      name: 'Ennore thermal substation',
      assetType: 'POWER_GRID',
      latitude: 13.23,
      longitude: 80.32,
    },
    {
      id: 'DEMO-GRID-NELLORE',
      name: 'Nellore coastal grid node',
      assetType: 'POWER_GRID',
      latitude: 14.44,
      longitude: 79.98,
    },
    {
      id: 'DEMO-ROAD-NH16',
      name: 'NH-16 Krishna delta span',
      assetType: 'ARTERIAL_ROAD',
      latitude: 15.85,
      longitude: 80.62,
    },
    {
      id: 'DEMO-ROAD-CHENNAI',
      name: 'Chennai bypass (NH-32)',
      assetType: 'ARTERIAL_ROAD',
      latitude: 12.9,
      longitude: 80.15,
    },
    {
      id: 'DEMO-SHELTER-BAPATLA',
      name: 'Bapatla coastal shelter',
      assetType: 'MEDICAL_SHELTER',
      latitude: 15.9,
      longitude: 80.47,
    },
    {
      id: 'DEMO-SHELTER-CHENNAI',
      name: 'Chennai public health centre',
      assetType: 'MEDICAL_SHELTER',
      latitude: 13.08,
      longitude: 80.28,
    },
    {
      id: 'DEMO-GRID-MACHILIPATNAM',
      name: 'Machilipatnam coastal node',
      assetType: 'POWER_GRID',
      latitude: 16.17,
      longitude: 81.13,
    },
  ],
};

export function demoAssessmentInput(scenario: DemoScenario = bayOfBengalScenario): ImpactAssessmentInput {
  return {
    track: { stormId: scenario.stormId, points: scenario.points },
    assets: scenario.assets,
    evaluationTime: scenario.evaluationTime,
  };
}
