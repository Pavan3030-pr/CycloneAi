#!/usr/bin/env bash
#
# Deploys CycloneAI to Cloud Run as a single service: the public site, the console and the API on one
# origin, reachable at one URL.
#
# Why one service rather than two: the console calls /api relative to its own origin, so there is no
# CORS configuration to get wrong, no API hostname to bake into the bundle at build time, and one URL
# to hand to a reviewer. The container is built from the repository root Dockerfile, which compiles
# the console and packages it inside the service jar.
#
# Usage:
#   PROJECT_ID=my-project ./deploy/cloudrun/deploy.sh
#   PROJECT_ID=my-project APP_GEMINI_API_KEY=... ./deploy/cloudrun/deploy.sh
#
# Environment:
#   PROJECT_ID            required — the Google Cloud project to deploy into
#   REGION                optional — default asia-south1 (Mumbai, closest to the Bay of Bengal)
#   SERVICE               optional — default cycloneai
#   APP_GEMINI_API_KEY    optional — stored in Secret Manager and mounted; without it the service
#                                    still works and issues deterministic advisories instead
#   APP_ADVISORY_MODE     optional — auto (default) | gemini | deterministic
#   ALLOW_UNAUTHENTICATED optional — default true, so the service URL is publicly reachable
#   APP_JWT_SECRET        optional — supplied for you and stored in Secret Manager when absent
#
# Idempotent: safe to re-run for every code change. Secrets are only created when missing, so
# re-running never rotates a live signing key and never invalidates issued tokens.

set -euo pipefail

PROJECT_ID="${PROJECT_ID:-}"
REGION="${REGION:-asia-south1}"
SERVICE="${SERVICE:-cycloneai}"
IMAGE_REPO="${IMAGE_REPO:-cycloneai}"
JWT_SECRET_NAME="${JWT_SECRET_NAME:-cycloneai-jwt-secret}"
GEMINI_SECRET_NAME="${GEMINI_SECRET_NAME:-cycloneai-gemini-api-key}"
ALLOW_UNAUTHENTICATED="${ALLOW_UNAUTHENTICATED:-true}"
APP_ADVISORY_MODE="${APP_ADVISORY_MODE:-auto}"

if [[ -z "${PROJECT_ID}" ]]; then
  echo "PROJECT_ID is required. Example:" >&2
  echo "  PROJECT_ID=my-project ./deploy/cloudrun/deploy.sh" >&2
  exit 1
fi

command -v gcloud >/dev/null 2>&1 || {
  echo "gcloud is not installed. See https://cloud.google.com/sdk/docs/install" >&2
  exit 1
}

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${REPO_ROOT}"

echo "==> Project ${PROJECT_ID}, region ${REGION}, service ${SERVICE}"

gcloud config set project "${PROJECT_ID}" --quiet >/dev/null

echo "==> Enabling the APIs this deployment needs (no-op when already enabled)"
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  --project "${PROJECT_ID}" --quiet

# ---------------------------------------------------------------- secrets
# A missing secret is created; an existing one is left exactly as it is. Rotating the signing key on
# every deploy would silently invalidate every token a team is holding mid-storm.
ensure_secret() {
  local name="$1"
  if gcloud secrets describe "${name}" --project "${PROJECT_ID}" >/dev/null 2>&1; then
    echo "    secret ${name} already exists — leaving it untouched"
  else
    echo "    creating secret ${name}"
    gcloud secrets create "${name}" \
      --replication-policy=automatic --project "${PROJECT_ID}" --quiet >/dev/null
  fi
}

put_secret_value() {
  local name="$1" value="$2"
  printf '%s' "${value}" | gcloud secrets versions add "${name}" \
    --data-file=- --project "${PROJECT_ID}" --quiet >/dev/null
  echo "    stored a new version of ${name}"
}

echo "==> JWT signing key (HS256, 48 random bytes)"
ensure_secret "${JWT_SECRET_NAME}"
if [[ -n "${APP_JWT_SECRET:-}" ]]; then
  put_secret_value "${JWT_SECRET_NAME}" "${APP_JWT_SECRET}"
elif [[ "$(gcloud secrets versions list "${JWT_SECRET_NAME}" --project "${PROJECT_ID}" \
        --filter='state=ENABLED' --format='value(name)' --limit=1)" == "" ]]; then
  put_secret_value "${JWT_SECRET_NAME}" "$(openssl rand -base64 48 | tr -d '\n')"
else
  echo "    ${JWT_SECRET_NAME} already has an enabled version — reusing it"
fi

SECRETS="APP_JWT_SECRET=${JWT_SECRET_NAME}:latest"

echo "==> Gemini API key"
if [[ -n "${APP_GEMINI_API_KEY:-}" ]]; then
  ensure_secret "${GEMINI_SECRET_NAME}"
  put_secret_value "${GEMINI_SECRET_NAME}" "${APP_GEMINI_API_KEY}"
  SECRETS="${SECRETS},APP_GEMINI_API_KEY=${GEMINI_SECRET_NAME}:latest"
elif gcloud secrets describe "${GEMINI_SECRET_NAME}" --project "${PROJECT_ID}" >/dev/null 2>&1; then
  SECRETS="${SECRETS},APP_GEMINI_API_KEY=${GEMINI_SECRET_NAME}:latest"
  echo "    mounting the existing ${GEMINI_SECRET_NAME}"
else
  echo "    no key supplied and none stored: the deterministic advisory generator will be used,"
  echo "    and every response will say so in its provenance field"
fi

# ---------------------------------------------------------------- deploy
echo "==> Building the image with Cloud Build and deploying to Cloud Run"
# --source . uses the root Dockerfile, so the console is compiled and packaged inside the service.
# Secrets are mounted as environment variables rather than baked into the image.
gcloud run deploy "${SERVICE}" \
  --source . \
  --project "${PROJECT_ID}" \
  --region "${REGION}" \
  --platform managed \
  --port 8080 \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 4 \
  --timeout 60 \
  --set-secrets "${SECRETS}" \
  --set-env-vars "APP_ADVISORY_MODE=${APP_ADVISORY_MODE},APP_TOKEN_TTL=PT2H,APP_RATE_LIMIT_CAPACITY=120" \
  --allow-unauthenticated="${ALLOW_UNAUTHENTICATED}" \
  --quiet

SERVICE_URL="$(gcloud run services describe "${SERVICE}" \
  --project "${PROJECT_ID}" --region "${REGION}" --format='value(status.url)')"

echo
echo "==> Deployed"
echo "    Site      ${SERVICE_URL}"
echo "    Console   ${SERVICE_URL}/app"
echo "    API docs  ${SERVICE_URL}/swagger-ui.html"
echo "    Health    ${SERVICE_URL}/actuator/health"
echo
echo "Sign in with analyst / cyclone-demo-analyst, or click Start free demo on the landing page."
echo
echo "Before showing this to anyone outside the team:"
echo "  * set APP_DEMO_*_PASSWORD so the documented demo credentials no longer work, and"
echo "  * decide whether APP_API_DOCS_PUBLIC should stay true."
