import type {
  AdvisoryChannel,
  AdvisoryDispatch,
  ApiHealth,
  AssessmentLanguage,
  ImpactAssessmentInput,
  ProblemDetails,
  TokenResponse,
} from './types';

/**
 * The one place that knows how to talk to the API.
 *
 * Every call goes through {@link apiRequest}, which attaches the bearer token and a correlation id,
 * and converts a failure into an {@link ApiError} carrying the server's problem document. That means
 * components never parse error payloads themselves, they render `error.message` and, when present,
 * `error.fieldErrors`.
 */

/**
 * Where the API lives.
 *
 * In development this is the Spring Boot service on :8080 and the request is cross-origin, which
 * exercises the API's CORS policy exactly as a deployed environment would. In a production build the
 * console is served from the same origin as the API — the container's nginx proxies `/api` to the
 * service — so the base URL is empty and every call is a relative, same-origin request. That is not
 * a micro-optimisation: it removes CORS from the deployment picture entirely, which is the difference
 * between a console that works on a judge's laptop and one that works from a URL.
 *
 * `VITE_API_BASE_URL` still overrides both, for a console served from a different host than the API.
 */
const CONFIGURED_BASE_URL = import.meta.env.VITE_API_BASE_URL;
const BASE_URL = (CONFIGURED_BASE_URL ?? (import.meta.env.PROD ? '' : 'http://localhost:8080')).replace(/\/+$/, '');

const TOKEN_STORAGE_KEY = 'cyclone.accessToken';

/**
 * Announcement that a request was rejected because the token is no longer good.
 *
 * The token lives here and the principal lives in the auth provider, so the two have to be told
 * apart cleanly: this event is how the client says "that session is over" without importing React, and
 * the provider listens for it and returns the console to sign in.
 */
export const SESSION_EXPIRED_EVENT = 'cyclone:session-expired';

const TOKEN_ENDPOINT = '/api/v1/auth/token';

export class ApiError extends Error {
  readonly status: number;
  readonly problem?: ProblemDetails;
  readonly correlationId?: string;

  constructor(status: number, message: string, problem?: ProblemDetails, correlationId?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
    this.correlationId = correlationId;
  }

  /** True when the request never reached the API. */
  get isNetworkFailure(): boolean {
    return this.status === 0;
  }

  get fieldErrors(): string[] {
    return (this.problem?.errors ?? []).map((error) => `${error.field}: ${error.message}`);
  }
}

let accessToken: string | null = readStoredToken();

