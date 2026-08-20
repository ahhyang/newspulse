# Deploy NewsPulse (production)

NewsPulse splits across three hosts:

| Piece | Host | Notes |
| --- | --- | --- |
| Frontend | [Vercel](https://vercel.com) | React SPA from repo root (`vercel.json` builds `frontend/`) |
| API | [Railway](https://railway.app) or [Render](https://render.com) | Spring Boot JAR via `backend/Dockerfile` |
| Database | [Neon](https://neon.tech) | Postgres 16 — set `DATABASE_URL` on the API host |

## 1. Neon (database)

1. Create a project at [neon.tech](https://neon.tech).
2. Copy the **pooled** connection string (`postgres://…?sslmode=require`).
3. Keep it for the API step — do not commit it.

## 2. API (Railway)

1. [New project → Deploy from GitHub repo](https://railway.app/new) → `ahhyang/newspulse`.
2. Set **Root Directory** to `backend`.
3. Railway detects `backend/Dockerfile` and `railway.toml`.
4. Add variables (Settings → Variables):

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | Neon connection string |
| `JWT_SECRET` | long random string (32+ chars) |
| `APP_ADMIN_USERNAME` | `admin` |
| `APP_ADMIN_PASSWORD` | strong password |
| `GNEWS_API_KEY` | from [gnews.io](https://gnews.io) |
| `HN_ENABLED` | `true` (Algolia search, no API key) |
| `HN_MAX_RESULTS` | `20` |
| `OPENROUTER_API_KEY` | from [openrouter.ai](https://openrouter.ai) |
| `CORS_ALLOWED_ORIGINS` | your Vercel URL, e.g. `https://newspulse.vercel.app` |

5. Deploy and copy the public URL (e.g. `https://newspulse-api.up.railway.app`).
6. Smoke test: `GET https://<api>/actuator/health` → `{"status":"UP"}`.

**Render alternative:** use the repo’s `render.yaml` blueprint instead of Railway.

## 3. Frontend (Vercel)

1. Import [github.com/ahhyang/newspulse](https://github.com/ahhyang/newspulse) at [vercel.com/new](https://vercel.com/new).
2. Leave **Root Directory** empty — root `vercel.json` builds `frontend/`.
3. Environment variable:

| Name | Value |
| --- | --- |
| `VITE_API_URL` | Railway/Render API origin (no trailing slash) |

4. Deploy. Open the site — Digest and Articles should load public data.
5. Admin tab: sign in with `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD`, then run ingest → enrich → digest.

## 4. After first deploy

1. Update `CORS_ALLOWED_ORIGINS` on the API if the Vercel URL changed.
2. Redeploy the API if you only added CORS after the first frontend deploy.
3. Trigger a pipeline run from the Admin page so the dashboard has data.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| Vercel shows `404: NOT_FOUND` | Redeploy from latest `main` (root `vercel.json` must build `frontend/dist`). |
| Dashboard loads but API errors | Set `VITE_API_URL` on Vercel and redeploy. |
| Browser CORS error | Add the exact Vercel origin to `CORS_ALLOWED_ORIGINS` on the API. |
| API crash on boot | Check `DATABASE_URL` format and that Flyway can reach Neon (`sslmode=require`). |
