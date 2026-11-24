# Gọi API đăng ký

Các endpoint đã được triển khai:

| Endpoint | Mục đích |
| --- | --- |
| `GET /api/auth/csrf` | Lấy CSRF cookie và một token để dùng cho các request tiếp theo |
| `POST /api/auth/register` | Tạo một tài khoản toàn cục và trả về ID dạng UUIDv7 của tài khoản đó |

Đăng ký tạo ra một tài khoản chưa có membership nào và không đăng nhập người dùng. Đăng nhập và cấp access/refresh token là các bước tiếp theo.

## 1. Khởi động backend ở môi trường local

Profile `local` ký access token bằng một RSA key pair đọc từ `~/.config/onward/jwt/`. Các key này không được đưa vào repository, nên cần sinh chúng một lần trên mỗi máy. `openssl` đã có sẵn trong `shell.nix`.

```bash
mkdir -p ~/.config/onward/jwt && chmod 700 ~/.config/onward/jwt
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out ~/.config/onward/jwt/private.pem
openssl pkey -in ~/.config/onward/jwt/private.pem -pubout -out ~/.config/onward/jwt/public.pem
chmod 600 ~/.config/onward/jwt/private.pem
```

Nếu thiếu các file này, ứng dụng sẽ khởi động thất bại khi tạo bean `jwkSet`.

Khi kết nối PostgreSQL và thiết lập Docker của dự án đã sẵn sàng, chạy:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Profile `local` cho phép CSRF cookie hoạt động trên localhost qua HTTP. Cấu hình mặc định yêu cầu HTTPS cho cookie này. Hãy dùng cùng một host cho tất cả các request bên dưới để cookie được gửi kèm nhất quán.

## 2. Lấy thông tin CSRF

```bash
curl -i -c cookies.txt http://localhost:8080/api/auth/csrf
```

Response chứa một header `Set-Cookie` cho `XSRF-TOKEN` và JSON có dạng:

```json
{
  "headerName": "X-CSRF-TOKEN",
  "token": "<copy-this-response-token>"
}
```

Giữ lại `cookies.txt` và sao chép token từ JSON response. Spring trả về một token đã được mask; hãy dùng giá trị trong response đó cho header, không dùng giá trị của cookie. Cookie này là `HttpOnly`; một client trên trình duyệt lấy token cho header thông qua endpoint này.

## 3. Đăng ký tài khoản

Thay giá trị header placeholder bằng token lấy được ở bước 2:

```bash
curl -i -b cookies.txt \
  -H 'Content-Type: application/json' \
  -H 'X-CSRF-TOKEN: <copy-this-response-token>' \
  --data '{"email":"alice@example.com","password":"violet river autumn 42"}' \
  http://localhost:8080/api/auth/register
```

Response khi thành công: `201 Created`, `Cache-Control: no-store`, và:

```json
{
  "userId": "<generated-uuidv7>"
}
```

Email được cắt khoảng trắng thừa và chuyển về chữ thường. Mật khẩu tuân theo chính sách hiện hành 10–128 Unicode code point và blocklist cục bộ. Khoảng trắng và chữ hoa/thường trong mật khẩu được giữ nguyên khi hash. Validation cú pháp email không xác minh quyền sở hữu hộp thư.

Trong Postman, trước tiên gửi request GET CSRF với cookie jar đã được bật. Sau đó POST tới URL đăng ký với body JSON dạng raw cùng tên header và token nhận được. Postman tự động gửi kèm cookie đã lưu.

## 4. Hiểu các trường hợp lỗi

Lỗi có dạng như sau:

```json
{
  "code": "EMAIL_ALREADY_EXISTS",
  "message": "An account with this email already exists"
}
```

Các message được quản lý trong `common.constants.ClientMessages`, còn các mã response trong `common.constants.ApiErrorCodes`. Logic phía frontend nên rẽ nhánh theo `code`; `message` chỉ là văn bản để hiển thị. Các message có tham số dùng đúng giới hạn thực tế của chính sách thay vì lặp lại các giá trị số đó trong danh mục message.

