# Debug: theo dõi một request đi qua hệ thống

Bản tiếng Anh (bản gốc): [../en/debugging.md](../en/debugging.md).

Có hai công cụ, chọn theo câu hỏi đang cần trả lời:

- **"Biến này đang có giá trị gì?"** → breakpoint trong VS Code (mục 1).
- **"Request đi qua filter nào, theo thứ tự nào, mỗi filter quyết định gì?"** → profile log `debug` (mục 2).

Cả hai repo đã được cấu hình sẵn cho cả hai cách. Không cần sửa code.

## 1. Breakpoint trong VS Code

Mở thư mục repo trong VS Code, vào **Run and Debug** (Ctrl+Shift+D), chọn một cấu hình, bấm F5. App chạy dưới debugger; gặp breakpoint thì request dừng lại giữa chừng và panel **Variables** hiện mọi giá trị tại chỗ đó.

| Repo | Cấu hình | Chạy gì |
| --- | --- | --- |
| onward-gateway | `Gateway` | gateway ở cổng 8081 |
| onward-gateway | `Gateway - debug logging` | như trên, cộng log của mục 2 |
| onward-be | `Spring Boot - Local` | Account ở cổng 8080, profile `local` |
| onward-be | `Spring Boot - Local + debug logging` | như trên, cộng log của mục 2 |

Phải `docker compose up -d` trước; Account lấy thông tin kết nối DB từ container Postgres.

### Đặt breakpoint ở đâu

Năm chỗ dưới đây phủ hết mọi quyết định hệ thống đưa ra với một request. Đặt breakpoint vào dòng đó, gửi request qua gateway, rồi đọc biến.

| Câu hỏi | File và dòng | Nhìn vào gì khi dừng |
| --- | --- | --- |
| Gateway có chấp nhận audience của token không? | gateway `SecurityConfiguration.java:53`, lambda `values -> values != null && ...` (dòng ngay trên chỉ dựng validator lúc khởi động, không bao giờ dừng khi có request) | `values`: mảng `aud` trong token |
| Session này có trong denylist không? | gateway `SessionDenylistFilter.java:72` (dòng `.map(values -> ...)`) | `values` là list hai phần tử: ô session và ô user, `null` nếu không có; `issuedAt` |
| Redis chết thì chuyện gì xảy ra? | gateway `SessionDenylistFilter.java:75` (`onErrorResume`) | `exception`: TimeoutException hoặc lỗi kết nối |
| Client này có bị rate limit không? | gateway `PublicAuthRateLimitFilter.java:50` (`doOnNext`) | `key`, `response.isAllowed()`, `response.getHeaders()` (số token còn lại) |
| Account kiểm tra gì trong DB? | onward-be `SessionJwtAuthenticationConverter.java:40` (`sessionRepository.findById`) | `jwt.getClaims()`, rồi Step Over và xem `session`, `user` |
| Account ghi Redis lúc nào? | onward-be `RedisSessionDenylist.java:53` (`redis.opsForValue().set`) | `key`, `value`, `ttl`. Chỉ dừng sau logout, reset, disable; không bao giờ dừng ở request thường |

### Đọc code reactive trong debugger (chỉ gateway)

Gateway chạy WebFlux. Một dòng như `.map(values -> ...)` **không** chạy khi debugger đi tới nó; dòng đó chỉ dựng pipeline. Lambda bên trong chạy sau, khi Redis trả lời. Vì vậy:

- Đặt breakpoint **bên trong lambda** (các dòng liệt kê ở trên), không đặt ở dòng `return redis.opsForValue()...`.
- **Step Over** (F10) trong lambda hoạt động bình thường. **Step Into** vào `.map` hay `.flatMap` sẽ dẫn vào ruột Reactor; dùng Step Out (Shift+F11) để quay ra.
- Tên thread trong call stack đổi giữa các lambda (`parallel-2`, `lettuce-nioEventLoop-5-1`). Bình thường: mỗi bước có thể chạy trên thread khác.

Account là Spring MVC cổ điển: một thread cho một request, Step Into và Step Over hoạt động như mong đợi.

### Đọc token khi đang dừng

