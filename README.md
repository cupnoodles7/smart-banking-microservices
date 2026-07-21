# Smart Banking & Wallet System — Microservices MVP

The microservices track of the Smart Banking & Wallet System
(see `Smart_Banking_System_Final_PRD.docx`).

- **Platform services** (`config-server`, `eureka-server`, `api-gateway`) — fully implemented.
- **Business services** (`auth-service`, `user-service`, `account-service`, `wallet-service`,
  `transaction-service`) — CRUD, JWT auth, and cross-service calls implemented.

## Stack

Java 21 · Spring Boot 3.3.5 · Spring Cloud 2023.0.3 · Maven · MongoDB (one DB per service) · JWT (jjwt 0.12).

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

## Running it

**Prerequisites:** JDK 21, Maven, and MongoDB on `localhost:27017`.

Start and stop the whole stack (config → eureka → business services + gateway, each gated on
a health check):

```
./run-all.sh      # start everything;  logs → .run/logs/<service>.log
./stop-all.sh     # stop everything
```

Or run a single service manually: `cd <service> && mvn spring-boot:run`
(start `config-server` and `eureka-server` first).

## Configuration model

- The **Config Server** serves the YAML in `config-repo/` to the business services. Point it
  at a Git repo via `CONFIG_REPO_URI`, or run it in the `native` profile to serve the folder
  straight off disk (what `run-all.sh` does).
- `config-repo/application.yml` — shared settings (Eureka URL, the JWT secret shared between
  Auth and Gateway, token lifetimes, actuator).
- `config-repo/<service>.yml` — each service's port and MongoDB URI.
- Business services import config via `spring.config.import=optional:configserver:...` and keep
  local fallbacks in their own `application.yml` for offline dev.

## Authentication & identity

- Public routes (no token): `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`.
  Every other route through the Gateway requires a valid `Authorization: Bearer <token>` JWT.
- The Gateway validates the JWT and injects identity headers downstream —
  `X-Customer-Id`, `X-Auth-Username`, `X-Auth-Roles`. Its signing secret must match the Auth
  Service's (shared via `config-repo/application.yml` / the `JWT_SECRET` env var).
- **Calling a service directly** (bypassing the Gateway, e.g. on its own port) means you must
  send those identity headers yourself — the tables below note which header each route needs.
- `*/internal` routes are service-to-service only (not exposed through the Gateway) and are
  guarded by the `X-Internal-Api-Key` header.

## API endpoints

Base paths are shown per service. Through the Gateway (`:8080`) the paths are identical
(e.g. `GET :8080/accounts/customer/{id}`); the Gateway supplies `X-Customer-Id` from the JWT.

### Auth Service — `:8081`

| Method | Path             | Auth / headers            | Body / notes |
|--------|------------------|---------------------------|--------------|
| POST   | `/auth/register` | public                    | `{username, email, password, fullName, phoneNumber, address{line1,city,state,pincode}}` → **201** `{customerId, username, email, roles}`. Also creates the customer profile in User Service. |
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
| POST   | `/users/internal`       | `X-Internal-Api-Key`                    | `{id, fullName, email, phoneNumber, address}` → **201**. `email` and `phoneNumber` are unique. Service-to-service. |
| DELETE | `/users/internal/{id}`  | `X-Internal-Api-Key`                    | → **204**, idempotent. Compensating delete. Service-to-service. |

### Account Service — `:8083`  (all routes need `X-Customer-Id`)

| Method | Path                          | Body / notes |
|--------|-------------------------------|--------------|
| POST   | `/accounts`                   | `{accountType: SAVINGS\|CURRENT}` → **201** account |
| POST   | `/accounts/deposit`           | `{accountId, amount}` → **200** updated account |
| POST   | `/accounts/withdraw`          | `{accountId, amount}` → **200** updated account |
| POST   | `/accounts/transfer`          | `{fromAccountId, toAccountId, amount}` → **200** |
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

## Postman

`postman/Smart-Banking.postman_collection.json` covers every endpoint above plus the Gateway
end-to-end flow. Import it and use **Run collection** (Collection Runner) to execute the folders
top-to-bottom — Register/Login capture the token and IDs into collection variables that later
requests reuse, so run it in order (or with **No Environment** selected) rather than sending the
dependent requests individually.
