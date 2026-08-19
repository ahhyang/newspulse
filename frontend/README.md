# NewsPulse dashboard

React + TypeScript + Vite + Tailwind SPA. It talks to the Spring Boot API over REST only.

```bash
cp .env.example .env
npm install
npm run dev
```

Local default: Vite proxies `/api` to `http://localhost:8080`. Start the API first (`docker compose up` from the repo root, or `mvn spring-boot:run` in `backend/`).

Dashboard: http://localhost:5173

For Vercel, set `VITE_API_URL` to the public API origin (Railway/Render). Spring Boot does not run on Vercel.

```bash
npm test
npm run build
```
