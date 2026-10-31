# Quy tắc bắt buộc của dự án

Áp dụng cho toàn bộ repository khi viết, sửa hoặc review code.

## Servlet và điều hướng

- Mọi Servlet và URL mapping của Servlet phải được khai báo tập trung bằng `<servlet>` và `<servlet-mapping>` trong `web/WEB-INF/web.xml`; không dùng `@WebServlet`.
- Servlet chỉ đọc tham số → kiểm tra → gọi service → `setAttribute` → forward tới `/WEB-INF/views/...`.
- Sau mọi POST thành công, dùng redirect theo mẫu Post/Redirect/Get (PRG).
- Luật nghiệp vụ nằm trong service; truy vấn SQL nằm trong DAO.

## DAO và SQL

- SQL chỉ nằm trong DAO; mọi câu SQL dùng `PreparedStatement`.
- Truyền giá trị bằng placeholder `?`; không nối chuỗi SQL bằng dấu `+`.
- Đóng statement và result set bằng try-with-resources.
- Giữ nguyên `DBContext` bản cũ; DAO phải `extends DBContext` và dùng connection kế thừa.
- Đóng connection do DAO sở hữu khi dùng xong. Connection do service truyền vào thuộc service; DAO không tự đóng, commit hoặc rollback connection đó.

## JSP, dữ liệu và bảo mật

- JSP chỉ dùng EL `${...}`, JSTL và tag của nhóm; không dùng scriptlet `<% %>`.
- In dữ liệu người dùng nhập bằng `<c:out>` hoặc `${fn:escapeXml(...)}` để chống XSS.
- Mọi kiểm tra dữ liệu phải thực hiện ở server. Thuộc tính `required` của HTML chỉ hỗ trợ người dùng.
- Lấy ID người đang đăng nhập từ session; không lấy ID này từ URL hoặc form.

## Tên và encoding

- Đặt tên class và biến bằng tiếng Anh; chú thích bằng tiếng Việt khi cần giải thích vì sao.
- Filter UTF-8 phải đứng đầu chuỗi filter để dữ liệu tiếng Việt được giải mã đúng.
- Chỉ tạo hoặc sửa `EncodingFilter` và mapping của nó khi người dùng yêu cầu rõ ràng. Quy tắc thứ tự filter không cho phép tự thêm filter vào công việc khác.

## Kiểm tra trực tiếp, không tạo file demo hoặc test

- Kiểm tra trực tiếp chức năng thật trên ứng dụng của dự án qua giao diện hoặc HTTP, dùng luồng xử lý và cấu hình thực tế.
- Không tạo file demo, file test, harness, servlet/endpoint thử hoặc ứng dụng riêng để kiểm thử, kể cả file tạm trong `.local/`, trừ khi người dùng yêu cầu rõ ràng.
- Báo rõ thao tác đã kiểm tra, kết quả thực tế và phần chưa kiểm tra được; không khẳng định đã đạt nếu chưa chạy kiểm tra.

## Giữ nguyên quy tắc khi làm việc với Git

- Giữ nguyên các quy tắc trong file này và thứ tự Filter UTF-8 khi pull, push, merge hoặc rebase.
- Không tự ý sửa, xoá hoặc ghi đè file này để hoàn tất thao tác Git hay làm code vượt qua kiểm tra.
- Khi có xung đột, bảo toàn các quy tắc đã chốt và báo rõ khác biệt; chỉ thay đổi quy tắc khi người dùng yêu cầu rõ ràng.
- Trước khi hoàn tất thay đổi code, đối chiếu mọi file đã sửa với các quy tắc áp dụng ở trên.
