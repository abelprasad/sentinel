# Sentinel

Sentinel is an air traffic anomaly detection system I built to track real ADS-B flights over the Philadelphia area, compute behavioral baselines per aircraft, score deviations in real time, and use an async LLM call to generate a one-sentence explanation for each flagged event. The public landing page lives at [sentinel.abelprasad.dev/public/status](https://sentinel.abelprasad.dev/public/status).

I built this because I wanted a project that sits at the intersection of defense-adjacent data (ADS-B, ICAO hex classification, military vs. civil aircraft), backend system design (scheduled ingestion, async processing, stateless JWT auth), and applied AI (LLM-assisted anomaly narration). It is not a production SIGINT tool. It is a portfolio project that reflects how real ABI (Activity Based Intelligence) pipelines work: ingest telemetry, build behavioral models, score deviations, and surface the interesting ones.

---

## Tech Stack

- **Java 21** with **Spring Boot 4.0.6** (web, security, data JPA, scheduling, async)
- **PostgreSQL 16** with **Flyway** for versioned schema management
- **JJWT 0.12.6** for stateless JWT authentication
- **Groq API** (`llama-3.3-70b-versatile`) for async anomaly narration
- **adsb.fi** open data API for live ADS-B telemetry
- **Docker** (multi-stage build) and **Docker Compose** for local and remote deployment
- **GitHub Actions** for CI/CD with Docker Hub push and SSH-based remote deploy over Tailscale

---

## Architecture

```
[adsb.fi API]
     |
     v  (every 30s, @Scheduled)
AdsbIngestionService
     |-- auto-creates AircraftEntity if new hex seen
     |-- classifies by ICAO hex prefix (MILITARY, CIVIL-US, etc.)
     |-- calls BaselineService.calculate() (needs >= 3 events)
     |-- calls AnomalyScoreService.score()
              |-- computes normalized deviation across altitude, speed, heading, lat, lon
              |-- if score >= 0.7 and entity not flagged in last 5 min: saves AnomalyScore
              |-- fires async GroqLlmService.summarizeAnomaly() -> updates explanation field
```

The HTTP layer is a standard Spring MVC REST API sitting behind a stateless JWT filter. Roles are `ANALYST`, `OPERATOR`, and `ADMIN`, enforced with `@EnableMethodSecurity` and `@PreAuthorize` annotations. There is no session state; every request is independently authenticated by the Bearer token.

PostgreSQL is the only datastore. Schema is owned by Flyway. Hibernate runs with `ddl-auto: none`. Data older than 7 days is pruned nightly at 3 AM by `PruneService`.

The frontend (a separate repo, `abelprasad/sentinel-ui`) is deployed as its own Docker container and proxied through the same host. CI/CD deploys both together.

---

## Key Features

### ADS-B Ingestion

`AdsbIngestionService` polls the adsb.fi open data API every 30 seconds for aircraft within 25 km of 40.0°N, 75.1°W (Philadelphia). For each aircraft in the response, it upserts an `AircraftEntity` keyed on ICAO hex, stores a `FlightEvent` with position and kinematics, then kicks off baseline calculation and anomaly scoring.

### Behavioral Baselines

`BaselineService` requires at least 3 historical events before computing a baseline. The baseline stores mean altitude, speed, heading, latitude, and longitude for a given aircraft. It is recalculated on every new event ingestion. The `Baseline` table has a one-to-one relationship with `AircraftEntity`.

### Anomaly Scoring

`AnomalyScoreService.score()` computes a normalized deviation for each kinematic dimension:

- altitude deviation: `|event - baseline| / 10000`
- speed deviation: `|event - baseline| / 200`
- heading deviation: `|event - baseline| / 180`
- lat/lon deviations: `|event - baseline| / 10.0`

The final score is the max across all dimensions, clamped to [0, 1]. Anything above 0.7 is flagged. To avoid alert storms, the same entity will not generate a new `AnomalyScore` within a 5-minute window.

### Async Groq LLM

When an anomaly is saved, `GroqLlmService.summarizeAnomaly()` fires asynchronously (Spring `@Async` thread pool). It sends the aircraft callsign, ICAO hex, anomaly score, and deviation breakdown to `llama-3.3-70b-versatile` on Groq and asks for a one-sentence explanation. The response updates `AnomalyScore.explanation` in the database. If the Groq call fails, a rule-based fallback explanation is used instead.

### ICAO Entity Classification

`IcaoClassificationService` classifies aircraft by ICAO hex prefix at ingestion time:

| Prefix / Range       | Classification |
|----------------------|----------------|
| `AE*`                | MILITARY       |
| `A*` (not AE)        | CIVIL-US       |
| `C*`                 | CIVIL-CA       |
| `400000–43FFFF`      | CIVIL-UK       |
| `380000–3BFFFF`      | CIVIL-FR       |
| `3C0000–3FFFFF`      | CIVIL-DE       |
| `700000–73FFFF`      | CIVIL-RU       |
| `780000–7BFFFF`      | CIVIL-CN       |
| Everything else      | UNKNOWN        |

This is a simplified approximation of ITU ICAO hex block assignments. It is accurate enough to distinguish US military (DoD ICAO block AE0000–AFFFFF) from civil traffic.

### Simulation Mode

`SimulationController` (ADMIN only) provides two endpoints for injecting synthetic anomalies without waiting for real ADS-B events:

- `POST /simulate/quick`: Picks a random entity that has a baseline, injects a flight event with altitude +12,000 ft and speed +250 kts above baseline. Guarantees a score of 1.0.
- `POST /simulate/custom`: Accepts explicit altitude, speed, heading, lat, and lon. Uses the entity's baseline values as fallback for unspecified fields.

### JWT / RBAC

Auth is handled by `JwtService` (JJWT 0.12.6). Tokens carry the username and role as claims, expire after 24 hours, and are validated on every request by `JwtAuthFilter`. Three roles:

- **ANALYST**: Read access to entities, events, anomalies, positions
- **OPERATOR**: Everything ANALYST can do, plus creating flight events (which triggers scoring)
- **ADMIN**: Full access including user registration, entity creation, and simulation endpoints

### Flyway Migrations

Two migrations in `src/main/resources/db/migration/`:

- `V1__init_schema.sql`: Creates `entities`, `events`, `baselines`, `anomalies`, and `users` tables with appropriate FKs and constraints.
- `V2__fix_explanation_text.sql`: Widens `anomalies.explanation` from `VARCHAR(255)` to `TEXT` to fit LLM responses.

### CI/CD

`.github/workflows/deploy.yml` runs on push to `main` and on manual dispatch. It:

1. Builds the backend JAR and pushes `abelprasad/sentinel-backend:latest` to Docker Hub.
2. Clones `abelprasad/sentinel-ui` and pushes `abelprasad/sentinel-ui:latest` to Docker Hub.
3. Connects to the remote host (`indra`) over Tailscale.
4. SSHes in and runs `docker compose pull && docker compose up -d --force-recreate`, then prunes old images.

---

## Public Endpoint

`GET /public/status` (no auth required) returns a live system snapshot:

```json
{
  "activeTracksNow": 12,
  "anomaliesLastHour": 3,
  "totalEntities": 148,
  "recentAnomalies": [...],
  "positions": [...]
}
```

`recentAnomalies` is the top 10 most recent flagged events with callsign, ICAO hex, classification, score, LLM explanation, and timestamp. `positions` is the set of aircraft with events in the last 5 minutes, annotated with whether they are currently anomalous and their most recent score.

This endpoint drives the frontend dashboard at `sentinel.abelprasad.dev/public`.

---

## Defense / ABI Relevance

ABI (Activity Based Intelligence) is the discipline of tracking entities over time to build behavioral patterns, then flagging deviations from those patterns. That is exactly what Sentinel does at the aircraft level: accumulate a kinematic baseline per ICAO hex, score each new observation against it, and surface the outliers with a human-readable explanation.

The ICAO hex classification gives a coarse but meaningful layer of entity typing (military vs. civil, US vs. foreign). In a real ISR context, you would layer in additional data (squawk codes, flight plans, radar cross-section, loiter patterns) and feed the scores into an analyst workflow. Sentinel is a stripped-down but architecturally faithful prototype of that kind of system.

---

## Local Dev Setup

### Prerequisites

- Java 21
- Maven 3.9+
- Docker and Docker Compose
- A Groq API key (free tier works)

### 1. Start PostgreSQL

```bash
docker compose up db -d
```

This brings up `postgres:16` on port 5432 with database `sentinel`, user `sentinel`, password `sentinel`.

### 2. Configure Secrets

Create `src/main/resources/application-secrets.yaml`:

```yaml
llm:
  api-key: your-groq-api-key-here
```

The main `application.yaml` imports this file. Do not commit it.

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

Flyway will run migrations on startup. The API is available on port 8080.

### 4. Register a User

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"yourpassword","role":"ADMIN"}'
```

Then login to get a JWT:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"yourpassword"}'
```

