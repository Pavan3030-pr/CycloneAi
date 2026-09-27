import type { ApiHealth, ProblemDetails, TokenResponse } from './types';

/**
 * The one place that knows how to talk to the API.
 *
 * Every call goes through {@link apiRequest}, which attaches the bearer token and a correlation id,
 * and converts a failure into an {@link ApiError} carrying the server's problem document. That means
 * components never parse error payloads themselves, they render `error.message` and, when present,
 * `error.fieldErrors`.
 */

const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/+$/, '');
const TOKEN_STORAGE_KEY = 'cyclone.accessToken';

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

export function apiBaseUrl(): string {
  return BASE_URL;
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

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  headers.set('X-Correlation-Id', newCorrelationId());
  if (init.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  if (accessToken !== null && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, { ...init, headers });
  } catch {
    throw new ApiError(0, `Cannot reach the API at ${BASE_URL}. Is the backend running?`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const payload: unknown = text.length > 0 ? safeJsonParse(text) : undefined;

  if (!response.ok) {
    const problem = isProblemDetails(payload) ? payload : undefined;
    const message = problem?.detail ?? problem?.title ?? `Request failed with status ${response.status}`;
    throw new ApiError(response.status, message, problem, problem?.correlationId);
  }
  return payload as T;
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
