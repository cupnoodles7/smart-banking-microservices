# User Profile Bug — Investigation

**Bug:** Registration never creates a user profile, so user-service's `POST /users/internal`
endpoint and the whole `X-Internal-Api-Key` path are dead code. Separately, Auth's
`customerId` (a UUID) never matches user-service's document `_id`, so profile ownership
(`X-Customer-Id == User._id`) can never pass.

**Scope:** Investigation only — no code changes.

---

## 1. FILES

### auth-service (the core of the fix — it currently never talks to user-service)
- **`auth-service/src/main/java/com/smartbank/auth/service/AuthService.java`** → `registerUser(AuthRequest)` (lines 38–58). This is the method that must be extended to call `POST /users/internal` after (or instead of) generating the id, and to decide the failure/rollback behavior.
- **`auth-service/src/main/java/com/smartbank/auth/dto/request/AuthRequest.java`** → the whole DTO. It only carries `username`, `email`, `password`. user-service's `CreateUserRequest` demands `fullName`, `email`, `phoneNumber`, and a non-null `address` (all bean-validated). **Registration currently collects none of the profile fields**, so this DTO has to grow those fields (or the profile-create has to be split into a later "complete your profile" step — see FIX PLAN).
- **`auth-service/src/main/java/com/smartbank/auth/controller/AuthController.java`** → `register(...)` (lines 45–57). Only touched if the request/response shape changes (e.g. new profile fields, or surfacing a "profile creation failed" status).
- **A new HTTP-client component in auth-service** (new file, e.g. `client/UserServiceClient.java`) + **`auth-service/pom.xml`**. Auth's pom has `spring-boot-starter-web` and `eureka-client` but **no `RestTemplate`/`WebClient`/OpenFeign wiring today** — there is currently no way for it to make an outbound call. Account-service's `AccountServiceApplication` (`@LoadBalanced RestTemplate`) + `TransactionRecorder` is the established pattern to copy.
- **`auth-service/src/main/java/com/smartbank/auth/AuthServiceApplication.java`** → to declare the `@LoadBalanced RestTemplate`/`WebClient` bean (mirroring `AccountServiceApplication` lines 19–24).

### user-service (only if we choose the client-supplied-id approach)
- **`user-service/src/main/java/com/smartbank/user/dto/request/CreateUserRequest.java`** → add an `id`/`customerId` field.
- **`user-service/src/main/java/com/smartbank/user/mapper/UserMapper.java`** → `toEntity(CreateUserRequest)` (lines 19–26). Currently it never sets `id`, so Mongo auto-generates an ObjectId. It would need `user.setId(request.getId())`.
- **`user-service/src/main/java/com/smartbank/user/service/impl/UserServiceImpl.java`** → `createUser(...)` (lines 44–60). If ids are client-supplied, this should also reject a duplicate/blank id.

### config-repo / gateway (config, no routing change)
- **`config-repo/auth-service.yml`** → add the `security.internal.api-key` (the shared key auth must present) and, if using RestTemplate to a fixed URL, any `user-service.url`. Today this file has *only* server port + Mongo URI.
- **`config-repo/user-service.yml`** → already defines `security.internal.api-key` (lines 15–17); the value just has to be shared with auth. No structural change unless we rename.
- **api-gateway `application.yml`** → **no change needed for routing.** `/users/**` is already routed (lines 17–20) but `/users/internal` is deliberately *not* meant to go through the gateway — auth calls `lb://user-service` directly via Eureka. `/users/internal` is **not** in `open-paths`, which is fine because it never transits the gateway. Confirmed nothing in the gateway needs to open that path.

---

## 2. ROOT CAUSE

### What `registerUser()` does end-to-end today
`AuthService.registerUser()` (AuthService.java:38–58): checks username/email uniqueness → builds an auth `User` → `user.setCustomerId(UUID.randomUUID().toString())` (line 49) → BCrypt-hashes the password → `userRepository.save(user)` into `auth_db`. **That's the entire method. It never calls user-service at all** — there is no HTTP client, no `POST /users/internal`, nothing. It skips user-service entirely. So a registered credential exists in `auth_db.users` with a `customerId`, but no corresponding profile document is ever created in `user_db.users`.

Consequence: `POST /users/internal` (`UserController.createInternal`, lines 84–91) and its guard `requireValidInternalKey` / the entire `X-Internal-Api-Key` mechanism (`InternalApiProperties`, `UserServiceConstants.HEADER_INTERNAL_API_KEY`, `security.internal.api-key` in config) have **no caller anywhere in the repo** — confirmed dead code.

