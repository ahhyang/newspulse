# API notes

Interactive docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Public

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/auth/login` | `{ "username", "password" }` → JWT |
| GET | `/api/topics` | Seeded with **AI Industry** |
| GET | `/api/articles` | Filters: `topicId`, `source`, `sentiment`, `from`, `to`, paging |
| GET | `/api/digests/latest` | 404 until digest generation ships |
| GET | `/api/digests/{date}` | Requires `topicId` |
| GET | `/api/stats` | Sentiment series (empty until enrichment) |
| GET | `/actuator/health` | Liveness/readiness |

## Admin (Bearer JWT)

| Method | Path |
| --- | --- |
| POST | `/api/topics` |
| PATCH | `/api/topics/{id}` |

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
