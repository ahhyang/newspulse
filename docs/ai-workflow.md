# How I used AI tools

This project is a portfolio piece for a Java/Spring Boot role. Cursor (Composer) was used as a pair-programmer, not as an unsupervised code generator.

## Where Cursor / Claude helped

- Bootstrapped the Spring Boot 4.1 Maven layout, Dockerfiles, Compose wiring, and GitHub Actions workflow.
- Drafted repetitive layers: JPA entities matching the Flyway schema, request/response records, and the `@ControllerAdvice` error envelope.
- Generated first-pass unit tests for `TopicService` and `AuthService`, which I then tightened around duplicate-name and JWT claims behavior.
- Drafted the Vite + Tailwind dashboard shell (routes, API client, Recharts wiring), which I then restyled as an editorial briefing rather than a generic admin template.
- Wrote the nginx reverse-proxy config so Compose can serve the SPA and `/api` on one origin.

## Decisions I made myself

- Domain model: topics → articles → enrichments → clusters → digests (with URL-hash uniqueness, not title matching).
- `NewsSource` / `LlmClient` ports so GNews, Hacker News, and OpenRouter can be swapped without touching digest logic.
- PostgreSQL + Neon instead of MySQL, because the live database target is Neon (Postgres). Local Compose uses the same dialect.
- JWT for admin writes, public GETs for the dashboard — a news briefing should be readable without a login wall.
- Secrets only in `.env` / host env vars. Keys that were ever pasted into chat should be rotated.
- Vercel for the React SPA only. The API stays on a JVM host; that constraint is documented rather than papered over.
- Title Jaccard (0.45) plus content-hash union-find for “N sources covered this”, instead of calling the LLM a second time for clustering.

## What I did not outsource

- Schema design and Flyway versioning
- Security filter chain (which routes are public vs admin)
- Production config (`ddl-auto=validate`, `open-in-view=false`, virtual threads, CORS)
- Failure isolation: one GNews/OpenRouter error must not abort the rest of the batch