### Where customerId is generated in Auth, and its format
`AuthService.registerUser()` line 49: `user.setCustomerId(UUID.randomUUID().toString())`. **Confirmed: it is a UUID string** — 36 chars with dashes, e.g. `550e8400-e29b-41d4-a716-446655440000`. This value is what gets stamped into the JWT `customerId` claim (`JwtUtil.generateAccessToken`, line 100) and forwarded by the gateway as `X-Customer-Id`.

### What user-service's `User._id` actually is
user-service `User` entity (User.java:22–23): `@Id private String id`, no `@GeneratedValue`-style override, and `UserMapper.toEntity()` (lines 19–26) **never sets it**. So Spring Data MongoDB auto-generates a **Mongo ObjectId, persisted as a 24-char hex string** (e.g. `652f1a9c8b3e2a0012ab34cd`). Its own Javadoc even states "The document `_id` doubles as the system-wide `customerId`" — that's the *intent*, but nothing enforces it.

**Why they can never be equal:** a 36-char dashed UUID (generated independently in `auth_db`) versus a 24-char hex ObjectId (generated independently in `user_db`) are two separately-minted values in two databases with no link between them. `AuthenticatedCustomer.authorizeSelfAccess(callerId, id)` (lines 25–29) does `callerCustomerId.equals(targetId)` — UUID vs ObjectId — which is always false. Even the format lengths differ, so it can't accidentally collide.

### Other places that already assume the two ids are the same (blast radius)
This assumption is baked platform-wide, not just in user-service:

