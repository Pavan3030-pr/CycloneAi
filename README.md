# Cyclone Impact & Infrastructure Vulnerability Forecaster

An end-to-end platform that answers one operational question: **given this cyclone track, which
critical infrastructure is about to be hit, how hard, and why?**

A Spring Boot domain core ingests NOAA/JTWC-style tracks, interpolates the storm between published
fixes, scores every registered asset, and generates an early-warning advisory. A React console runs
assessments against the live API and puts the result on a map.

```
cyclone-impact-forecaster/
├── backend/                 Spring Boot 3.5 · Java 21 · hexagonal
│   ├── src/main/java/com/enterprise/cyclone/
│   │   ├── domain/          model + services — zero framework dependencies
│   │   ├── application/     use cases and outbound ports
│   │   ├── adapter/in/web   REST controllers, DTOs, exception advice
│   │   ├── adapter/out      GeoJSON parser, in-memory registry, future GEE/Gemini seams
│   │   ├── security/        Spring Security 6, JWT, rate limiting, correlation ids
│   │   └── config/          wiring, OpenAPI, JSON hardening, health indicator
│   └── src/main/resources/application.yml
├── frontend/                Vite · React 18 · TypeScript · Tailwind · Leaflet
├── docker-compose.yml
├── run-demo.sh
└── README.md
```

---

## Quick start

### Prerequisites

| Tool  | Version used |
|-------|--------------|
| JDK   | 21+ (built and verified on 21 and 25) |
| Maven | 3.9+ |
| Node  | 20+ (verified on 24) |
| npm   | 10+ (verified on 11) |

### Option A — one command

```bash
./run-demo.sh
```

Starts the API on **8080** and the console on **5173**, waits for the API to answer, and stops both
on Ctrl-C. Logs go to `backend.log` and `frontend.log`.

### Option B — two terminals

```bash
# terminal 1 — API on http://localhost:8080
cd backend
mvn spring-boot:run
```

```bash
# terminal 2 — console on http://localhost:5173
cd frontend
npm install
npm run dev
```

### Option C — containers

```bash
docker compose up --build
```

Console on <http://localhost:5173>, API on <http://localhost:8080>. Set `APP_JWT_SECRET` in the
environment before using this anywhere shared.

### Then

Open <http://localhost:5173> and click **Demo mode**. That signs in as the demo analyst, registers
seven coastal assets, runs a real assessment against the Bay of Bengal storm, and lands you on the
dashboard. OpenAPI documentation is at <http://localhost:8080/swagger-ui.html>.

---

## The demo storm

A synthetic reconstruction of a Michaung-like December storm: six-hourly fixes from the southern Bay
of Bengal to landfall near Bapatla, Andhra Pradesh. It is evaluated at `2023-12-05T06:00:00Z`, a
**mid-interval** instant, so the interpolated centre is a modelled position rather than a published
fix — which is the whole point of the track aggregate.

Expected result: **2 critical, 1 high, 1 medium, 3 low.** The Bapatla coastal shelter sits 13 nm from
the interpolated centre and the NH-16 Krishna delta span 19 nm, so both trip the distance-based
hurricane-force band even though the storm peaks at 58 kt.

---

## Credentials and roles

Configured in `backend/src/main/resources/application.yml` under `app.security.users`, overridable by
environment variable. **These are local demo credentials**: replace them and set `APP_JWT_SECRET`
before any deployment a stranger can reach. Passwords may also be supplied as BCrypt hashes.

| Username | Password                | Roles              | Can do |
|----------|-------------------------|--------------------|--------|
| `admin`  | `cyclone-demo-admin`    | ADMIN, ANALYST, VIEWER | Everything, including actuator metrics |
| `analyst`| `cyclone-demo-analyst`  | ANALYST, VIEWER    | Assess, read and write the asset registry |
| `viewer` | `cyclone-demo-viewer`   | VIEWER             | Assess and read only |

Get a token by hand when you want to call the API directly:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"analyst","password":"cyclone-demo-analyst"}' | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

curl -s http://localhost:8080/api/v1/assets -H "Authorization: Bearer $TOKEN"
```

---

## API

All endpoints except the token endpoint and the health probe require `Authorization: Bearer <token>`.

| Method | Path | Role | Purpose |
|--------|------|------|---------|
| POST | `/api/v1/auth/token` | anonymous | Exchange credentials for a JWT |
| POST | `/api/v1/impact-assessments` | any | Assess a structured track against a list of assets |
| POST | `/api/v1/impact-assessments/from-geojson` | any | Assess a raw GeoJSON feature collection |
| GET | `/api/v1/assets` | any | List the registry, ordered by identifier |
| GET | `/api/v1/assets/{id}` | any | Fetch one asset |
| POST | `/api/v1/assets` | ANALYST, ADMIN | Register or replace an asset |
| DELETE | `/api/v1/assets/{id}` | ANALYST, ADMIN | Remove an asset |
| GET | `/actuator/health` | anonymous | Liveness, readiness and a domain self-test |
| GET | `/actuator/info`, `/actuator/metrics` | ADMIN | Build info and metrics |
| GET | `/swagger-ui.html`, `/v3/api-docs` | anonymous by default | API documentation |

Failures are RFC 9457 problem documents. Errors name the offending field, and every response carries
an `X-Correlation-Id` header that also appears on the matching log line.

```bash
curl -s -X POST http://localhost:8080/api/v1/impact-assessments \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
    "track": {"stormId": "IO-DEMO-01", "points": [
      {"latitude": 13.9, "longitude": 81.8, "windSpeedKnots": 60, "centralPressureMb": 986, "timestamp": "2023-12-04T00:00:00Z"},
      {"latitude": 15.4, "longitude": 80.5, "windSpeedKnots": 55, "centralPressureMb": 990, "timestamp": "2023-12-05T00:00:00Z"}]},
    "assets": [{"id": "AST-1", "name": "Bapatla shelter", "assetType": "MEDICAL_SHELTER",
                "latitude": 15.9, "longitude": 80.47}],
    "evaluationTime": "2023-12-04T12:00:00Z"}'