function readStoredToken(): string | null {
  try {
    return window.localStorage.getItem(TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
  try {
    if (token === null) {
      window.localStorage.removeItem(TOKEN_STORAGE_KEY);
    } else {
      window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
    }
  } catch {
    // A blocked storage API must not break authentication for the current tab.
  }
}

export function getAccessToken(): string | null {
  return accessToken;
}

/** Forgets the current token and tells the application to ask for a new one. */
function expireSession(): void {
  setAccessToken(null);
  try {
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
  } catch {
    // No window (a test environment): clearing the token is still the right outcome.
  }
}

/** The API location, for display in error messages. */
export function apiBaseUrl(): string {
  return BASE_URL.length === 0 ? window.location.origin : BASE_URL;
}

/**
 * Correlation id sent with each browser request, so a report from the UI can be traced to one
 * backend log line even if the server never saw the request body.
 */
function newCorrelationId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `web-${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`;
}

function withRequestHeaders(init: RequestInit, path: string): Headers {
  const headers = new Headers(init.headers);
  // Only defaulted, never forced: the CAP download asks for application/cap+xml, and overwriting that
  // here would make the server answer 406 to a request that is otherwise perfectly valid.
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }
  headers.set('X-Correlation-Id', newCorrelationId());
  if (init.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  // The token endpoint is never sent a credential. Sending one is not merely pointless, it is a
  // trap: the resource server authenticates any presented bearer token before it applies the
  // permitAll rule, so a token that has expired or been signed by a retired key makes the sign-in
  // request itself fail with 401 — and the user cannot sign in again to replace it. This one
  // exclusion is what keeps a stale token from locking a user out of the product.
  if (accessToken !== null && path !== TOKEN_ENDPOINT && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }
  return headers;
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, { ...init, headers: withRequestHeaders(init, path) });
  } catch {
    throw new ApiError(0, `Cannot reach the API at ${apiBaseUrl()}. Is the backend running?`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const payload: unknown = text.length > 0 ? safeJsonParse(text) : undefined;

  if (!response.ok) {
    // A rejected token is a session that ended, not a bug: the key may have rotated, the token may
    // have expired, or the service may have restarted. Dropping it here means the next screen asks
    // for a sign-in instead of every panel reporting its own 401. The token endpoint is excluded,
    // because a wrong password is not an expired session.
    if (response.status === 401 && accessToken !== null && path !== TOKEN_ENDPOINT) {
      expireSession();
    }
    throw toApiError(response, payload);
  }
  return payload as T;
}

function toApiError(response: Response, payload: unknown): ApiError {
  const problem = isProblemDetails(payload) ? payload : undefined;
  const message = problem?.detail ?? problem?.title ?? `Request failed with status ${response.status}`;
  return new ApiError(response.status, message, problem, problem?.correlationId);
}

function safeJsonParse(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

function isProblemDetails(value: unknown): value is ProblemDetails {
  return typeof value === 'object' && value !== null && ('detail' in value || 'title' in value);
}

export function login(username: string, password: string): Promise<TokenResponse> {
  return apiRequest<TokenResponse>('/api/v1/auth/token', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}

export function fetchHealth(): Promise<ApiHealth> {
  return apiRequest<ApiHealth>('/actuator/health');
}

/** Readiness of the notification channel, so the console can say whether dispatch is available. */
export function fetchAdvisoryChannel(): Promise<AdvisoryChannel> {
  return apiRequest<AdvisoryChannel>('/api/v1/advisories/channel');
}

/**
 * Runs an assessment and returns the result as a CAP 1.2 alert document.
 *
 * Downloading rather than displaying: the artefact exists to be handed to another system — a state
 * operations centre, an aggregator, the cell-broadcast chain — so the browser saving a file is the
 * right interaction, and the server is the only place the document is rendered.
 */
export async function downloadCapAlert(
  input: ImpactAssessmentInput,
  language: AssessmentLanguage,
): Promise<{ blob: Blob; filename: string }> {
  const init: RequestInit = {
    method: 'POST',
    headers: { Accept: 'application/cap+xml' },
    body: JSON.stringify({ ...input, language }),
  };

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}/api/v1/impact-assessments/cap`, {
      ...init,
      headers: withRequestHeaders(init, '/api/v1/impact-assessments/cap'),
    });
  } catch {
    throw new ApiError(0, `Cannot reach the API at ${apiBaseUrl()}. Is the backend running?`);
  }

  if (!response.ok) {
    const text = await response.text();
    throw toApiError(response, text.length > 0 ? safeJsonParse(text) : undefined);
  }

  return {
    blob: await response.blob(),
    filename: filenameFromDisposition(response.headers.get('Content-Disposition')) ?? 'cyclone-alert.xml',
  };
}

/** Reads the server's filename, falling back to a stable name when the header is absent. */
function filenameFromDisposition(disposition: string | null): string | null {
  if (disposition === null) {
    return null;
  }
  const match = /filename="?([^";]+)"?/i.exec(disposition);
  return match === null ? null : match[1];
}

/**
 * Re-assesses the supplied track on the server and publishes the advisory to the configured channel.
 *
 * The request carries the assessment inputs rather than the advisory text, because the server is the
 * only authority on what a warning says: a client that could post arbitrary prose to a messaging
 * provider would turn this endpoint into a spam relay.
 */
export function dispatchAdvisory(
  input: ImpactAssessmentInput,
  language: AssessmentLanguage,
): Promise<AdvisoryDispatch> {
  return apiRequest<AdvisoryDispatch>('/api/v1/advisories/dispatch', {
    method: 'POST',
    body: JSON.stringify({ ...input, language }),
  });
}
