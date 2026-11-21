# Calling the registration API

Vietnamese translation: [../vi/registration-api.md](../vi/registration-api.md).

Implemented endpoints:

| Endpoint | Purpose |
| --- | --- |
| `GET /api/auth/csrf` | Obtain a CSRF cookie and a token for subsequent requests |
| `POST /api/auth/register` | Create a global account and return its UUIDv7 ID |

Registration creates an account with no memberships and does not log the user in. Login and access/refresh-token issuance are subsequent steps.

## 1. Start the local backend

With the project's PostgreSQL connection and Docker setup ready, run:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

The `local` profile allows the CSRF cookie on localhost HTTP. The default configuration requires HTTPS for the cookie. Use the same host for all requests below so the cookie is sent consistently.

## 2. Obtain CSRF credentials

```bash
curl -i -c cookies.txt http://localhost:8080/api/auth/csrf
```

The response contains a `Set-Cookie` header for `XSRF-TOKEN` and JSON shaped like:

```json
{
  "headerName": "X-CSRF-TOKEN",
  "token": "<copy-this-response-token>"
}
```

Keep `cookies.txt` and copy the token from the JSON response. Spring returns a masked token; use that response value in the header, not the cookie's value. The cookie is `HttpOnly`; a browser client obtains the header token through this endpoint.

## 3. Register an account

Replace the placeholder header value with the token from step 2:

```bash
curl -i -b cookies.txt \
  -H 'Content-Type: application/json' \
  -H 'X-CSRF-TOKEN: <copy-this-response-token>' \
  --data '{"email":"alice@example.com","password":"violet river autumn 42"}' \
  http://localhost:8080/api/auth/register
```

Successful response: `201 Created`, `Cache-Control: no-store`, and:

```json
{
  "userId": "<generated-uuidv7>"
}
```

The email is stripped and lowercased. Passwords use the current 10–128 Unicode-code-point policy and the local blocklist. Password spaces and case are preserved when hashing. Email syntax validation does not verify mailbox ownership.

In Postman, first send the CSRF GET request with its cookie jar enabled. Then POST to the registration URL with a raw JSON body and the returned header name and token. Postman sends the stored cookie automatically.

## 4. Understand failures

Errors have this shape:

```json
{
  "code": "EMAIL_ALREADY_EXISTS",
  "message": "An account with this email already exists"
}
```

Messages are maintained in `common.constants.ClientMessages`, and response codes in `common.constants.ApiErrorCodes`. Frontend logic should branch on `code`; `message` is display text. Parameterized messages use the actual policy limits instead of repeating their numeric values in the message catalog.

| Status / code | Meaning |
| --- | --- |
| `400 INVALID_REQUEST` | Missing or invalid credentials, malformed JSON, unknown fields, duplicate keys, trailing JSON, or wrong JSON value types |
| `403 CSRF_INVALID` | Missing cookie/header or an invalid CSRF token |
| `409 EMAIL_ALREADY_EXISTS` | An account with the canonical email already exists |
| `415 UNSUPPORTED_MEDIA_TYPE` | The request is not `application/json` |
| `429 RATE_LIMITED` | Registration allowance exhausted; use the `Retry-After` response header |
| `500 INTERNAL_ERROR` | Unexpected server or database failure; no exception details are returned |

Protected routes currently return `401 UNAUTHENTICATED` because login/JWT authentication is not implemented. CSRF is checked before rate limiting and controller input handling, so a request with both invalid JSON and missing CSRF receives `403` first.

Rate limiting of registration is enforced by the gateway, not by Account (see `microservice-decomposition.md`, 7.4): 10 requests per client per path per hour as a token bucket in Redis, shared by all gateway instances, answered with `429 RATE_LIMITED` and `Retry-After`. Account itself no longer limits, so it must not be reachable by clients directly in production.

## 5. Follow the code

1. `SecurityConfiguration` permits the two public routes, configures cookie CSRF, and returns JSON security errors. Authentication is not stored in servlet sessions.
2. `RegistrationRateLimitFilter` runs after CSRF and before JSON binding. It keeps bounded, synchronized, in-memory counters for this single-instance project.
3. `WebConfiguration` configures strict Jackson 3 parsing, including rejecting unknown fields and numeric/boolean values for strings.
4. `AuthController` receives `RegisterRequest` through `@RequestBody`, invokes Bean Validation through `@Valid`, and delegates to `RegisterService`.
5. `RegisterService` performs the existing validation, hashing, UUIDv7 generation, and insert workflow.
6. `RegisterResponse` exposes only `userId`. The project-wide `web.error.GlobalExceptionHandler` handles application and MVC errors for all controllers. `ApiErrorWriter` also writes security-filter errors.

`web.error.ExceptionHandlingFilter` is registered once as a servlet filter before Spring Security. It catches unhandled exceptions from the downstream synchronous filter chain and delegates to Spring MVC's `HandlerExceptionResolver`, reusing the global exception mappings. An unresolved exception produces a generic `500 INTERNAL_ERROR`. The filter preserves security handlers' normal `401`/`403` responses and never writes a second response once the response is committed. Direct I/O failures are rethrown; asynchronous/background work and container errors outside this filter's invocation are outside its scope. Framework input errors now use the general `INVALID_REQUEST` message, while explicit registration-policy errors retain their specific messages.

Request DTO `toString()` output redacts credentials, and password serialization is disabled. Errors omit rejected values and parser/database exception messages. A `record` is used for each DTO to provide immutable fields and accessors such as `request.email()`.

This guide describes manual API usage. No automated tests are added by this step.
