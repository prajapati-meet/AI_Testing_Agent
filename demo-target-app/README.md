# Demo Target Application (`demo-target-app`)

Standalone React + Vite e-commerce web application that serves as the **target application under test** for the **AI Test Agent**.

## Configuration
- **Port**: `3000`
- **URL**: `http://localhost:3000`

## Routes
- `/` — Home page
- `/login` — Customer login page
- `/dashboard` — Customer account dashboard
- `/products` — Products catalog
- `/cart` — Shopping cart
- `/checkout` — Checkout workflow

## Demo Credentials
- **Email**: `test@example.com`
- **Password**: `password123`

## Bug Simulation (`BUG_MODE`)
- Supports `BUG_MODE=true` / `BUG_MODE=false` via environment variables or the header toggle button.
- When `BUG_MODE=true`, the login workflow intentionally fails even with valid credentials.
- When `BUG_MODE=false`, login with `test@example.com` / `password123` succeeds and redirects to `/dashboard`.

## Build & Run
```bash
# Install dependencies
npm install

# Start development server on http://localhost:3000
npm run dev

# Create production build
npm run build
```
