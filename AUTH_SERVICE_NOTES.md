# Auth Service — Integration Notes

Everything about how your auth files were ported into this repo, how the system fits
together, and how to run and test it. Nothing in the repo's code/structure was changed
to produce this document.

Build status: `auth-service` and `api-gateway` both compile (`mvn -DskipTests compile`, exit 0).

---

## 1. What changed (in detail)

### 1.1 Package & folder moves
- Base package `com.tnf.auth_service.*` → **`com.smartbank.auth.*`** (matches the repo's `auth-service` module).
- Files placed into the PRD §6.11 package layout:
  | Your file | New location |
  |-----------|--------------|
  | `JwtUtil` | `util/JwtUtil.java` |
  | `AuthService` | `service/AuthService.java` |
  | `AuthController` (`Controller/`) | `controller/AuthController.java` |
  | `User` | `entity/User.java` |
  | `UserRepository` | `repository/UserRepository.java` |
  | `AuthRequest`, `LoginRequest` | `dto/request/` |
  | `AuthResponse` | `dto/response/` |
  | `SecurityConfig`, `OpenApiConfig` (`Config/`) | `config/` |
  | *(new)* `RefreshRequest` | `dto/request/` |

### 1.2 Routing / endpoint path
- Controller base path `/api/auth` → **`/auth`**. The gateway routes `Path=/auth/**` to this
  service with no prefix stripping, so the controller must live under `/auth` to be reachable.

### 1.3 Configuration properties
- `${jwt.secret}` / `${jwt.expiration}` → the repo's shared **`security.jwt.*`**:
  - `security.jwt.secret` (shared with the gateway — this is what makes local validation work)
  - `security.jwt.access-token-expiry-ms` (900000 = 15 min)
  - `security.jwt.refresh-token-expiry-ms` (604800000 = 7 days)
- These come from `config-repo/application.yml` (served by the Config Server). Fallback copies
  were added to `auth-service/src/main/resources/application.yml` so the service can still sign
  tokens if the Config Server is down. `gateway.url` fallback (`:8080`) added for Swagger.

### 1.4 jjwt 0.11 → 0.12 API upgrade (`JwtUtil`)
The pom pins jjwt 0.12.6, whose API differs from your 0.11 code:
- `Jwts.parserBuilder().setSigningKey(k).build().parseClaimsJws(t).getBody()`
  → `Jwts.parser().verifyWith(k).build().parseSignedClaims(t).getPayload()`
- Builder `setClaims/setSubject/setIssuedAt/setExpiration` → `claims()/subject()/issuedAt()/expiration()`
- `signWith(key, SignatureAlgorithm.HS256)` → `signWith(key, Jwts.SIG.HS256)`
- `Key` → `javax.crypto.SecretKey`; secret bytes read as UTF-8.

### 1.5 Identity model — customerId + email + roles (decision)
Downstream services (account/wallet/transaction) key on **customerId**, so the token must carry it.
- `User` entity gained **`customerId`** (a UUID assigned once at registration), plus `refreshToken`
  and `refreshTokenExpiry`.
- Access token now carries claims: `customerId`, `email`, `roles`, `type=access`; `subject = username`.
- The **gateway filter** was updated to forward these downstream as trusted headers:
  `X-Auth-Username` (subject), `X-Customer-Id`, `X-User-Email`, `X-Auth-Roles` (comma-joined).
- `AuthController` now reads `X-Auth-Username` / `X-Auth-Roles` (previously read raw, unforwarded headers).

### 1.6 Refresh tokens added (PRD §6.8) (decision)
- New endpoint **`POST /auth/refresh`** + `RefreshRequest` DTO.
- Login issues an **access + refresh** pair; the refresh token (and its expiry) is stored on the user.
- `AuthResponse` now returns `token` (access), `refreshToken`, and `customerId`.
- `refreshAccessToken()` validates the refresh token's signature, `type=refresh`, that it matches the
  stored one, and that it hasn't expired, then issues a fresh access token.

### 1.7 Validation model — local at the gateway (decision)
- The gateway verifies the JWT **itself** using the shared secret; it does **not** call the auth service.
- Your `/auth/validate` endpoint is kept only as an optional debug/introspection helper.

### 1.8 Fixes & smaller changes
- `validateTokeb` typo → replaced by a proper `validateToken(token, username)` overload.
- `OpenApiConfig` gateway URL default corrected `:8083` → **`:8080`**.
- Added the **`springdoc-openapi-starter-webmvc-ui` 2.6.0** dependency (OpenApiConfig needs it).
- `registerUser` now assigns `customerId`; `loginUser` persists the refresh token; log messages tidied.
- `SecurityConfig` simplified to `permitAll` (auth is enforced at the gateway); comment that contained
  a `*/` sequence (which would have closed the Javadoc early and broken compilation) was fixed.

---

## 2. Application flow (Track B)

### 2.1 Topology
```
                       ┌──────────────┐        ┌──────────────┐
                       │ Config Server│        │    Eureka    │
                       │    :8888     │        │    :8761     │
                       │ (git-backed) │        │  (registry)  │
                       └──────┬───────┘        └──────┬───────┘
     every service pulls config at startup     every service registers here
                              │                       │
   Client ─▶ ┌───────────────────────────────────────────────┐
   (Bearer)  │              API Gateway  :8080                 │
             │  1. match route (/auth,/users,/accounts,...)    │
             │  2. JWT filter: open path? skip : verify locally│
             │  3. forward X-Auth-Username/X-Customer-Id/...   │
             │  4. route via Eureka  lb://<service>            │
             └───────┬─────────┬─────────┬─────────┬──────────┘
                     │         │         │         │
                 auth:8081 user:8082 account:8083 wallet:8084 transaction:8085
                     │         │         │         │         │
                  auth_db   user_db  account_db wallet_db transaction_db   (one Mongo DB each)
```

### 2.2 Startup sequence
1. **Config Server** boots first (self-contained; serves `config-repo/*.yml` over Git).
2. **Eureka** boots (self-contained registry).
3. **API Gateway** boots, registers with Eureka, loads routes + the JWT secret.
4. **Business services** boot: each imports its config from the Config Server, connects to its own
   MongoDB, and registers with Eureka.

### 2.3 Request lifecycle (any protected call)
1. Client sends request to the gateway with `Authorization: Bearer <accessToken>`.
2. Gateway's `JwtAuthenticationFilter` (order −1, runs before routing):
   - If the path is in `security.jwt.open-paths` (`/auth/register`, `/auth/login`, `/auth/refresh`,
     `/actuator/**`) → pass through untouched.
   - Otherwise verify the token locally with the shared secret. Invalid/missing → **401** in the
     standard error shape; no downstream call happens.
   - Valid → attach `X-Auth-Username`, `X-Customer-Id`, `X-User-Email`, `X-Auth-Roles`.
3. Gateway resolves `lb://<service>` through Eureka and forwards the request.
4. The service trusts those headers (it does **not** re-validate the JWT) and does its work against
   its own MongoDB.

### 2.4 Auth-specific flow
- **Register** (`POST /auth/register`, open): validates uniqueness of username/email, BCrypt-hashes the
  password, assigns a `customerId`, saves to `auth_db`.
- **Login** (`POST /auth/login`, open): verifies password + enabled, updates `lastLogin`, issues an
  access token (15 min) and a refresh token (7 days, stored on the user), returns both.
- **Refresh** (`POST /auth/refresh`, open): checks the refresh token and returns a new access token.
- **Protected** (`GET /auth/me`, `GET /auth/users`, `POST /auth/logout`): require a valid access token;
  the gateway injects identity headers the controller reads.

---

## 3. Order the auth files were written

1. `auth-service/pom.xml` — added the springdoc dependency
2. `auth-service/src/main/resources/application.yml` — added `security.jwt.*` + `gateway.url` fallbacks
3. `AuthServiceApplication.java`
4. `entity/User.java`
5. `repository/UserRepository.java`
6. `dto/request/AuthRequest.java`
7. `dto/request/LoginRequest.java`
8. `dto/request/RefreshRequest.java`
9. `dto/response/AuthResponse.java`
10. `util/JwtUtil.java`
11. `service/AuthService.java`
12. `controller/AuthController.java`
13. `config/SecurityConfig.java`
14. `config/OpenApiConfig.java`
15. *(then)* `api-gateway/.../JwtAuthenticationFilter.java` — header-forwarding update
16. *(then)* `config/SecurityConfig.java` — Javadoc `*/` fix

---

## 4. How to run & test

### 4.1 Prerequisites
- Java 21+ and Maven (you have Maven 3.9.16).
- **MongoDB** running on `localhost:27017` (each service creates its own DB automatically).
  Quick option: `docker run -d -p 27017:27017 --name mongo mongo:7`.
- A Git source for the Config Server (it's git-backed). Simplest local setup:
  ```bash
  cd config-repo
  git init && git add . && git commit -m "config"
  cd ..
  export CONFIG_REPO_URI="file://$(pwd)/config-repo"
  ```
  (Or push `config-repo/` to a remote and `export CONFIG_REPO_URI=https://.../bank-config-repo.git`.)

### 4.2 Start the platform, then the service (each in its own terminal)
```bash
# 1. Config Server   (uses CONFIG_REPO_URI from above)
cd config-server && mvn spring-boot:run

# 2. Eureka
cd eureka-server && mvn spring-boot:run

# 3. API Gateway
cd api-gateway && mvn spring-boot:run

# 4. Auth Service
cd auth-service && mvn spring-boot:run
```
Sanity checks:
- Eureka dashboard: http://localhost:8761  (auth-service + api-gateway should appear as UP)
- Health: http://localhost:8081/actuator/health  and  http://localhost:8080/actuator/health
- Swagger (direct on the service, not via gateway): http://localhost:8081/swagger-ui.html

> Tip: to test auth alone without the platform, you can run just `auth-service` — the Config Server
> import is `optional:`, so it falls back to the local `application.yml` (port 8081, local Mongo).
> You then call it directly on `:8081` instead of through the gateway on `:8080`.

### 4.3 Test the auth flow (through the gateway, :8080)

**Register**
```bash
curl -s -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"akriti","email":"akriti@example.com","password":"secret123"}'
# 201 -> { message, customerId, username, email, roles }
```

**Login** (capture the tokens)
```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"akriti","password":"secret123"}'
# 200 -> { token, refreshToken, type:"Bearer", customerId, username, email, roles, expiresIn }
```
Save the access token:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"akriti","password":"secret123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
```

**Call a protected endpoint** (gateway validates + injects `X-Auth-Username`)
```bash
curl -s http://localhost:8080/auth/me -H "Authorization: Bearer $TOKEN"
# 200 -> the user profile
```

**No / bad token → 401 from the gateway**
```bash
curl -i -s http://localhost:8080/auth/me
# HTTP/1.1 401  { "timestamp":..., "status":401, "error":"Unauthorized", ... }
```

**Refresh the access token**
```bash
REFRESH=<paste refreshToken from login>
curl -s -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"
# 200 -> { token (new access), ... }
```

**Admin-only endpoint** (needs an ADMIN role in the token; default users get `USER`)
```bash
curl -i -s http://localhost:8080/auth/users -H "Authorization: Bearer $TOKEN"
# 403/500-style "Access denied" unless the token's roles contain ADMIN
```

### 4.4 Expected results summary
| Call | Result |
|------|--------|
| register (new) | 201 + customerId |
| register (dup username/email) | error "already exists" |
| login (good) | 200 + access + refresh token |
| login (bad password) | error "Invalid username/email or password" |
| `/auth/me` with valid token | 200 + profile |
| `/auth/me` without token | 401 at the gateway |
| `/auth/refresh` with valid refresh token | 200 + new access token |
