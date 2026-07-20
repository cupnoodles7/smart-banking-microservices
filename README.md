# Smart Banking & Wallet System — Track B (Microservices MVP)

The microservices track of the Smart Banking & Wallet System
(see `Smart_Banking_System_Final_PRD.docx`).

- **Platform services** (`config-server`, `eureka-server`, `api-gateway`) are fully implemented.
- **Business services** (`auth-service`, `user-service`, `account-service`, `wallet-service`,
  `transaction-service`) are implemented (CRUD, JWT auth, cross-service calls) per the
  per-service package layout in PRD §6.11.

## Stack

Java 21 · Spring Boot 3.3.5 · Spring Cloud 2023.0.3 · Maven · MongoDB (one DB per service) · JWT (jjwt 0.12).

## Layout

```
bank-microservice/
├── config-repo/            # Git-backed config served by the Config Server (per-service YAML)
├── config-server/          # FULL  — Spring Cloud Config Server            (:8888)
├── eureka-server/          # FULL  — Netflix Eureka registry               (:8761)
├── api-gateway/            # FULL  — Spring Cloud Gateway + JWT validation  (:8080)
├── auth-service/           # register/login/refresh, JWT issue, BCrypt      (:8081)
├── user-service/           # customer profile CRUD                          (:8082)
├── account-service/        # savings/current accounts, deposit/withdraw/xfer (:8083)
├── wallet-service/         # stored-value wallet: topup/transfer/pay-bill   (:8084)
└── transaction-service/    # immutable transaction ledger                   (:8085)
```

Each business service follows PRD §6.11:

```
controller/  service/  service/impl/  repository/  entity/
dto/request/  dto/response/  exception/  security/  client/  mapper/  config/  util/  constants/
```

(Empty folders are kept in version control with `.gitkeep`.)

## Configuration model

- The **Config Server** is Git-backed. It serves the YAML in `config-repo/` to the
  business services. Point it at your Git repo via the `CONFIG_REPO_URI` env var:
  - Push `config-repo/` to a remote: `export CONFIG_REPO_URI=https://github.com/<you>/bank-config-repo.git`
  - …or use it as a local Git repo:
    ```
    cd config-repo && git init && git add . && git commit -m "config" && cd -
    export CONFIG_REPO_URI="file://$(pwd)/config-repo"
    ```
- `config-repo/application.yml` holds shared settings (Eureka URL, the JWT secret shared
  between Auth and Gateway, token lifetimes, actuator).
- `config-repo/<service>.yml` holds each service's port and MongoDB URI.
- Business services import config via `spring.config.import=optional:configserver:...`
  and keep local fallbacks in their own `application.yml` for offline dev.

## Ports

| Service              | Port |
|----------------------|------|
| API Gateway          | 8080 |
| Config Server        | 8888 |
| Eureka               | 8761 |
| Auth                 | 8081 |
| User                 | 8082 |
| Account              | 8083 |
| Wallet               | 8084 |
| Transaction          | 8085 |

## Run order

1. `config-server`  →  2. `eureka-server`  →  3. `api-gateway`  →  4. business services.

```
cd config-server && mvn spring-boot:run
```

The Gateway validates JWTs for every route except the open auth endpoints
(`/auth/register`, `/auth/login`, `/auth/refresh`) — PRD §6.8. Its signing secret must
match the Auth Service's (shared via `config-repo/application.yml` / the `JWT_SECRET` env var).
