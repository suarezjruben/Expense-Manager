# Expense Manager

Expense Manager is a full-stack personal budgeting app for tracking monthly income and expenses, planning category budgets, and comparing planned vs actual results in a single dashboard.

## Features

- Monthly dashboard with starting balance, net change, and ending balance
- Planned vs actual totals for income and expenses
- Category-level budget planning (income and expense)
- Transaction tracking by month and type (income/expense)
- Category management (create, edit, activate/deactivate, delete)

## Tech Stack

- Backend: Java 21, Spring Boot 4, Spring Data JPA, Bean Validation
- Database: PostgreSQL 16
- Frontend: Angular 19, RxJS, SCSS
- Local infrastructure: Docker Compose

## Repository Structure

```text
.
|-- src/                  # Spring Boot API
|-- ui/                   # Angular frontend
|-- infra/postgres/       # Local PostgreSQL docker-compose
|-- pom.xml               # Backend build config
|-- mvnw / mvnw.cmd       # Maven wrappers
```

## Quick Start

### Prerequisites

- Java 21
- Node.js + npm
- Docker (for local PostgreSQL)

### 1. Start PostgreSQL

```bash
cd infra/postgres
docker compose up -d
```

Default DB config from `src/main/resources/application.properties`:

- Host: `localhost`
- Port: `5432`
- Database: `expenses`
- Username: `user`
- Password: `password`

### 2. Run the backend API

From the repository root:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend runs at `http://localhost:9095`.

### 3. Run the frontend

```bash
cd ui
npm install
npm start
```

Frontend runs at `http://localhost:4200`.

The dev server proxies `/api` requests to `http://localhost:9095` via `ui/proxy.conf.json`.

## API Overview

Base path: `/api`

Month route parameters use the format `yyyy-MM` (example: `2026-02`).

- `GET /api/categories?type=EXPENSE|INCOME`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`
- `GET /api/accounts?includeInactive=false`
- `POST /api/accounts`
- `POST /api/accounts/{accountId}/statement-imports` (multipart `file` for `.csv`, `.ofx`, `.qfx`)
  - If CSV has no header row and no saved mapping exists for that account, response status is `HEADER_MAPPING_REQUIRED`.
  - Then resend with mapping fields: `dateColumnIndex`, `amountColumnIndex`, `descriptionColumnIndex`, optional `categoryColumnIndex`, optional `externalIdColumnIndex`, and `saveHeaderMapping=true` to persist per account.
- `POST /api/accounts/{accountId}/plaid/link-token`
- `POST /api/accounts/{accountId}/plaid/exchange`
- `GET /api/accounts/{accountId}/plaid/connections`
- `POST /api/accounts/{accountId}/plaid/connections/{connectionId}/sync`
- `GET /api/months/{yearMonth}/settings`
- `PUT /api/months/{yearMonth}/settings`
- `GET /api/months/{yearMonth}/summary`
- `GET /api/months/{yearMonth}/plans?type=EXPENSE|INCOME`
- `PUT /api/months/{yearMonth}/plans?type=EXPENSE|INCOME`
- `GET /api/months/{yearMonth}/transactions?type=EXPENSE|INCOME&accountId={id}`
- `POST /api/months/{yearMonth}/transactions?type=EXPENSE|INCOME&accountId={id}`
- `PUT /api/months/{yearMonth}/transactions/{id}?type=EXPENSE|INCOME&accountId={id}`
- `DELETE /api/months/{yearMonth}/transactions/{id}?type=EXPENSE|INCOME&accountId={id}`

## Example Requests

Create an expense transaction:

```bash
curl -X POST "http://localhost:9095/api/months/2026-02/transactions?type=EXPENSE" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2026-02-15",
    "amount": 42.50,
    "description": "Groceries",
    "categoryId": 1
  }'
```

Update month starting balance:

```bash
curl -X PUT "http://localhost:9095/api/months/2026-02/settings" \
  -H "Content-Type: application/json" \
  -d '{
    "startingBalance": 1000
  }'
```

## Development Commands

Backend:

- Run tests: `./mvnw test`
- Build jar: `./mvnw clean package`

Frontend (from `ui/`):

- Start dev server: `npm start`
- Build: `npm run build`
- Run tests: `npm test`

## Notes

- CORS is configured to allow `http://localhost:4200` for `/api/**`.
- JPA schema mode is `update` for local development.
- `target/` and `ui/dist/` build outputs are generated artifacts.

## Plaid Setup

Plaid integration is disabled by default. Configure these properties in `src/main/resources/application.properties` or environment-specific overrides:

- `plaid.enabled=true`
- `plaid.base-url=https://sandbox.plaid.com` (or development/production)
- `plaid.client-id=...`
- `plaid.secret=...`
- Optional: `plaid.country-codes=US`, `plaid.products=transactions`, `plaid.sync-page-size=100`
- Usage warning controls:
  - `plaid.usage.free-monthly-call-limit=200`
  - `plaid.usage.warning-threshold-percent=80`

`/plaid/connections/{connectionId}/sync` tracks monthly Plaid Transactions API calls and returns a usage payload with warning/exhausted flags when usage approaches or reaches the configured free-tier limit.