Trong **Debug Console**, khi dừng ở chỗ có biến `jwt`:

```text
jwt.getClaims()
jwt.getHeaders()
jwt.getExpiresAt()
```

## 2. Profile log `debug`

Chạy app với profile `debug` (chọn cấu hình có đuôi `debug logging`, hoặc `--spring.profiles.active=debug` cho gateway và `--spring.profiles.active=local,debug` cho Account). File cấu hình là `application-debug.properties` trong mỗi repo; nó chỉ đổi mức log, không đổi hành vi.

### Một request `GET /api/auth/me` có token hợp lệ in ra gì

Gateway, theo thứ tự:

```text
RoutePredicateHandlerMapping   : Route matched: account
DelegatingReactiveAuthorizationManager : Checking authorization on '/api/auth/me' using ...
SessionDenylistFilter          : Denylist check sid=01a1... user=01a1... iat=2026-11-21T14:33:38Z denied=false
```

Account, ngay sau đó:

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

Ba câu SQL đó là phần kiểm tra session riêng của Account, phần mà gateway không làm. Thấy chúng ở mọi request chính là cái giá mà mục 7 của `microservice-decomposition.md` đã quyết định giữ lại.

### Cùng request đó nhưng không có token

Gateway chặn; Account không in gì:

```text
DelegatingReactiveAuthorizationManager : Checking authorization on '/api/auth/me' using ...
AuthorizationWebFilter                 : Authorization failed: Access Denied
```

### Một request bị rate limit

```text
PublicAuthRateLimitFilter : Rate limit key=/api/auth/register|0:0:0:0:0:0:0:1 allowed=true remaining=3240
```

`remaining` là số token còn trong bucket. Mỗi request tốn 360, nên 3240 nghĩa là đã dùng một request.

### Xem gateway gửi gì sang Account, từng byte

Trong `application-debug.properties` của gateway, bỏ comment hai dòng `wiretap`. Mọi request forward và response của nó được in ra dạng hex và text. Rất nhiều; tắt lại sau khi xem xong.

## 3. Nhìn các "dây nối" từ bên ngoài

- **Header gửi đi và nhận về:** `curl -v` in header request nó gửi và header response nó nhận. `curl -i` chỉ in header response.
- **Mọi lệnh Redis, thời gian thực:** `docker exec onward-redis redis-cli monitor`. Một request có token qua gateway hiện đúng một `MGET`. Một lần logout hiện một `SET denied:session:... EX 300`.
- **Nội dung Redis:** `docker exec onward-redis redis-cli keys '*'`, rồi `get <key>` và `ttl <key>`.
- **Mail:** <http://localhost:8025> (Mailpit) hiện mọi mail kèm link thô.
- **Broker:** <http://localhost:15672> (RabbitMQ, guest/guest) hiện exchange, queue và số message.
- **Process nào đang giữ cổng:** `ss -ltnp | grep 8081`.

## 4. Năm dây nối, theo file và dòng

Không có chỗ nào các service "đăng ký" với nhau. Mỗi dây nối là một URL trong file config và một dòng code dùng URL đó.

| Dây nối | Config | Code |
| --- | --- | --- |
| Client → gateway | gateway `application.properties`: `server.port` | không có |
| Gateway → Account (forward) | gateway `application.properties`: `routes[0].uri` | không có, Spring Cloud Gateway tự làm |
| Gateway → Account (lấy public key) | gateway `application.properties`: `gateway.auth.jwk-set-uri` | gateway `SecurityConfiguration.java`, `withJwkSetUri` |
| Gateway → Redis (đọc denylist, rate limit) | gateway `application.properties`: `spring.data.redis.*` | gateway `SessionDenylistFilter.java` `multiGet`; `PublicAuthRateLimitFilter.java` `isAllowed` |
| Account → Redis (ghi denylist) | onward-be `application-local.properties`: `spring.data.redis.*` | onward-be `RedisSessionDenylist.java`, `set` |

Account và gateway không bao giờ nói chuyện với nhau ngoài việc forward. Chúng chỉ thống nhất ba giá trị phải giống nhau trong hai file config: `issuer`, `audience`, và URL JWKS.
