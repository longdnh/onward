# Gọi API đăng ký

Bản tiếng Anh (bản gốc): [../en/registration-api.md](../en/registration-api.md).

Các endpoint đã triển khai:

| Endpoint | Mục đích |
| --- | --- |
| `GET /api/auth/csrf` | Lấy cookie CSRF và một token cho các request tiếp theo |
| `POST /api/auth/register` | Tạo tài khoản toàn cục và trả về ID dạng UUIDv7 của tài khoản đó |

Đăng ký tạo ra một tài khoản chưa có membership nào và không đăng nhập người dùng. Đăng nhập và cấp access/refresh token là các bước tiếp theo.

## 1. Khởi động backend local

Khi kết nối PostgreSQL và thiết lập Docker của project đã sẵn sàng, chạy:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Profile `local` cho phép cookie CSRF trên localhost HTTP. Cấu hình mặc định yêu cầu HTTPS cho cookie. Dùng cùng một host cho tất cả các request bên dưới để cookie được gửi nhất quán.

## 2. Lấy thông tin CSRF

```bash
curl -i -c cookies.txt http://localhost:8080/api/auth/csrf
```

Response chứa header `Set-Cookie` cho `XSRF-TOKEN` và JSON có dạng:

```json
{
  "headerName": "X-CSRF-TOKEN",
  "token": "<copy-this-response-token>"
}
```

Giữ lại `cookies.txt` và sao chép token từ JSON response. Spring trả về một token đã được che (masked); dùng giá trị trong response đó cho header, không dùng giá trị của cookie. Cookie là `HttpOnly`; client trên trình duyệt lấy token cho header thông qua endpoint này.

## 3. Đăng ký tài khoản

Thay giá trị header giữ chỗ bằng token lấy ở bước 2:

```bash
curl -i -b cookies.txt \
  -H 'Content-Type: application/json' \
  -H 'X-CSRF-TOKEN: <copy-this-response-token>' \
  --data '{"email":"alice@example.com","password":"violet river autumn 42"}' \
  http://localhost:8080/api/auth/register
```

Response thành công: `201 Created`, `Cache-Control: no-store`, và:

```json
{
  "userId": "<generated-uuidv7>"
}
```

Email được cắt khoảng trắng hai đầu và chuyển về chữ thường. Password tuân theo chính sách hiện tại là 10–128 code point Unicode và blocklist local. Khoảng trắng và chữ hoa/thường trong password được giữ nguyên khi hash. Validation cú pháp email không xác minh quyền sở hữu hộp thư.

Trong Postman, trước hết gửi request GET CSRF với cookie jar được bật. Sau đó POST tới URL đăng ký với body JSON dạng raw cùng tên header và token đã trả về. Postman tự động gửi cookie đã lưu.

## 4. Hiểu các trường hợp lỗi

Lỗi có dạng sau:

```json
{
  "code": "EMAIL_ALREADY_EXISTS",
  "message": "An account with this email already exists"
}
```

Các message được quản lý trong `common.constants.ClientMessages`, còn các mã response trong `common.constants.ApiErrorCodes`. Logic frontend nên rẽ nhánh theo `code`; `message` là văn bản để hiển thị. Các message có tham số dùng giới hạn thực tế của chính sách thay vì lặp lại giá trị số trong danh mục message.

| Status / code | Ý nghĩa |
| --- | --- |
| `400 INVALID_REQUEST` | Thiếu hoặc sai thông tin đăng nhập, JSON sai định dạng, trường không xác định, khóa trùng lặp, JSON thừa ở cuối, hoặc sai kiểu giá trị JSON |
| `403 CSRF_INVALID` | Thiếu cookie/header hoặc token CSRF không hợp lệ |
| `409 EMAIL_ALREADY_EXISTS` | Đã tồn tại tài khoản với email chuẩn hóa này |
| `415 UNSUPPORTED_MEDIA_TYPE` | Request không phải `application/json` |
| `429 RATE_LIMITED` | Đã hết hạn mức đăng ký; dùng header response `Retry-After` |
| `500 INTERNAL_ERROR` | Lỗi server hoặc cơ sở dữ liệu không mong đợi; không trả về chi tiết exception |

Các route được bảo vệ hiện trả về `401 UNAUTHENTICATED` vì xác thực đăng nhập/JWT chưa được triển khai. CSRF được kiểm tra trước rate limit và trước xử lý đầu vào của controller, nên một request vừa có JSON không hợp lệ vừa thiếu CSRF sẽ nhận `403` trước.

Rate limit cho đăng ký do gateway thực thi, không phải Account (xem `microservice-decomposition.md`, 7.4): 10 request cho mỗi client trên mỗi path mỗi giờ theo cơ chế token bucket trong Redis, dùng chung cho tất cả các instance gateway, trả lời bằng `429 RATE_LIMITED` và `Retry-After`. Bản thân Account không còn giới hạn nữa, nên trong production client không được phép truy cập trực tiếp tới nó.

## 5. Theo dõi mã nguồn

1. `SecurityConfiguration` cho phép hai route công khai, cấu hình CSRF dạng cookie, và trả về lỗi bảo mật dưới dạng JSON. Thông tin xác thực không được lưu trong servlet session.
2. `RegistrationRateLimitFilter` chạy sau CSRF và trước khi bind JSON. Nó giữ các bộ đếm trong bộ nhớ, có giới hạn và được đồng bộ hóa, cho project chạy một instance này.
3. `WebConfiguration` cấu hình phân tích Jackson 3 ở chế độ nghiêm ngặt, bao gồm từ chối các trường không xác định và từ chối giá trị số/boolean cho kiểu chuỗi.
4. `AuthController` nhận `RegisterRequest` qua `@RequestBody`, gọi Bean Validation qua `@Valid`, và ủy quyền cho `RegisterService`.
5. `RegisterService` thực hiện luồng validation, hash, sinh UUIDv7 và insert hiện có.
6. `RegisterResponse` chỉ lộ ra `userId`. `web.error.GlobalExceptionHandler` dùng chung toàn project xử lý lỗi của application và MVC cho tất cả các controller. `ApiErrorWriter` cũng ghi lỗi của các security filter.

`web.error.ExceptionHandlingFilter` được đăng ký một lần làm servlet filter đứng trước Spring Security. Nó bắt các exception chưa được xử lý từ chuỗi filter đồng bộ phía sau và ủy quyền cho `HandlerExceptionResolver` của Spring MVC, tái sử dụng các ánh xạ exception toàn cục. Một exception không được giải quyết sẽ tạo ra `500 INTERNAL_ERROR` chung. Filter này giữ nguyên các response `401`/`403` thông thường của các security handler và không bao giờ ghi response thứ hai một khi response đã được commit. Lỗi I/O trực tiếp được ném lại; công việc bất đồng bộ/chạy nền và lỗi của container nằm ngoài lượt gọi của filter này không thuộc phạm vi của nó. Lỗi đầu vào của framework giờ dùng message `INVALID_REQUEST` chung, trong khi các lỗi chính sách đăng ký tường minh vẫn giữ message riêng của chúng.

Đầu ra `toString()` của request DTO che đi thông tin đăng nhập, và việc serialize password bị tắt. Lỗi không kèm giá trị bị từ chối cũng như message exception của parser/cơ sở dữ liệu. Mỗi DTO dùng một `record` để có các trường bất biến và các accessor như `request.email()`.

Hướng dẫn này mô tả cách dùng API thủ công. Bước này không thêm test tự động nào.
