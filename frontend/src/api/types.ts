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

/** Languages an advisory can be issued in, mirroring the backend's AdvisoryLanguage. */
export type AssessmentLanguage = 'en' | 'hi' | 'te';

export interface LanguageOption {
  code: AssessmentLanguage;
  /** Name of the language in that language. */
  label: string;
  /** Shown in the selector so an operator knows what stays in English. */
  note: string;
}

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

/**
 * How an advisory was produced.
 *
 * Rendered in the console so an operator can always tell model-written text from template text, and
 * can see when a model was attempted and failed. `detail` is omitted when there is nothing to explain.
 */
export interface AdvisoryProvenance {
  generator: 'gemini' | 'deterministic';
  model?: string | null;
  language: AssessmentLanguage;
  latencyMillis: number;
  degraded: boolean;
  detail?: string | null;
}

export interface ImpactAssessment {
  stormId: string;
  evaluatedAt: string;
  stormPosition: StormPosition;
  summary: ImpactSummary;
  advisory: string;
  advisoryProvenance: AdvisoryProvenance;
  exposures: AssetExposure[];
}

export interface ImpactAssessmentInput {
  track: { stormId: string; points: TrackPointInput[] };
  assets: AssetInput[];
  evaluationTime: string | null;
  language?: AssessmentLanguage;
}

export interface GeoJsonAssessmentInput {
  featureCollection: unknown;
  assets: AssetInput[];
  evaluationTime: string | null;
  language?: AssessmentLanguage;
}

/** Readiness of the outbound notification channel. */
export interface AdvisoryChannel {
  channel: string;
  configured: boolean;
  detail: string;
}

/** What was published and whether the channel accepted it. */
export interface AdvisoryDispatch {
  delivered: boolean;
  channel: string;
  channelConfigured: boolean;
  stormId: string;
  language: AssessmentLanguage;
  headline: string;
  advisory: string;
  generator: string;
  model?: string | null;
  degraded: boolean;
  capIdentifier: string;
  detail: string;
  latencyMillis: number;
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
