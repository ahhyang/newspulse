# API notes

Interactive docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Public

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/auth/login` | `{ "username", "password" }` → JWT |
| GET | `/api/topics` | Seeded with **AI Industry** |
| GET | `/api/articles` | Filters: `topicId`, `source`, `sourceName`, `clusterId`, `sentiment`, `q` (title search), `from`, `to`, paging |
| GET | `/api/digests/latest` | Most recent digest. Optional `topicId` (defaults to first active topic for date lookup on `/{date}`) |
| GET | `/api/digests/{date}` | ISO date in UTC. Optional `topicId` |
| GET | `/api/stats` | Daily sentiment series. Optional `topicId`, `from`, `to` (default: last 7 UTC days through today) |
| GET | `/actuator/health` | Liveness/readiness |

Admin ingest: `POST /api/ingestion/runs` with a Bearer JWT. The scheduler also runs on `app.ingestion.interval-ms` (default 1 hour) and never crashes the process on GNews or Hacker News errors.

## Admin (Bearer JWT)

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/topics` | Create a tracked topic |
| PATCH | `/api/topics/{id}` | Update a topic |
| POST | `/api/ingestion/runs` | Pull GNews + Hacker News now |
| POST | `/api/enrichment/runs` | Summarize + classify unenriched articles |
| POST | `/api/digests/runs` | Generate/regenerate digests for a UTC date (`date`, optional `topicId`) |

Errors use a single envelope:

```json
{
  "timestamp": "2026-08-19T00:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Topic 9 was not found",
  "path": "/api/topics/9",
  "details": []
}
```
