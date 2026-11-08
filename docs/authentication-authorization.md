# Simple multi-tenant authentication and authorization

Status: registration and CSRF HTTP endpoints, registration rate limiting, the registration service and input policy, UUIDv7 generation, password hashing, and user persistence are implemented. Login, refresh, logout, and tenant-management APIs remain planned. See the [registration API walkthrough](registration-api.md) for local usage.

Authentication uses a planned combination of access JWTs, application-owned sessions, and rotating refresh tokens. Their [domain models](data-and-domain-model.md#authsession--one-login) are defined. The implemented CSRF cookie does not represent a login session; Spring Security does not persist authentication in servlet `HttpSession`.

Build a backend that lets users register, sign in, belong to multiple tenants, and perform actions according to their role in each tenant. The application has no selected business domain. This document uses **organization** as the name for a tenant.

The existing project uses Java 21, Spring Boot, Spring Security, Spring Data JPA, Flyway, and PostgreSQL.

## 1. First version

Implement:

- Registration, login, current-user lookup, and logout.
- Organization creation and listing the organizations a user belongs to.
- Adding registered users to organizations, changing roles, and removing memberships.
- Tenant isolation and three fixed roles: `OWNER`, `ADMIN`, and `MEMBER`.
- Tests demonstrating both allowed and denied access.

Use one backend and one PostgreSQL database with a shared schema. Access JWTs will identify users and their application-owned login sessions; browser refresh credentials will use cookies. Curl or Postman can retain cookies and send headers, so a frontend is optional.

Keep email invitations, email verification, password recovery, MFA, social login, custom roles, and audit dashboards as optional extensions. The first version is a local learning project.

## 2. Identity, tenant, and role

| Concept | Meaning | Example |
| --- | --- | --- |
| User | A global account that can log in | Alice |
| Organization | A tenant and its access boundary | Organization A |
| Membership | A user's association with an organization | Alice belongs to Organization A |
| Role | Permissions granted through that membership | Alice is its owner |

**Authentication** identifies the user. **Authorization** checks whether that user may perform an action in the requested organization.

Alice can be an `OWNER` in Organization A and a `MEMBER` in Organization B. Logging in identifies Alice; it does not give her owner access everywhere. Roles belong on memberships, not on the user record.

```mermaid
flowchart LR
    R[Request with access JWT] --> U[Validate JWT, session, and user]
    U --> T[Read organization ID from route]
    T --> M[Load current membership]
    M --> P[Check permission for action and target]
    P --> D[Access data within that organization]
```

## 3. Minimal data model

Three application tables currently exist. `AuthSession` and `RefreshToken` domain classes are defined; their database tables and repositories are a subsequent step.

| Entity | Fields | Constraints |
| --- | --- | --- |
| `User` | `id`, `email`, `passwordHash`, `status`, `createdAt`, `updatedAt` | Unique canonical email; status is `ACTIVE` or `DISABLED` |
| `Organization` | `id`, `name`, `createdAt`, `updatedAt` | Names do not grant access or establish identity |
| `Membership` | `id`, `organizationId`, `userId`, `role`, `status`, `createdAt`, `updatedAt` | Unique `(organizationId, userId)`; status is `ACTIVE` or `REMOVED` |

Use UUIDv7 IDs, UTC timestamps, foreign keys to users and organizations, and database constraints for valid roles and statuses. UUIDs do not replace permission checks.

The [data and domain model specification](data-and-domain-model.md) defines the Java models, implemented persistence, and initial PostgreSQL migration. Registration is implemented; login/session and tenancy services remain planned work.

For this application, canonicalize email by trimming surrounding whitespace and lowercasing with a locale-independent rule, then enforce a maximum of 254 Unicode code points. Organization names are trimmed and limited to 200 Unicode code points. Do not strip dots or `+` suffixes from email. Never transform or silently truncate a password.

A user starts with no memberships. Creating an organization gives that user its owner membership. Removing a membership preserves its row; adding that person again reactivates it with the newly authorized role.

Permissions are a fixed mapping in application code for this version. Separate role and permission tables are unnecessary until custom roles become a requirement.

## 4. Authentication

### Registration and passwords

The implemented entry point is `RegisterService.register(email, rawPassword)`, returning the created user's UUID. `RegistrationPolicy` canonicalizes and validates email with Jakarta Bean Validation's `@Email`, checks the current 10–128 code-point password limit, and checks a case-insensitive local blocklist. Password hashing receives the original input, including spaces and case. The blocklist at `src/main/resources/security/common-passwords.txt` is a small starter list for this learning project, not a comprehensive breach corpus. Email validation checks syntax; it does not verify mailbox ownership.

The service uses `IdGenerator`, `PasswordHasher`, `UserRepository`, and an injected UTC `Clock`. Infrastructure supplies UUIDv7 through Java UUID Generator and salted Argon2id hashes through Spring Security. Stored hashes carry the `{argon2}` encoder identifier and the Argon2 parameters and salt. The configured costs are 19 MiB memory, 2 iterations, and parallelism 1. [Java UUID Generator](https://github.com/cowtowncoder/java-uuid-generator)

An initial email lookup provides an early duplicate result. The insert-only `UserRepository.create` operation uses `persist` and `flush` in a write transaction, with the database's `uq_users_email` constraint resolving concurrent registrations. Both duplicate paths raise `EmailAlreadyExistsException`. Other database errors propagate separately. Password hashing runs before the write transaction starts. Registration creates neither memberships nor an authenticated session.

`AuthController` exposes `POST /api/auth/register`. A dedicated request DTO accepts credentials and redacts them in `toString()`; a separate response DTO contains only `userId`. Bean Validation checks required fields, while the service's policy handles normalization, email syntax, and password rules. `GlobalExceptionHandler` translates invalid registration input to `400`, duplicates to `409`, and unexpected failures to a generic `500` without returning exception details or rejected values.

Registration accepts `{email, password}` as JSON strings and creates an active user with no organization or role. Unknown fields such as `role`, `organizationId`, or `status`, duplicate keys, trailing JSON values, and numeric/boolean credentials are rejected. Other content types return `415` after security checks pass.

Return `201` on creation and `409 EMAIL_ALREADY_EXISTS` for a duplicate canonical email. This simple contract reveals whether an address is registered; a later public signup flow can use generic responses and email verification. Never overwrite an existing user's credentials.

Email is only a login identifier in this version. Without verification, it is not evidence of mailbox ownership. Assign memberships using a registered user's ID; do not automatically join organizations based on email or email domain.

The current project password policy accepts 10–128 Unicode code points, including spaces, without mandatory character combinations. It uses a local common-password blocklist. Hashes use Spring Security's `PasswordEncoder`, configured for Argon2id with 19 MiB memory, 2 iterations, and parallelism 1, with Bouncy Castle providing the required implementation. See [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) and [Spring password storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).

### Login, sessions, refresh, and logout — planned

The [authentication implementation guide](authentication-implementation.md) describes the login, protected-request, refresh, and logout flows, transaction boundaries, and suggested configuration code. It is a guide for user implementation, not a statement that these features exist. Use one validated infrastructure configuration record for timeout settings; a separate `SessionPolicy` class is unnecessary at this stage.

1. Login accepts `{email, password}` and checks credentials and current account status.
2. Return the same `401 Invalid credentials` for an unknown user, incorrect password, or disabled account. Perform a dummy hash comparison for unknown users to avoid an obvious timing difference.
3. On success, create a new `AuthSession`, store a refresh-token hash, issue a five-minute access JWT, and set the refresh-token cookie. Access-token claims include `sub`, `sid`, `iss`, `aud`, `iat`, and `exp`.
4. Validate the JWT and check that its session belongs to its user, is active, and has not expired. Check current account status and load current tenant memberships for authorization; roles remain specific to the requested organization.
5. Use 30 minutes idle expiry and a 12-hour absolute session lifetime. Accepted activity cannot extend the absolute deadline.
6. Refresh atomically consumes the old refresh token and inserts its replacement. Reuse of a consumed token revokes the session, and that revocation must commit before returning an error.
7. Logout revokes the current session and expires its refresh cookie. Subsequent protected requests fail their session check even when the access JWT has not expired.

Use a host-only refresh cookie with `HttpOnly`, `Secure`, and `SameSite=Lax`. Access JWTs are sent in the `Authorization: Bearer` header. Refresh-token generation, JWT signing and validation, session persistence, and these endpoints are not yet implemented. Form login, HTTP Basic, and Spring's default logout endpoint are disabled.

Planned login throttling: 5 failed attempts per identifier and 30 per IP in 15 minutes, counting unknown identifiers too. Implemented registration throttling: 10 requests per connection IP in a fixed one-hour window, counted after CSRF succeeds and before JSON binding. Responses use `429 RATE_LIMITED` with `Retry-After`. Configure `app.security.registration.max-attempts` and `app.security.registration.window-seconds`. The limiter is in memory, resets on restart, and bounds tracked clients at 10,000; a new client is temporarily rejected if that capacity is full. It does not read caller-supplied forwarded-IP headers. These are project choices; multiple backend instances would need shared limits.

### CSRF

CSRF protection is enabled. `GET /api/auth/csrf` returns `{headerName, token}` and sets a host-only `XSRF-TOKEN` cookie with `HttpOnly`, `SameSite=Lax`, and `Path=/`. The cookie is `Secure` by default; the explicit `local` profile disables `Secure` for localhost HTTP. Spring's cookie CSRF repository and default XOR request handler are used. Retain the cookie and send the token from the JSON response in the returned `X-CSRF-TOKEN` header; the cookie value itself is not the masked response token. Invalid or missing CSRF returns `403 CSRF_INVALID`. Future login and logout flows must rotate CSRF state and clients must fetch a fresh token afterward.

Auth responses use `Cache-Control: no-store`. Start with same-origin browser access. A future frontend on another origin needs an explicit CORS allowlist with credentials; CORS does not grant tenant permissions. See [Spring CSRF protection](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

## 5. Roles and membership lifecycle

Every permission below requires an active user and active membership in the requested organization.

Each organization has exactly one active `OWNER` (single-owner model). The database enforces "at most one" with a partial unique index on active owner memberships (migration V4). The application keeps "at least one" by creating the owner together with the organization and by changing who the owner is only through an ownership transfer.

| Action | OWNER | ADMIN | MEMBER |
| --- | --- | --- | --- |
| Read organization profile | Yes | Yes | Yes |
| Update organization name | Yes | Yes | No |
| Read own membership | Yes | Yes | Yes |
| List organization memberships | Yes | Yes | No |
| Add a registered user as `MEMBER` | Yes | Yes | No |
| Add a registered user as `ADMIN` | Yes | No | No |
| Change a membership's role between `ADMIN` and `MEMBER` | Yes | No | No |
| Remove a `MEMBER` | Yes | Yes | No |
| Remove an `ADMIN` | Yes | No | No |
| Transfer ownership to an active member | Yes | No | No |

Any active authenticated user can create an organization and list their own organizations. An admin cannot promote themselves, create another admin, change roles, or remove admins. No role grants access to all tenants or control over another user's global account. The owner's own membership is never demoted or removed, not even by the owner; ownership moves only through a transfer.

- **Create organization:** create the organization and the caller's owner membership in one transaction. Derive the owner from authentication.
- **Add member:** accept `{userId, role}` for an existing active user; `role` is `ADMIN` or `MEMBER`, never `OWNER`. Apply the role matrix before creating or reactivating membership. An already-active membership returns `409 MEMBERSHIP_ALREADY_ACTIVE`; adding cannot silently change its role. A removed membership is reactivated with the newly assigned role, keeping its ID.
- **Change role:** `ADMIN` to `MEMBER` or back. `OWNER` as the new role returns `400`. Targeting the owner's membership returns `409 OWNER_PROTECTED`; targeting a removed membership returns `409 MEMBERSHIP_NOT_ACTIVE`. Setting the role the membership already has is a no-op `200`.
- **Remove member:** mark it `REMOVED`. Subsequent requests lose access to this organization; login and other memberships remain valid. Removing an already removed membership is an idempotent `204`. Targeting the owner returns `409 OWNER_PROTECTED`.
- **Transfer ownership:** the owner names an active membership of the same organization whose user account is active. In one transaction the current owner becomes `ADMIN` first and the target becomes `OWNER` second, so the unique index holds after every statement. Transferring to oneself is a no-op `200`.
- Subsequent requests use the current role, even with a preexisting session.

For local learning, the new user obtains their ID from `/api/auth/me` and shares it with an owner or admin. Knowing an ID does not authorize member creation. No global user-directory endpoint is needed. Invitation acceptance can replace direct assignment later.

Serialize membership mutations by locking the organization row in a transaction and performing every check after the lock: the actor's role, the target's current role and status, and the receiving account's status. This prevents, for example, an admin removing a member who received ownership while that request was waiting. Membership writes are flushed immediately so statements reach the database in call order.

Accepted limitation: with a single owner, a lost or disabled owner account leaves the organization without anyone able to transfer ownership. A production system would need an out-of-band support operation; this learning project does not. There is no organization-deletion or global account-management API in this version.

## 6. Tenant isolation

Use routes such as `/api/organizations/{organizationId}/memberships`. The route selects the requested tenant; it is untrusted input, not proof of access.

For every tenant request:

1. Authenticate the user and check account status.
2. Load active membership using both `organizationId` and authenticated `userId`.
3. Check whether the actor's role permits the operation.
4. Load the target using both its ID and the requested organization ID.
5. Check target-specific restrictions, such as whether an admin may remove that member's role.
6. For writes, repeat relevant checks inside the transaction.

Scope membership queries, for example `findByOrganizationIdAndId(...)`. Lists and counts must use the same organization filter. A membership ID from B must not work inside an A URL, even when the caller belongs to both.

Derive the organization from the validated route. Only member creation accepts a target `userId`; only authorized member creation and role changes accept a role. Reject unknown or server-controlled input fields. Each tenant route carries its organization ID; the session has no mutable global active tenant.

Keep permission checks in an `OrganizationAccessService` or `AuthorizationPolicy`, enforced by application services. `SecurityFilterChain` handles authentication and CSRF. A global `hasRole('OWNER')` check alone cannot establish tenant access. If using `@PreAuthorize`, enable method security explicitly and account for proxy self-invocation limitations. See [OWASP Authorization](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html), [OWASP Multi-Tenant Security](https://cheatsheetseries.owasp.org/cheatsheets/Multi_Tenant_Security_Cheat_Sheet.html), and [Spring method security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html).

## 7. API contracts

All endpoints below are implemented. Only `GET /api/auth/csrf` and the `POST` endpoints `register`, `login`, `refresh`, and `logout` allow anonymous requests; every other route requires a Bearer access token. Public unsafe methods require CSRF. Requests carrying a Bearer token are exempt from CSRF (Spring Security's resource-server default), so a request without a token to a protected unsafe endpoint is rejected by CSRF first and returns `403` rather than `401`. Roles below always mean roles in the requested organization.

| Method and path | Access | Input / success result |
| --- | --- | --- |
| `GET /api/auth/csrf` | Public | `200 {headerName, token}`; cookie as needed |
| `POST /api/auth/register` | Public | `{email, password}` → `201 {userId}` |
| `POST /api/auth/login` | Public | `{email, password}` → `200 {accessToken, tokenType, expiresIn}` and refresh cookie |
| `POST /api/auth/refresh` | Valid refresh cookie, with CSRF | `200 {accessToken, tokenType, expiresIn}` and replacement refresh cookie |
| `GET /api/auth/me` | Authenticated | `200 {userId, email}` |
| `POST /api/auth/logout` | Public, with CSRF | Invalidate current session if present → `204` |
| `POST /api/organizations` | Authenticated | `{name}` → `201` organization and owner membership |
| `GET /api/organizations` | Authenticated | `200` caller's active organizations and roles |
| `GET /api/organizations/{orgId}` | Any active member | `200` profile |
| `PATCH /api/organizations/{orgId}` | Owner or admin | `{name}` → `200` profile |
| `GET /api/organizations/{orgId}/memberships/me` | Any active member | `200` own membership |
| `GET /api/organizations/{orgId}/memberships` | Owner or admin | `200` membership list |
| `POST /api/organizations/{orgId}/memberships` | Owner or admin, role restrictions apply | `{userId, role}` → `201` created or reactivated membership |
| `PATCH /api/organizations/{orgId}/memberships/{id}/role` | Owner | `{role}` (`ADMIN` or `MEMBER`) → `200` membership |
| `DELETE /api/organizations/{orgId}/memberships/{id}` | Owner, or admin removing a member | Mark removed → `204`; idempotent |
| `POST /api/organizations/{orgId}/memberships/ownership-transfer` | Owner | `{membershipId}` → `200 {newOwner, previousOwner}` |

Membership responses contain `id`, `organizationId`, `userId`, `role`, and `status`. Lists are paginated: page 0 and size 20 by default, maximum size 100, stable ordering. Never return password hashes or session details. Member lists omit email addresses and memberships in other tenants.

| Status | Use |
| --- | --- |
| `400` | Invalid input, unsupported role, or unrecognized fields |
| `401` | Invalid credentials or missing/expired authentication |
| `403` | Tenant member lacks permission; also missing/invalid CSRF |
| `404` | Organization inaccessible, resource outside requested tenant, or target user unavailable for member creation |
| `409` | Duplicate registration, already-active membership, owner's membership targeted by a role change or removal, or removed membership targeted by a role change or transfer |
| `429` | Throttled request, with `Retry-After` |

Use consistent `{code, message}` errors. Nonmembers receive the same `404` whether an organization exists or not. After establishing membership, check operation permission before mutation and scope target lookup to that organization. Missing authentication normally returns `401`, but a CSRF rejection can occur first and return `403`.

## 8. Acceptance tests

Use organizations A and B. Alice owns A and is a member of B; Bob is an admin of A; Carol is a member of A; Eve belongs only to B.

| Scenario | Expected result |
| --- | --- |
| Register and log in | Password hashed; subsequent request recognizes user |
| Register duplicate canonical email | `409`; original password unchanged |
| Unknown user, wrong password, or disabled user logs in | Same `401` response shape |
| Successful login | Creates a new application-owned session and issues tokens |
| Unsafe request lacks valid CSRF | `403`; no mutation |
| Logout or reach session expiry | Subsequent protected request returns `401` |
| Create organization | Caller receives owner membership atomically |
| Alice tries to manage B members | `403`; her A role does not apply |
| Eve requests A | `404`; no tenant information leaked |
| Use B membership ID in A route | `404`; no read or change |
| Bob adds or removes an ordinary member | Allowed |
| Bob adds an admin, changes a role, or removes an admin | `403` |
| Anyone, including Alice, demotes or removes the owner | `409`; ownership changes only through a transfer |
| Register with a supplied role or tenant ID | `400`; no privilege granted |
| List organizations or memberships | Only permitted data and counts appear |
| Demote or remove a member with an existing session | Next request reflects current membership |
| Alice transfers ownership to Carol | Carol becomes `OWNER` and Alice becomes `ADMIN` in one transaction; Alice can no longer transfer |
| Transfer to a removed membership or a disabled account | `409` or `404`; the owner is unchanged |
| A second active owner is inserted directly in the database | Rejected by the partial unique index |
| Concurrent ownership transfer and admin removal of the same member | Exactly one succeeds; the organization keeps exactly one active owner |
| Concurrent promotion to `ADMIN` and admin removal of the same member | Serialized checks: the promotion wins and the removal returns `403`, or the removal wins and the promotion returns `409` |

Use policy unit tests, Spring Security integration tests for JWTs/CSRF, and PostgreSQL integration tests for scoping, constraints, and concurrency. The scenarios above have been exercised manually against a local PostgreSQL, including the concurrent ones (20 rounds each); automated coverage is the next step.

## 9. Implementation order

1. Completed: the three initial tables, registration service and HTTP API, password hashing, CSRF endpoint, and registration throttling.
2. Completed: session/refresh-token persistence, login with throttling, JWT/session validation and `/auth/me`, refresh rotation, and logout. See the [implementation guide](authentication-implementation.md).
3. Completed: organization creation, listing, profile, and rename.
4. Completed: the role matrix, member management, and the single-owner model with ownership transfer (migration V4).
5. Next: automated authentication, tenant-isolation, permission, and concurrency tests for the scenarios above.

The first version is complete when a user can log in, belong to multiple organizations with different roles, and access only the operations and tenant data permitted by current memberships.
