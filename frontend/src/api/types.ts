/**
 * Wire contracts of the Cyclone Impact API.
 *
 * These mirror the backend request and response records exactly. They are hand-written rather than
 * generated so that a backend change surfaces as a type error here, and so the enums stay narrow:
 * a typo in a risk level is a compile error, not a blank badge at runtime.
 */

export type AssetType = 'POWER_GRID' | 'ARTERIAL_ROAD' | 'MEDICAL_SHELTER';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type SaffirSimpsonCategory = 'TD' | 'TS' | 'CAT1' | 'CAT2' | 'CAT3' | 'CAT4' | 'CAT5';
export type Role = 'ADMIN' | 'ANALYST' | 'VIEWER';

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  roles: Role[];
}

export interface Asset {
  id: string;
  name: string;
  assetType: AssetType;
  latitude: number;
  longitude: number;
  coordinateString: string;
}

export interface AssetInput {
  id: string;
  name: string;
  assetType: AssetType;
  latitude: number;
  longitude: number;
}

export interface TrackPointInput {
  latitude: number;
  longitude: number;
  windSpeedKnots: number;
  centralPressureMb: number;
  timestamp: string;
}

export interface StormPosition {
  latitude: number;
  longitude: number;
  windSpeedKnots: number;
  centralPressureMb: number;
  category: SaffirSimpsonCategory;
  coordinateString: string;
}

export interface ImpactSummary {
  totalAssets: number;
  countByRisk: Record<RiskLevel, number>;
  highestRisk: RiskLevel;
  nearestDistanceNauticalMiles: number;
  peakEstimatedWindKnots: number;
}

export interface AssetExposure {
  assetId: string;
  assetName: string;
  assetType: AssetType;
  coordinateString: string;
  distanceNauticalMiles: number;
  estimatedWindAtAsset: number;
  riskLevel: RiskLevel;
  rationale: string;
}

export interface ImpactAssessment {
  stormId: string;
  evaluatedAt: string;
  stormPosition: StormPosition;
  summary: ImpactSummary;
  advisory: string;
  exposures: AssetExposure[];
}

export interface ImpactAssessmentInput {
  track: { stormId: string; points: TrackPointInput[] };
  assets: AssetInput[];
  evaluationTime: string | null;
}

export interface GeoJsonAssessmentInput {
  featureCollection: unknown;
  assets: AssetInput[];
  evaluationTime: string | null;
}

export interface FieldError {
  field: string;
  message: string;
}

/** RFC 9457 problem document, as returned by every failing endpoint. */
export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  correlationId?: string;
  errors?: FieldError[];
}

export interface ApiHealth {
  status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN';
  groups?: string[];
}
