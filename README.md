# Smart Banking & Wallet System — Microservices MVP

The microservices track of the Smart Banking & Wallet System
(see `Smart_Banking_System_Final_PRD.docx`).

- **Platform services** (`config-server`, `eureka-server`, `api-gateway`) — fully implemented.
- **Business services** (`auth-service`, `user-service`, `account-service`, `wallet-service`,
  `transaction-service`) — CRUD, JWT auth, and cross-service calls implemented.

## Stack

Java 21 · Spring Boot 3.3.5 · Spring Cloud 2023.0.3 · Maven · MongoDB (one DB per service, run as a single-node replica set) · Docker · JWT (jjwt 0.12).

## Layout

```
bank-microservice/
├── config-repo/            # Config served by the Config Server (per-service YAML)
├── config-server/          # Spring Cloud Config Server              (:8888)
├── eureka-server/          # Netflix Eureka registry                 (:8761)
├── api-gateway/            # Spring Cloud Gateway + JWT validation    (:8080)
├── auth-service/           # register/login/refresh, JWT issue, BCrypt (:8081)
├── user-service/           # customer profile CRUD                    (:8082)
├── account-service/        # savings/current accounts, deposit/withdraw/transfer (:8083)
├── wallet-service/         # stored-value wallet: topup/transfer/pay-bill (:8084)
├── transaction-service/    # immutable transaction ledger             (:8085)
├── docker-compose.yml      # MongoDB as a single-node replica set
└── postman/                # Postman collection for the full API
```

## Ports

| Service       | Port |
|---------------|------|
| API Gateway   | 8080 |
| Auth          | 8081 |
| User          | 8082 |
| Account       | 8083 |
| Wallet        | 8084 |
| Transaction   | 8085 |
| Eureka        | 8761 |
| Config Server | 8888 |

## Running it (quickstart)

**Prerequisites:** JDK 21 (the build targets Java 21; a newer JDK works too), Maven, and Docker
(for MongoDB).

**1. Start MongoDB as a single-node replica set.** A replica set is required because
`account-service` uses a multi-document transaction for transfers — a standalone `mongod` would
reject it. The bundled compose file starts Mongo and initialises the set for you:

```
docker compose up -d      # mongo on :27017, replica set rs0 initialised automatically
```

Make sure nothing else already holds port `27017` (e.g. a Homebrew `mongod` — `brew services stop mongodb-community`).

**2. Create a `.env`** at the repo root (git-ignored). The services ship with **no** secret
defaults, so these must be provided — `run-all.sh` sources `.env`, or generates ephemeral secrets
if it is absent:

```
JWT_SECRET=$(openssl rand -hex 32)
USER_INTERNAL_API_KEY=$(openssl rand -hex 24)
ACCOUNT_DB_URI=mongodb://localhost:27017/account_db?directConnection=true
```

`ACCOUNT_DB_URI` points account-service at the replica set (`directConnection=true` lets the driver
run transactions against the single node).

**3. Start the whole stack** (config → eureka → business services + gateway, each gated on a
health check):

```
./run-all.sh      # start everything;  logs → .run/logs/<service>.log
./stop-all.sh     # stop the services  (then `docker compose down` to stop Mongo)
```

Or run a single service manually: `cd <service> && mvn spring-boot:run`
(start `config-server` and `eureka-server` first).

**4. Explore the API.** Once everything is up:
- **Swagger UI (all five services on one page):** http://localhost:8080/swagger-ui.html — click
  **Authorize**, paste a token from `POST /auth/login`, then "Try it out" routes through the Gateway.
