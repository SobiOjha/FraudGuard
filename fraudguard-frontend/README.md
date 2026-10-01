# React + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.

## Secure Frontend-to-Sentinel BFF

The dashboard is a React/Vite client served by a small Node.js backend-for-frontend
(BFF). The browser never receives Sentinel's `X-API-Key`.

```text
Browser -- HttpOnly session cookie --> Node BFF -- X-API-Key --> Sentinel API
```

The BFF serves the built `dist` directory and exposes only these protected routes:

- `GET /bff/transactions`
- `GET /bff/transactions/dashboard/stats`
- `POST /bff/transactions/analyze`
- `PUT /bff/transactions/:id/fraud-action`

Dashboard login uses `POST /bff/auth/login` and a short-lived in-memory session
cookie. The cookie is `HttpOnly`, `SameSite=Strict`, and `Secure` in production.
The session store is intentionally single-instance for this dashboard; use a
shared session store before horizontally scaling the BFF.

The BFF requires these server-only environment variables:

```text
SENTINEL_BACKEND_URL=https://sentinel-vp1i.onrender.com
SENTINEL_API_KEY=<server-only-integration-key>
BFF_USERNAME=<dashboard-username>
BFF_PASSWORD=<dashboard-password>
```

Do not prefix these variables with `VITE_`. Do not place them in React source,
browser storage, or frontend responses.

### Local development

Run the BFF on port 3000 with the variables above, then run Vite on port 5173:

```powershell
$env:SENTINEL_BACKEND_URL="http://localhost:8080"
$env:SENTINEL_API_KEY="<local-integration-key>"
$env:BFF_USERNAME="dashboard-admin"
$env:BFF_PASSWORD="<local-dashboard-password>"
npm run start
```

In a second terminal:

```powershell
npm run dev
```

Vite proxies `/bff` to the local BFF during development. In production, Render
should deploy this project as a Node Web Service with:

- Build command: `npm ci && npm run build`
- Start command: `npm start`
- Render-provided `PORT`
- The four server-only variables above configured in the service environment

The BFF does not proxy `/api/integrations/**`, `/api/auth/**`, arbitrary paths,
or arbitrary HTTP methods. Sentinel's API-key authentication, transaction
ownership, and integration isolation remain enforced by the Spring Boot backend.
