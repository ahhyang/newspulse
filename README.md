# NewsPulse

AI-native news aggregation and sentiment briefing platform. Java / Spring Boot API with a React dashboard.

[![CI](https://github.com/ahhyang/newspulse/actions/workflows/ci.yml/badge.svg)](https://github.com/ahhyang/newspulse/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen)
![Postgres](https://img.shields.io/badge/PostgreSQL-16%20%2F%20Neon-blue)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

NewsPulse pulls articles about a tracked topic (default: **AI industry**), stores them, enriches them with an LLM (summary, sentiment, stance), clusters near-duplicate coverage, and produces a daily digest.

> **Status:** Phase 6 — `docker compose up --build` runs Postgres, the API, and the dashboard. README/CI polish is next.

## Architecture

```mermaid
flowchart LR
  Dashboard[React dashboard / Vercel] --> API[Spring Boot API]
  API --> PG[(PostgreSQL / Neon)]
  API --> GNews[GNews]
  API --> OpenRouter[OpenRouter / Claude]
```

More detail: [docs/architecture.md](docs/architecture.md)

## Stack

| Layer | Choice | Why |
| --- | --- | --- |
| API | Java 21, Spring Boot 4.1 | Current Spring generation (3.x OSS ended mid-2026). Job listings still say “3.x”; this repo tracks the live platform. |
| HTTP | Spring Web MVC, OpenAPI/Swagger | REST-only contract for the SPA |
| Persistence | Spring Data JPA + Flyway | Versioned schema; Hibernate `ddl-auto=validate` only |
| Database | PostgreSQL 16 locally, Neon in prod | Neon is Postgres. Same dialect in Compose and cloud. |
| Auth | JWT (admin writes) | Public read APIs for the dashboard |
| News | GNews behind `NewsSource` | First live source; RSS can be added without touching persist/digest logic |
| LLM | OpenRouter (`LlmClient`) | Claude (or other) models without locking the code to one vendor SDK |
| Frontend | React + TypeScript + Vite + Tailwind | REST-only SPA; deploy to Vercel |
| Packaging | Docker Compose | `db` + `api` + `frontend` (nginx reverse-proxy to the API) |

## API

| Method | Path | Auth |
| --- | --- | --- |
| `POST` | `/api/auth/login` | public |
| `GET` | `/api/topics` | public |
| `POST` | `/api/topics` | JWT admin |
| `POST` | `/api/ingestion/runs` | JWT admin |
| `POST` | `/api/enrichment/runs` | JWT admin |
| `POST` | `/api/digests/runs` | JWT admin |
| `GET` | `/api/articles` | public, filterable |
| `GET` | `/api/digests/latest` | public |
| `GET` | `/api/digests/{date}` | public, optional `topicId` |
| `GET` | `/api/stats` | public, optional `topicId`/`from`/`to` |
| `GET` | `/swagger-ui.html` | public |

Error body is consistent (`status`, `error`, `message`, `path`, `details`). See [docs/api.md](docs/api.md).

## Local setup

**Prereqs:** Docker Desktop. JDK 21 / Node 20+ are only needed if you run services outside Compose.

```bash
cp .env.example .env
# fill GNEWS_API_KEY, OPENROUTER_API_KEY, JWT_SECRET, APP_ADMIN_PASSWORD
docker compose up --build
```

| Service | URL |
| --- | --- |
| Dashboard | http://localhost |
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html (also via http://localhost/swagger-ui.html) |
| Health | http://localhost:8080/actuator/health |

The nginx frontend proxies `/api` to the API container, so the browser stays same-origin. Digest, articles, and stats are public; open **Admin** and sign in with `APP_ADMIN_*` to run ingest/enrich/digest.

If port 80 is already taken, change the frontend host port in `docker-compose.yml` (`"8088:80"`) and open that instead.

### Frontend hot-reload (optional)

Leave Compose running for `db` + `api`, then:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Vite on http://localhost:5173 proxies `/api` to `:8080`.

Login (values from your `.env`):

```bash
curl -s http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"YOUR_PASSWORD\"}"
```

After login, you can run the pipeline on demand:

```bash
curl -s -X POST http://localhost:8080/api/ingestion/runs -H "Authorization: Bearer TOKEN"
curl -s -X POST http://localhost:8080/api/enrichment/runs -H "Authorization: Bearer TOKEN"
curl -s -X POST http://localhost:8080/api/digests/runs -H "Authorization: Bearer TOKEN"
curl -s http://localhost:8080/api/digests/latest
curl -s http://localhost:8080/api/stats
```

Run tests (Docker required for Testcontainers):

```bash
cd backend
./mvnw -B verify
cd ../frontend
npm test
npm run build
```

### Neon

Create a Postgres database in [Neon](https://neon.tech), then set:

```env
DATABASE_URL=postgresql://USER:PASSWORD@HOST/dbname?sslmode=require
SPRING_PROFILES_ACTIVE=prod
```

The API accepts Neon-style `postgres://` URLs and maps them to JDBC. Do not also set `SPRING_DATASOURCE_URL` if you want `DATABASE_URL` to win.

### Vercel

Vercel hosts the **frontend SPA**, not the JVM API. Set `VITE_API_URL` to the deployed Spring Boot origin (Railway or Render). Putting the JAR on Vercel is not supported.

## Secrets

Keys live in `.env` (gitignored). `.env.example` is the template. If a key was ever pasted into chat, email, or a screenshot, **rotate it** in GNews and OpenRouter and update `.env`.

## Future improvements (called out on purpose)

- Redis cache for digest payloads
- Durable queue (Kafka or at least an outbox) for the ingest → enrich pipeline
- SMTP daily digest
- Additional `NewsSource` adapters (RSS, GNews already planned as first)

## How I used AI tools

See [docs/ai-workflow.md](docs/ai-workflow.md) — concrete split between scaffolding help and design decisions I own.

## License

MIT
