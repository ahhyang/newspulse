# Changelog

All notable changes to NewsPulse are documented here.

## [Unreleased]

### Added
- Phase 1 backend scaffold: Spring Boot API, Flyway schema, JWT auth, empty REST resources, Testcontainers migration test.
- Phase 2 GNews ingestion: `NewsSource` adapter, URL-hash dedupe, hourly scheduler, `POST /api/ingestion/runs`.
- Phase 3 LLM enrichment: OpenRouter adapter, summary/sentiment/stance, retry/backoff, `POST /api/enrichment/runs`.
- Phase 4 digest generation: Jaccard title clustering, daily UTC digest (scheduler + `POST /api/digests/runs`), real `GET /api/stats` sentiment series.
- Phase 5 React dashboard: Vite + TypeScript + Tailwind, digest/articles/admin views, Recharts sentiment trend.