- **Postman:** import `postman/Smart-Banking.postman_collection.json` and use **Run collection** — it
  registers a fresh user, logs in, and chains the IDs through every service. See [Postman](#postman).

## Configuration model

- The **Config Server** serves the YAML in `config-repo/` to the business services. Point it
  at a Git repo via `CONFIG_REPO_URI`, or run it in the `native` profile to serve the folder
  straight off disk (what `run-all.sh` does).
- `config-repo/application.yml` — shared settings (Eureka URL, the JWT config shared between
  Auth and Gateway, token lifetimes, actuator).
- `config-repo/<service>.yml` — each service's port and MongoDB URI.
- Business services import config via `spring.config.import=optional:configserver:...` and keep
  local fallbacks in their own `application.yml` for offline dev.

### Secrets

No secrets are committed — the configs reference these env vars with **no defaults**, so a service
fails to start if they are unset:

- `JWT_SECRET` — shared HMAC signing key for JWTs (identical on Auth + Gateway, ≥ 256 bits).
- `USER_INTERNAL_API_KEY` — shared key guarding user-service's `/users/internal` endpoints.

For local dev, `run-all.sh` loads a git-ignored `.env` if present, otherwise generates ephemeral
secrets for the run. See [Running it](#running-it-quickstart) for a ready-to-use `.env`.


## Authentication & identity

- Public routes (no token): `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`.
  Every other route through the Gateway requires a valid `Authorization: Bearer <token>` JWT.
- The Gateway accepts only **access** tokens (a refresh token is rejected), strips any
  client-supplied `X-Customer-Id` / `X-Auth-*` / `X-User-Email` / `X-Internal-Api-Key` on the way
  in, and then injects trusted identity headers from the JWT: `X-Customer-Id`, `X-Auth-Username`,
  `X-User-Email`, `X-Auth-Roles`. Its signing secret must match the Auth Service's — both read the
  `JWT_SECRET` env var (see **Secrets** above).
- **Calling a service directly** (bypassing the Gateway, e.g. on its own port) means you must
  send those identity headers yourself — the tables below note which header each route needs.
- **Internal routes.** `user-service`'s `/users/internal*` require the `X-Internal-Api-Key` header
  (value = `USER_INTERNAL_API_KEY`, compared in constant time). Because the Gateway strips that
  header, they're reachable only by direct service-to-service calls, not through the public
  Gateway. `transaction-service`'s `/transactions/internal` has no separate key — through the
  Gateway it just needs a valid JWT like any other route.

## API documentation (Swagger / OpenAPI)

Every business service exposes interactive Swagger UI. The Gateway aggregates all five into a
single page — use the **dropdown (top-right)** to switch services:

**Unified UI → http://localhost:8080/swagger-ui.html**

Click **Authorize**, paste a JWT from `POST /auth/login`, and "Try it out" routes through the
Gateway (which injects `X-Customer-Id` downstream), so authenticated calls work end-to-end.

Per-service UIs (direct, bypassing the Gateway) are also available:

| Service     | Swagger UI                             | OpenAPI JSON                       |
|-------------|----------------------------------------|------------------------------------|
| Auth        | http://localhost:8081/swagger-ui.html  | http://localhost:8081/v3/api-docs  |
| User        | http://localhost:8082/swagger-ui.html  | http://localhost:8082/v3/api-docs  |
| Account     | http://localhost:8083/swagger-ui.html  | http://localhost:8083/v3/api-docs  |
| Wallet      | http://localhost:8084/swagger-ui.html  | http://localhost:8084/v3/api-docs  |
| Transaction | http://localhost:8085/swagger-ui.html  | http://localhost:8085/v3/api-docs  |

## API endpoints

Base paths are shown per service. Through the Gateway (`:8080`) the paths are identical
(e.g. `GET :8080/accounts/customer/{id}`); the Gateway supplies `X-Customer-Id` from the JWT.

### Auth Service — `:8081`

| Method | Path             | Auth / headers            | Body / notes |
|--------|------------------|---------------------------|--------------|
| POST   | `/auth/register` | public                    | `{username, email, password, fullName, phoneNumber, address{line1,city,state,pincode}}` → **201** `{message, customerId, username, email, roles}`. Also creates the customer profile in User Service. |
| POST   | `/auth/login`    | public                    | `{usernameOrEmail, password}` → **200** `{token, refreshToken, customerId, username, email, roles, expiresIn}` |
| POST   | `/auth/refresh`  | public                    | `{refreshToken}` → **200** new access token |
| POST   | `/auth/validate` | `Authorization: Bearer`   | → `{valid, username, customerId, roles}` |
| GET    | `/auth/me`       | `X-Auth-Username`         | current user's credentials record |
| GET    | `/auth/users`    | `X-Auth-Roles: ADMIN`     | all users (admin only) |
| POST   | `/auth/logout`   | `Authorization: Bearer`   | stateless — client discards the token |

### User Service — `:8082`

| Method | Path                    | Auth / headers                          | Body / notes |
|--------|-------------------------|-----------------------------------------|--------------|
| GET    | `/users/{id}`           | `X-Customer-Id` (must equal `{id}`)     | own profile only |
| PUT    | `/users/{id}`           | `X-Customer-Id` (must equal `{id}`)     | `{fullName, email, phoneNumber, address}` |
| POST   | `/users/internal`       | `X-Internal-Api-Key` (value = `USER_INTERNAL_API_KEY`) | `{id, fullName, email, phoneNumber, address}` → **201**. `email` and `phoneNumber` are unique. Service-to-service. |
| DELETE | `/users/internal/{id}`  | `X-Internal-Api-Key`                    | → **204**, idempotent. Compensating delete. Service-to-service. |

### Account Service — `:8083`  (all routes need `X-Customer-Id`)

| Method | Path                          | Body / notes |
|--------|-------------------------------|--------------|
| POST   | `/accounts`                   | `{accountType: SAVINGS\|CURRENT}` → **201** account |
| POST   | `/accounts/deposit`           | `{accountId, amount, [idempotencyKey]}` → **200** updated account. Optional `idempotencyKey` makes the call replay-safe. |
| POST   | `/accounts/withdraw`          | `{accountId, amount, [idempotencyKey]}` → **200** updated account. Optional `idempotencyKey` makes the call replay-safe. |
| POST   | `/accounts/transfer`          | `{fromAccountId, toAccountId, amount}` → **200** (atomic — runs as a single MongoDB multi-document transaction) |
| GET    | `/accounts/customer/{customerId}` | list the caller's accounts |

### Wallet Service — `:8084`  (all routes need `X-Customer-Id`)

Money-moving routes return **HTTP 200** with a `TransactionResult` whose `status` is
`SUCCESS` or `FAILED` — a business-rule failure (e.g. insufficient balance) is *not* an HTTP error.

| Method | Path                          | Body / notes |
|--------|-------------------------------|--------------|
| POST   | `/wallets`                    | `{linkedAccountId, walletType: PAYTM\|PHONEPE\|CUSTOM}` → **201** wallet |
| POST   | `/wallets/topup`              | `{walletId, amount, idempotencyKey}` → debits the linked account, credits the wallet |
| POST   | `/wallets/transfer`           | `{fromWalletId, toWalletId, amount, idempotencyKey}` |
| POST   | `/wallets/pay-bill`           | `{walletId, billerId, amount, idempotencyKey}` |
| GET    | `/wallets/customer/{customerId}?page=0&size=20` | paged list of the caller's wallets |

### Transaction Service — `:8085`

Immutable ledger. `POST /internal` is service-to-service (idempotent on `idempotencyKey`);
reads are paged, newest first.

| Method | Path                                   | Body / notes |
|--------|----------------------------------------|--------------|
| POST   | `/transactions/internal`               | `{customerId, transactionType, senderType, senderId, receiverType, receiverId, amount, status, failureReason, idempotencyKey, [initiatedAt, completedAt]}` → **201** |
| GET    | `/transactions/{id}`                   | one transaction |
| GET    | `/transactions/account/{accountId}?page&size`   | transactions touching an account |
| GET    | `/transactions/wallet/{walletId}?page&size`     | transactions touching a wallet |
| GET    | `/transactions/customer/{customerId}?page&size` | a customer's transactions |

### Enum values

| Enum              | Values |
|-------------------|--------|
| `accountType`     | `SAVINGS`, `CURRENT` |
| `walletType`      | `PAYTM`, `PHONEPE`, `CUSTOM` |
| `transactionType` | `DEPOSIT`, `WITHDRAW`, `TRANSFER`, `WALLET_TOPUP`, `WALLET_TRANSFER`, `BILL_PAYMENT` |
| `senderType`      | `ACCOUNT`, `WALLET` |
| `receiverType`    | `ACCOUNT`, `WALLET`, `MERCHANT` |
| `status`          | `SUCCESS`, `FAILED` |
| `failureReason`   | `NONE`, `INVALID_AMOUNT`, `INVALID_ACCOUNT`, `INVALID_WALLET`, `INSUFFICIENT_BALANCE`, `DAILY_LIMIT_EXCEEDED`, `WALLET_LIMIT_EXCEEDED`, `SELF_TRANSFER` |

## Observability (Actuator)

Every service (platform + business) exposes Spring Boot Actuator with `micrometer-registry-prometheus`.
The exposed set is deliberately limited to three endpoints:

| Endpoint | Path | Notes |
|----------|------|-------|
| Health | `/actuator/health` | `show-details: always`; returns `{"status":"UP", ...}` |
| Info | `/actuator/info` | build/app info |
| Prometheus | `/actuator/prometheus` | scrape target, tagged `application=<service-name>` |

Examples (each service on its own port):
```
curl http://localhost:8081/actuator/health      # auth
curl http://localhost:8084/actuator/prometheus   # wallet
```
Ports: config `8888`, eureka `8761`, gateway `8080`, auth `8081`, user `8082`, account `8083`,
wallet `8084`, transaction `8085`.

> **`prometheus` is gated on config-server.** The `health,info,prometheus` exposure lives in
> `config-repo/<service>.yml`; each service's local fallback `application.yml` exposes only
> `health,info`. So if a business service boots while config-server is down, `/actuator/prometheus`
> won't be present until config-server is reachable.

## Postman

`postman/Smart-Banking.postman_collection.json` covers every endpoint above plus the Gateway
end-to-end flow and an **Actuator** folder (health / info / prometheus for all 8 services). Import
it and use **Run collection** (Collection Runner) to execute the folders top-to-bottom —
Register/Login capture the token and IDs into collection variables that later requests reuse, so
run it in order (or with **No Environment** selected) rather than sending the dependent requests
individually. The Actuator folder is independent and can be run on its own.

Two notes: Mongo must be a single-node replica set (see [Running it](#running-it-quickstart)) or the
Account **Transfer** request 500s; and to run the two **User → *(internal)*** requests directly, set
the `internalUserApiKey` collection variable to your `USER_INTERNAL_API_KEY` value.