```

The GeoJSON endpoint accepts what agencies actually publish: `maxwind`/`mslp`/`validtime` (with the
usual aliases), RFC 3339 or ATCF compact times, and skips non-`Point` features such as the drawn
track line and the wind cone.

---

## Configuration

Every value is externalizable. The important ones:

| Variable | Default | Notes |
|----------|---------|-------|
| `SERVER_PORT` | `8080` | API port |
| `APP_JWT_SECRET` | *(generated)* | HS256 secret, at least 32 bytes. When absent a random key is generated per boot and tokens do not survive a restart |
| `APP_JWT_ISSUER` | `cyclone-impact-forecaster` | Tokens from another issuer are rejected |
| `APP_TOKEN_TTL` | `PT2H` | Token lifetime |
| `APP_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Exact CORS origins, no wildcards |
| `APP_MAX_PAYLOAD_BYTES` | `1048576` | Request bodies larger than this are refused with 413 |
| `APP_RATE_LIMIT_ENABLED` | `true` | Token-bucket limiter on assessment and auth paths |
| `APP_RATE_LIMIT_CAPACITY` | `120` | Requests per window, per principal |
| `APP_RATE_LIMIT_REFILL` | `PT1M` | Window length |
| `APP_DEMO_ADMIN_PASSWORD` | `cyclone-demo-admin` | Demo directory; replace in any shared environment |
| `APP_API_DOCS_PUBLIC` | `true` | Set to `false` so the API documentation requires ADMIN |

Frontend variables live in `frontend/.env` (see `.env.example`): `VITE_API_BASE_URL`,
`VITE_DEMO_USERNAME`, `VITE_DEMO_PASSWORD`.

---

## Security

Implemented in `com.enterprise.cyclone.security` and verified by hand; see the checklist in the
project summary. In short:

- **Authentication** — HS256 JWT via Spring Security's resource server. The signing key is never
  compiled in: absent configuration, a random key is generated and the trade-off is logged. Tokens
  are issuer-validated, so one minted for another environment cannot be replayed.
- **Authorisation** — ADMIN/ANALYST/VIEWER, enforced on the URL patterns, not in controllers.
  Anything unmatched falls through to `authenticated()`, so a new endpoint is protected by default.
- **Rate limiting** — a token bucket keyed by principal, falling back to the socket peer address
  (never `X-Forwarded-For`, which a client can forge). Guards the assessment and token paths, so the
  login endpoint cannot be used to grind passwords.
- **Input handling** — bean validation on every DTO with size caps on all collections and strings,
  a body size limit checked before the body is read, and Jackson parser constraints on nesting depth,
  string length and number length for chunked requests that declare no length.
- **Logging** — one access line per request with method, path, status, duration and principal. Never
  bodies, never headers, because both carry credentials. Correlation ids are validated against a
  character class before being written to a log line.
- **Headers and CORS** — exact-origin CORS with credentials disabled (the API uses a bearer header,
  not a cookie), `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, and a
  restrictive `Content-Security-Policy`.
- **Actuator** — only health, info and metrics are exposed; health is anonymous for probes, the rest
  needs ADMIN.

---

## Tests

```bash
cd backend && mvn test      # 52 domain tests
cd frontend && npm run build  # typecheck + production build
```

The backend suite covers the domain only: coordinate validation, interpolation including the
antimeridian case, chronological canonicalisation, duplicate-timestamp rejection, and the
lockdown/refresh of JsonNode parsing. The web, security and adapter layers were verified against a
running instance rather than with a mocked container.

---

## Notes and limitations

- **The exposure model is a screening model.** The wind field decays exponentially with distance
  (75 nm e-folding) and ignores terrain, gust factor, quadrant asymmetry and forecast positional
  uncertainty. It answers "which assets deserve attention first", not "what will the wind be here".
- **The asset registry is in-memory.** It implements a port, so a database is a new adapter rather
  than a change to callers. Assets do not survive a restart.
- **The satellite and model integrations are contracts, not implementations.**
  `GoogleEarthEngineClient` and `GeminiAdvisoryClient` define what those integrations must provide;
  the deterministic advisory generator is the working default and the fallback.
- **Swagger UI** is public by default for demonstrability; set `APP_API_DOCS_PUBLIC=false` to require
  ADMIN.
