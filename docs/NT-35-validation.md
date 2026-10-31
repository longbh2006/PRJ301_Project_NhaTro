# NT-35 — UC05: yêu cầu và hợp đồng của người thuê

- `GET /tenant/requests`: giữ luồng danh sách, bộ lọc và chi tiết đã có; chỉ truy vấn yêu cầu của ID trong session.
- `POST /tenant/requests/cancel`: nhận `requestId`, `csrfToken`; lấy `tenantId` từ session. Thành công redirect về danh sách, thông báo chỉ xuất hiện một lần.
- `GET /tenant/contract`: lấy hợp đồng ACTIVE của ID trong session, hiển thị mã phòng, ngày bắt đầu/kết thúc, giá thuê, tiền cọc và số người. Không có hợp đồng thì hiển thị thông báo.

Nút hủy chỉ hiển thị cho PENDING. Service gọi UPDATE có đủ điều kiện mã yêu cầu,
người thuê và trạng thái PENDING; không có dòng cập nhật thì trả 409. Vì kiểm tra
nằm trong cùng câu UPDATE, thao tác hủy không ghi đè yêu cầu đã được duyệt.
Khi hủy, ghi `decidedAt = SYSDATETIME()` và `decidedBy = NULL` theo ràng buộc
`CK_RentalRequest_decision` của CSDL hiện tại.

Servlet mới và mapping bổ sung khai báo trong web.xml. Không sửa DBContext,
EncodingFilter, cấu hình kết nối hoặc quy tắc dự án. Không tạo file test,
harness hay endpoint thử.

## Kết quả kiểm tra ngày 31/10/2026

Build bằng Ant của dự án, JDK 17: thành công, biên dịch 33 lớp và tạo WAR.
Triển khai chính output `build/web` trên Tomcat 10.1.17, cổng 8085, context
`/Project_NhaTro`; dùng cấu hình kết nối cục bộ của DBContext cũ.

Kiểm tra HTTP trực tiếp qua controller, service, DAO và SQL Server thật:

- Khách xem yêu cầu/hợp đồng hoặc POST hủy: 302 về đăng nhập.
- Truy cập JSP hợp đồng trong WEB-INF trực tiếp: 404.
- Đăng ký và đăng nhập qua chức năng thật; tài khoản mới xem danh sách rỗng: 200.
- Chưa có hợp đồng: 200 và thông báo, kể cả khi gửi tenantId/contractId khác trên URL.
- Tạo yêu cầu PENDING qua POST thật, danh sách hiển thị nút hủy.
- Xem chi tiết yêu cầu của tài khoản khác: 404; hủy của tài khoản khác với token hợp lệ: 409.
- Ghi chú chứa HTML được escape khi xem chi tiết.
- Thiếu token CSRF: 403; ID bằng 0, âm, chữ hoặc vượt miền int: 400.
- GET đường dẫn hủy: 405.
- Hủy hai yêu cầu do phiên kiểm tra tạo: 302 về danh sách; gửi tenantId giả trong form không ảnh hưởng ID lấy từ session.
- Sau redirect: trạng thái Đã hủy, không còn nút hủy, thông báo chỉ hiển thị một lần.
- POST hủy lại: 409; lọc CANCELLED hiển thị đúng yêu cầu.

Lần chạy đầu phát hiện UPDATE thiếu decidedAt, bị CHECK constraint từ chối;
đã sửa và chạy lại các kiểm tra hủy, quyền sở hữu, CSRF, dữ liệu sai và PRG nêu trên.
Dữ liệu do phiên kiểm tra tạo (6 tài khoản và các yêu cầu 58–61) đã được dọn
bằng giao dịch có kiểm tra phạm vi; không giữ yêu cầu PENDING của phiên kiểm tra.

Chưa kiểm tra trực tiếp: hiển thị hợp đồng ACTIVE, POST hủy yêu cầu APPROVED,
truy cập bằng vai trò MANAGER/ADMIN và chạy đồng thời duyệt/hủy. Chưa có tài khoản
phù hợp được cung cấp; không thay dữ liệu nghiệp vụ có sẵn để tạo các tình huống này.
Chưa đánh giá hiển thị bằng trình duyệt; các kiểm tra trên dùng HTTP.