- **account-service — the active, breaking assumption.** `AccountServiceImpl.validateCustomerExists()` (lines 268–291) calls `GET http://user-service/users/{callerCustomerId}` with `X-Customer-Id = callerCustomerId`, i.e. it literally treats Auth's customerId as user-service's `_id` **and** relies on the self-access ownership check passing. With `user-service.validation-enabled: true` (the config-repo default), **account creation is currently impossible for any real user** — it would throw `CustomerNotFoundException` (profile doesn't exist) or, if a profile did exist, `ForbiddenException` (UUID ≠ ObjectId). The `validation-enabled: false` flag is effectively the only reason accounts can be created today.
- **api-gateway** `JwtAuthenticationFilter` — injects the JWT's `customerId` claim as `X-Customer-Id` for every downstream service. Whatever id auth chooses becomes the single shared identity value everywhere.
- **wallet-service** (`WalletController` + `WalletServiceImpl.createWallet`, entity `Wallet.customerId`) and **transaction-service** (`Transaction.customerId`, `getByCustomerId`) and **account-service** (`Account.customerId`, `findByCustomerId`) all **persist and query by this same `customerId`**. They trust the gateway header and don't call user-service, so they won't error — but they anchor all their rows to whatever id auth issues. That's why the fix must make the JWT `customerId` and the user-service `_id` be the *same value from the start*: changing the id scheme later would orphan every account/wallet/transaction row.

---

## 3. FIX PLAN

### Step 0 — Resolve the data-gap blocker first (this is the real design fork)
Before wiring anything, note: **`AuthRequest` doesn't collect the fields `CreateUserRequest` requires** (`fullName`, `phoneNumber`, `address` are all `@NotBlank`/`@NotNull` in user-service, plus phone must be 10 digits and is uniqueness-checked). So "wire register → create profile" is not a pure plumbing change; registration either has to start collecting profile data, or profile creation has to be deferred. Two shapes:
- **(A) One-shot registration:** expand `AuthRequest` to include the profile fields and create the profile inside `registerUser`.
- **(B) Two-step:** register creates only the credential + a minimal/placeholder profile (or none), and a later authenticated "complete profile" call fills it in. But account-service already blocks on the profile existing, so a placeholder profile would still need to be created at register time.

Recommend surfacing this as a product decision. For the id mechanics below, assume the profile is created at registration time (shape A or a placeholder under B).

### The id-ownership question: who mints the customerId? (tradeoffs, not a silent pick)

**Option 1 — Auth generates the id and passes it to user-service as the `_id` to use.**
- *How:* keep `UUID.randomUUID()` (or switch to an ObjectId-shaped value) in auth, send it in `CreateUserRequest.id`, user-service sets `user.setId(request.getId())` instead of auto-generating.
- *Pros:* Auth is the identity authority (it already owns the JWT). One value, generated once, before any DB write — no ordering dependency, no "what id do I put in the token" gap. Matches the entity Javadoc intent. Cleanest for the JWT-first flow.
- *Cons:* user-service must accept a client-supplied `_id` and defend it (reject blank/duplicate/malformed; a duplicate id now throws instead of silently overwriting). Slightly widens user-service's trust surface — anyone who can call `/users/internal` can choose ids (mitigated by the internal-key guard). If ids stay UUIDs, they no longer look like Mongo ObjectIds (cosmetic, but breaks the "doubles as ObjectId" mental model).

**Option 2 — user-service generates the ObjectId and returns it; Auth stores it as customerId.**
- *How:* auth calls `POST /users/internal` *without* an id, reads `UserResponse.id` from the 201, then sets `user.setCustomerId(returnedId)` before saving the credential and issuing tokens.
- *Pros:* user-service keeps full control of its `_id` (no client-supplied-id risk); ids stay native ObjectIds. user-service DTO/mapper need no change.
- *Cons:* Ordering dependency — auth cannot finalize the credential or mint a token until user-service responds. If the profile call succeeds but the response is lost, or auth crashes after, you get an **orphaned profile with no matching credential** (the inverse orphan). Makes user-service a hard, synchronous dependency of registration.

**Recommendation:** **Option 1.** Auth is already the identity source of truth (it signs the `customerId` into the JWT), the whole platform keys on that claim, and generating the id up-front removes the round-trip ordering problem. Option 2's benefit (native ObjectIds) is cosmetic here.

### Step 1 — Wire the call, and define failure behavior
Place the `POST /users/internal` call **inside `registerUser`, after the username/email uniqueness checks but as part of the same registration unit of work.** The ordering that avoids orphans under Option 1:

1. Generate `customerId`.
2. Call `POST /users/internal` (with `X-Internal-Api-Key` and the id) **first**.
3. Only on a 201 success, `userRepository.save(user)` the auth credential.
4. Return the response / issue tokens.

**On failure:** if the profile call fails (validation 400, duplicate 409, or user-service unreachable), **abort registration and return an error — do not save the auth credential.** This is the key anti-orphan decision. Because auth_db and user_db are separate Mongo databases, there is **no distributed transaction** — a local `@Transactional` won't roll back the remote call. Doing the remote create *before* the local save means:
- profile-create fails → nothing persisted in auth_db → clean failure, user can retry. ✅
- profile-create succeeds but auth save fails → orphaned *profile* (no credential). This is the residual risk; handle it with either a compensating delete call back to user-service on auth-save failure, or an idempotent `/users/internal` (keyed on the supplied id/email) so a retry reconciles rather than duplicates.

Open question for review: how far to go on the orphan edge (simple "profile-first, best-effort compensate" vs. a reconciliation job).

### Step 2 — Fix the account-service blast radius
Nothing to change in account-service *code* — once ids line up, `validateCustomerExists` (`GET /users/{customerId}`) starts passing on its own. But confirm `user-service.validation-enabled` is `true` in the real environments (it's currently the escape hatch masking this bug) and re-test account creation end-to-end after the fix.

### Step 3 — Re-examine `X-Internal-Api-Key` now that it's live
It's currently a documented placeholder (`UserController.createInternal` Javadoc, `InternalApiProperties`, config default `change-me-internal-user-service-key`). Now that it will actually gate a real, unauthenticated-by-JWT, profile-creating endpoint, this is the right moment to flag it rather than silently promote it:
- **Minimum bar:** it must never be the committed default value in any deployed env — inject a real secret via `USER_INTERNAL_API_KEY`, and confirm `/users/internal` is genuinely unreachable through the gateway (it is today: not routed as open, and auth hits `lb://user-service` directly — good).
- **Weaknesses to name:** a static shared secret is replayable, has no rotation story, and grants "create a profile with any id" to anyone who holds it. A leaked key + client-supplied ids (Option 1) = ability to pre-seed arbitrary customer ids.
- **Recommendation:** keep the shared key for *this* fix (don't expand scope), but explicitly log it as tech debt to revisit with the platform-wide internal-auth mechanism the code comments already anticipate (mTLS or a signed short-lived service token). If we adopt Option 1, tightening this is more urgent than under Option 2.