### 5. Check Public Status (No Auth)

```bash
curl http://localhost:8080/public/status
```

### 6. Run a Simulation (Requires ADMIN JWT)

```bash
curl -X POST http://localhost:8080/simulate/quick \
  -H "Authorization: Bearer <your-token>"
```

### 7. Full Stack with Docker Compose

```bash
docker compose up -d
```

Backend is on port 8888, frontend on port 3000. Set `LLM_API_KEY` as an environment variable or in a `.env` file before running.

---

## Project Structure

```
src/main/java/com/abel/sentinel/
├── config/          # Jackson configuration
├── controller/      # AuthController, AircraftEntityController, FlightEventController,
│                    # AnomalyController, PositionController, SimulationController,
│                    # PublicController, HealthController
├── dto/             # PositionDTO, PublicStatusDTO
├── model/           # User, AircraftEntity, FlightEvent, Baseline, AnomalyScore, AdsbAircraft
├── repository/      # 5 Spring Data JPA repositories
├── security/        # JwtService, JwtAuthFilter, SecurityConfig
└── service/         # AdsbIngestionService, BaselineService, AnomalyScoreService,
                     # GroqLlmService, IcaoClassificationService, SimulationService,
                     # PruneService, PositionService, AircraftEntityService,
                     # FlightEventService, JwtService
```

---

## Notes

- The ADS-B feed is public and unauthenticated. The adsb.fi endpoint used covers Philadelphia airspace. Change the lat/lon/dist parameters in `application.yaml` to monitor a different area.
- The JWT secret in `application.yaml` is a placeholder. Replace it with a proper secret in production.
- `application-secrets.yaml` is gitignored. Never commit your Groq API key.
- H2 is on the test classpath only; all integration tests run against it. Production always uses PostgreSQL.
