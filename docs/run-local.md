# Chạy web trên máy

Trong terminal PowerShell tại thư mục project:

```powershell
.\scripts\run-local.ps1
```

Script build bằng JDK của `javac`, sau đó chạy Tomcat riêng ở cổng 8080.
Các đường dẫn mặc định sử dụng bản cài NetBeans 17 và Tomcat 10.1 trên máy này.
Có thể truyền `-JdkPath`, `-AntPath`, `-TomcatPath` hoặc `-Port` khi cần.

- Chi tiết phòng: <http://127.0.0.1:8080/Project_NhaTro/rooms/detail?roomId=1>
- Giao diện mẫu: <http://127.0.0.1:8080/Project_NhaTro/layout-demo>

Chạy lại lệnh sau khi sửa code để build và khởi động lại. Dừng server này bằng:

```powershell
.\scripts\run-local.ps1 -Stop
```

Thư viện JDBC và JSTL nằm trong `web/WEB-INF/lib`, chỉ được đóng gói vào project.
Servlet/JSP API được cung cấp bởi bản cài Tomcat. Cấu hình server, log, bản build
và WAR dùng riêng thư mục `.local/`, đã được bỏ qua trong Git. Script không sửa
bản cài Tomcat hay cấu hình Java chung của máy.

`web/WEB-INF/ConnectDB.properties` là cấu hình database cục bộ theo DBContext
hiện tại, đã được bỏ qua trong Git. Các khóa cần có là `url`, `userID`, `password`.
Phần đăng nhập chưa có trong nhánh này; gửi yêu cầu thuê cần session `accountId`
kiểu Integer và `role` là `TENANT` từ chức năng đăng nhập.
