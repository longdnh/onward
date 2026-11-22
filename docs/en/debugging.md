# Debugging: watching one request walk through the system

Vietnamese version: [../vi/debugging.md](../vi/debugging.md).

Two tools, pick by question:

- **"What value does this variable have right now?"** → breakpoints in VS Code (section 1).
- **"Which filter ran, in what order, and what did it decide?"** → the `debug` logging profile (section 2).

Both repos are set up for both. Nothing here needs code changes.

## 1. Breakpoints in VS Code

Open the repo folder in VS Code, go to **Run and Debug** (Ctrl+Shift+D), pick a configuration, press F5. The app starts under the debugger; a breakpoint stops the request mid-flight and the **Variables** panel shows every local value.

| Repo | Configuration | What it runs |
| --- | --- | --- |
| onward-gateway | `Gateway` | gateway on 8081 |
| onward-gateway | `Gateway - debug logging` | same, plus the logging of section 2 |
| onward-be | `Spring Boot - Local` | Account on 8080, profile `local` |
| onward-be | `Spring Boot - Local + debug logging` | same, plus the logging of section 2 |

Docker (`docker compose up -d`) must be running first; Account reads its database settings from the Postgres container.

### Where to put breakpoints

Five places cover every decision the system makes about a request. Put a breakpoint on the line, send a request through the gateway, and read the variables.

| Question | File and line | What to look at when it stops |
| --- | --- | --- |
| Did the gateway accept the token's audience? | gateway `SecurityConfiguration.java:53`, the `values -> values != null && ...` lambda (the line above it only builds the validator at startup and never stops on a request) | `values`: the `aud` array of the token |
| Is this session on the denylist? | gateway `SessionDenylistFilter.java:72` (the `.map(values -> ...)` line) | `values` is a list of two: the session entry and the user entry, `null` when absent; `issuedAt` |
| What did Redis do when it was down? | gateway `SessionDenylistFilter.java:75` (`onErrorResume`) | `exception`: TimeoutException or a connection error |
| Was this client rate limited? | gateway `PublicAuthRateLimitFilter.java:50` (`doOnNext`) | `key`, `response.isAllowed()`, `response.getHeaders()` (tokens remaining) |
| What does Account check in the database? | onward-be `SessionJwtAuthenticationConverter.java:40` (`sessionRepository.findById`) | `jwt.getClaims()`, then step over and inspect `session` and `user` |
| When does Account write to Redis? | onward-be `RedisSessionDenylist.java:53` (`redis.opsForValue().set`) | `key`, `value`, `ttl`. Hits after logout, reset, disable; never during normal requests |

### Reading reactive code in the debugger (gateway only)

The gateway is WebFlux. A line like `.map(values -> ...)` does **not** run when the debugger reaches it; it only builds the pipeline. The lambda runs later, when the response from Redis arrives. So:

- Put breakpoints **inside the lambdas** (the lines listed above), not on the `return redis.opsForValue()...` line.
- **Step Over** (F10) inside a lambda works normally. **Step Into** a `.map` or `.flatMap` call leads into Reactor internals; use Step Out (Shift+F11) to get back.
- The thread name in the call stack changes between lambdas (`parallel-2`, `lettuce-nioEventLoop-5-1`). That is normal: each stage may run on a different thread.

Account is classic Spring MVC: one thread per request, Step Into and Step Over behave as expected.

### Reading a token while stopped

In the **Debug Console**, with a breakpoint hit where `jwt` is in scope:

```text
jwt.getClaims()
jwt.getHeaders()
jwt.getExpiresAt()
```

## 2. Debug logging profile

Start either app with the profile `debug` (the `... debug logging` launch configurations, or `--spring.profiles.active=debug` for the gateway and `--spring.profiles.active=local,debug` for Account). The files are `application-debug.properties` in each repo; they only change log levels.

### What one `GET /api/auth/me` with a valid token prints

Gateway, in order:

```text
RoutePredicateHandlerMapping   : Route matched: account
DelegatingReactiveAuthorizationManager : Checking authorization on '/api/auth/me' using ...
SessionDenylistFilter          : Denylist check sid=01a1... user=01a1... iat=2026-11-21T14:33:38Z denied=false
```

Account, right after:

```text
FilterChainProxy               : Securing GET /api/auth/me
FilterChainProxy               : Invoking CsrfFilter (5/12)
FilterChainProxy               : Invoking BearerTokenAuthenticationFilter (7/12)
org.hibernate.SQL              : select ... from auth_sessions ... where id=?
org.hibernate.orm.jdbc.bind    : binding parameter (1:UUID) <- [01a1...]
org.hibernate.SQL              : select ... from users ... where id=?
org.hibernate.SQL              : update auth_sessions set last_seen_at=? where ...
BearerTokenAuthenticationFilter: Set SecurityContextHolder to JwtAuthenticationToken ...
FilterChainProxy               : Secured GET /api/auth/me
```

Those three SQL statements are Account's own session check, the one the gateway does not do. Seeing them for every request is the cost that section 7 of `microservice-decomposition.md` decided to keep for now.

### The same request without a token

Gateway stops it; Account prints nothing:

```text
DelegatingReactiveAuthorizationManager : Checking authorization on '/api/auth/me' using ...
AuthorizationWebFilter                 : Authorization failed: Access Denied
```

### A rate-limited request

```text
PublicAuthRateLimitFilter : Rate limit key=/api/auth/register|0:0:0:0:0:0:0:1 allowed=true remaining=3240
```

`remaining` is tokens left in the bucket. Each request costs 360, so 3240 means one request has been spent.

### Seeing what the gateway sends to Account, byte by byte

In the gateway's `application-debug.properties`, uncomment the two `wiretap` lines. Every forwarded request and its response are dumped in hex and text. Very noisy; turn it back off afterwards.

## 3. Looking at the wires from outside

- **Headers in and out:** `curl -v` prints the request headers it sends and the response headers it gets. `curl -i` prints only the response headers.
- **Every Redis command, live:** `docker exec onward-redis redis-cli monitor`. One authenticated request through the gateway shows exactly one `MGET`. A logout shows one `SET denied:session:... EX 300`.
- **Redis contents:** `docker exec onward-redis redis-cli keys '*'`, then `get <key>` and `ttl <key>`.
- **Mail:** <http://localhost:8025> (Mailpit) shows every mail with the raw link.
- **Broker:** <http://localhost:15672> (RabbitMQ, guest/guest) shows the exchange, queue and message counts.
- **Which process holds a port:** `ss -ltnp | grep 8081`.

## 4. The five connections, by file and line

Nothing registers with anything. Each connection is one URL in a config file and one line of code that uses it.

| Connection | Config | Code |
| --- | --- | --- |
| Client → gateway | gateway `application.properties`: `server.port` | none |
| Gateway → Account (forwarding) | gateway `application.properties`: `routes[0].uri` | none, Spring Cloud Gateway |
| Gateway → Account (public keys) | gateway `application.properties`: `gateway.auth.jwk-set-uri` | gateway `SecurityConfiguration.java`, `withJwkSetUri` |
| Gateway → Redis (read denylist, rate limit) | gateway `application.properties`: `spring.data.redis.*` | gateway `SessionDenylistFilter.java` `multiGet`; `PublicAuthRateLimitFilter.java` `isAllowed` |
| Account → Redis (write denylist) | onward-be `application-local.properties`: `spring.data.redis.*` | onward-be `RedisSessionDenylist.java`, `set` |

Account and the gateway never talk to each other except through forwarding. They agree on three values that must match in both config files: `issuer`, `audience`, and the JWKS URL.
