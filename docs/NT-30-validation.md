# NT-30 — UC02: đăng ký tài khoản người thuê

Yêu cầu: https://cuongthai.com/work/prj301-nha-tro-sinh-vien/NT/issue/30

Mã nguồn UC02 được quản lý trên nhánh `nt-30`.
Chạy các lệnh bên dưới từ thư mục gốc của repository.

## Cách dùng

Chạy bằng JDK 17, Tomcat 10.1 và CSDL NhaTroSV đã có bảng `Account`, `Tenant`
từ `setup.sql` của nhóm. Không chạy lại setup.sql để kiểm thử vì script đó xoá CSDL cũ.

Giữ nguyên DBContext cũ. Cấu hình cục bộ tại `web/WEB-INF/ConnectDB.properties`
có các khoá `url`, `userID`, `password`; file này đã được Git bỏ qua.
Không commit file cấu hình hoặc WAR chứa thông tin kết nối.
JDBC SQL Server và hai thư viện Jakarta JSTL được đặt trong `web/WEB-INF/lib`.
Đường dẫn JDBC trong NetBeans dùng đường dẫn tương đối.

Sau khi deploy, mở `<context-path>/register`.
Đăng ký thành công trả HTTP 302 tới `<context-path>/login?registered=1`.
Trang `/login` thuộc NT-31, chưa có trên nhánh main hiện tại.

## Hành vi và giao diện tích hợp

- `RegisterController`: GET forward tới `/WEB-INF/views/register.jsp`; POST đọc
  UTF-8 trước khi đọc tham số, gọi service và dùng PRG khi thành công.
- `AuthService.register(RegistrationForm, String)` trả `accountId`; lỗi dữ liệu
  trả `RegistrationException.getErrors()`, lỗi lưu trữ khác trả `SQLException`.
- Service kiểm tra username 4–30 ký tự ASCII (chữ, số, `.`, `_`, `-`), mật khẩu
  8–1024 ký tự, họ tên 1–100 ký tự, CCCD đúng 12 chữ số; điện thoại, email,
  trường, ngày sinh, quê quán được kiểm tra theo giới hạn của schema nếu có nhập.
- Service cố định role `TENANT`, active `true`; không nhận role hoặc ID tài khoản
  từ form. Hai INSERT dùng một connection, commit cùng nhau, rollback khi lỗi.
  Ràng buộc UNIQUE xử lý cả trường hợp đăng ký đồng thời vượt qua bước kiểm tra trùng.
- DAO kế thừa DBContext và dùng connection kế thừa; service điều khiển transaction.
  Hai phương thức INSERT không đóng/commit/rollback connection được truyền vào.
  Statement và ResultSet dùng try-with-resources; kết thúc service thì đóng DAO.
- `RegistrationForm` không chứa mật khẩu. JSP escape dữ liệu đã nhập, giữ lại các
  trường khác khi lỗi, để trống mật khẩu; form không tạo session. Đáp ứng có `Cache-Control: no-store`.
- Không tạo hoặc sửa EncodingFilter, mapping filter hoặc AGENTS.md.

`PasswordHasher.hash(String)` dùng PBKDF2WithHmacSHA256, 600.000 vòng,
salt SecureRandom 16 byte và hash 32 byte. Định dạng ghi theo issue:
`600000:saltBase64:hashBase64`. `verify(String password, String storedHash)`
cũng đọc được `pbkdf2_sha256$600000$saltBase64$hashBase64` trong tài liệu khung.
So sánh bằng `MessageDigest.isEqual`; hash sai định dạng trả `false`.
Tham khảo work factor: [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2).

## Kiểm thử

Chạy bằng Windows PowerShell (script UTF-8 có BOM):

```powershell
./test/run-registration-checks.ps1 `
  -TomcatHome 'C:/Program Files/Apache Software Foundation/Tomcat 10.1' `
  -JavaHome 'C:/Program Files/Java/jdk-17'
```

Có thể truyền `-DatabaseConfig '<đường dẫn ConnectDB.properties>'` và `-Port 18030`.
Script compile với `--release 17 -encoding UTF-8 -Xlint:all`, chạy kiểm thử độc lập,
tạo Tomcat tạm chỉ nghe `127.0.0.1`, chạy kiểm thử SQL Server và HTTP, dọn dữ liệu
kiểm thử và dừng đúng tiến trình Tomcat do script tạo. Log giữ lại ở đường dẫn
`Evidence` được in ra. Chỉ dùng `-KeepServer` nếu cần xem giao diện sau kiểm thử;
sau đó dừng PID được in ra. Không deploy `RegistrationChecksServlet` lên môi trường thật;
file này nằm ngoài `src/java` nên không được đưa vào WAR ứng dụng.

Kết quả ngày 2026-10-27, JDK 17 + Tomcat 10.1.17 + SQL Server NhaTroSV:

- PASS PasswordHasher: salt khác nhau, Unicode và khoảng trắng trong mật khẩu,
  mật khẩu sai, hai định dạng, vector PBKDF2 tính độc lập bằng .NET và hash hỏng.
- PASS validation: trường bắt buộc, độ dài, CCCD dùng chữ số ASCII, ngày sai/ngày tương lai.
- PASS SQL Server: Account/Tenant cùng ID, role TENANT, active, hash xác thực được,
  dữ liệu tiếng Việt, trường trống thành SQL NULL, trùng username/CCCD không để Account mồ côi.
- PASS lỗi UNIQUE thật tại INSERT Tenant sau khi INSERT Account đã chạy: service
  rollback Account, khôi phục auto-commit và đóng connection. Lỗi lưu trữ khác
  tại bước hai cũng rollback và được truyền ra.
- PASS HTTP: JSP render được, dữ liệu sai được kiểm tra server, dữ liệu form giữ
  lại và escape XSS, mật khẩu không phản chiếu, PRG đúng, gửi role ADMIN/accountId
  giả vẫn tạo TENANT, URL trực tiếp trong WEB-INF trả 404.
- PASS NetBeans Ant `dist`: tạo WAR thành công, không đóng gói các lớp kiểm thử.
- Kiểm tra DOM ở viewport 375px: chiều rộng nội dung bằng 375px, không tràn ngang.
  Chụp ảnh qua kết nối Chrome bị timeout; không có ảnh chụp được xác nhận.

Đã chạy lại toàn bộ kiểm thử từ workspace chính sau khi chuyển file: tất cả đều PASS.
Log lần chạy cuối: `C:/Users/Khoa/AppData/Local/Temp/nhatro-nt30-c6666eb5d7174f0bbeee978fc924fbe8`.