| Status / code | Ý nghĩa |
| --- | --- |
| `400 INVALID_REQUEST` | Thiếu hoặc sai thông tin đăng nhập, JSON sai định dạng, có field không xác định, key trùng lặp, JSON có phần dư ở cuối, hoặc kiểu giá trị JSON không đúng |
| `403 CSRF_INVALID` | Thiếu cookie/header hoặc CSRF token không hợp lệ |
| `409 EMAIL_ALREADY_EXISTS` | Đã tồn tại tài khoản với email chuẩn hóa này |
| `415 UNSUPPORTED_MEDIA_TYPE` | Request không phải `application/json` |
| `429 RATE_LIMITED` | Đã dùng hết hạn mức đăng ký; hãy dựa vào header `Retry-After` của response |
| `500 INTERNAL_ERROR` | Lỗi server hoặc database không mong đợi; không trả về chi tiết exception |

Các route được bảo vệ hiện trả về `401 UNAUTHENTICATED` vì phần đăng nhập/xác thực JWT chưa được triển khai. CSRF được kiểm tra trước rate limit và trước khi controller xử lý input, nên một request vừa có JSON không hợp lệ vừa thiếu CSRF sẽ nhận `403` trước.

Rate limit cho đăng ký do gateway thực thi, không phải Account (xem `microservice-decomposition.md`, mục 7.4): 10 request cho mỗi client trên mỗi path mỗi giờ, dưới dạng token bucket trong Redis, dùng chung cho tất cả các instance gateway, và được trả lời bằng `429 RATE_LIMITED` kèm `Retry-After`. Bản thân Account không còn giới hạn nữa, nên trong production nó không được để client truy cập trực tiếp.

## 5. Lần theo mã nguồn

1. `SecurityConfiguration` cho phép hai route công khai, cấu hình CSRF dựa trên cookie, và trả về lỗi bảo mật dưới dạng JSON. Thông tin xác thực không được lưu trong servlet session.
2. `RegistrationRateLimitFilter` chạy sau CSRF và trước khi bind JSON. Nó giữ các bộ đếm in-memory có giới hạn và được đồng bộ hóa cho dự án đơn instance này.
3. `WebConfiguration` cấu hình Jackson 3 parse ở chế độ nghiêm ngặt, bao gồm từ chối các field không xác định và từ chối giá trị số/boolean cho các trường chuỗi.
4. `AuthController` nhận `RegisterRequest` qua `@RequestBody`, gọi Bean Validation thông qua `@Valid`, rồi ủy quyền cho `RegisterService`.
5. `RegisterService` thực hiện quy trình hiện có gồm validation, hash, sinh UUIDv7 và insert.
6. `RegisterResponse` chỉ lộ ra `userId`. `web.error.GlobalExceptionHandler` dùng chung toàn dự án xử lý lỗi của ứng dụng và lỗi MVC cho tất cả các controller. `ApiErrorWriter` cũng ghi các lỗi phát sinh từ security filter.

`web.error.ExceptionHandlingFilter` được đăng ký một lần dưới dạng servlet filter đứng trước Spring Security. Nó bắt các exception chưa được xử lý từ chuỗi filter đồng bộ phía sau và ủy quyền cho `HandlerExceptionResolver` của Spring MVC, tái sử dụng các ánh xạ exception toàn cục. Một exception không được phân giải sẽ tạo ra lỗi chung `500 INTERNAL_ERROR`. Filter này giữ nguyên các response `401`/`403` thông thường của các security handler và không bao giờ ghi response thứ hai một khi response đã được commit. Các lỗi I/O trực tiếp được ném lại; công việc bất đồng bộ/chạy nền và các lỗi của container nằm ngoài lượt gọi của filter này thì không thuộc phạm vi của nó. Các lỗi input từ framework giờ dùng message chung `INVALID_REQUEST`, trong khi các lỗi vi phạm chính sách đăng ký được nêu rõ vẫn giữ message riêng của chúng.

Đầu ra `toString()` của request DTO che đi thông tin đăng nhập, và việc serialize mật khẩu bị vô hiệu hóa. Các lỗi không kèm giá trị bị từ chối cũng như message exception của parser/database. Mỗi DTO được viết dưới dạng `record` để có các field bất biến và các accessor như `request.email()`.

Tài liệu này mô tả cách sử dụng API thủ công. Bước này không bổ sung automated test nào.
