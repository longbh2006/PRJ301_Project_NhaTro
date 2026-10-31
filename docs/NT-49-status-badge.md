# NT-49 — Nhãn trạng thái dùng chung

Tag `web/WEB-INF/tags/statusBadge.tag` nhận thuộc tính String `value` bắt buộc.
Các JSP include `_taglibs.jspf` dùng trực tiếp:

```jsp
<ui:statusBadge value="${rentalRequest.status}" />
```

JSP khác cần khai báo `<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>`.
Tag chuẩn hóa khoảng trắng và chữ hoa, dùng EL/JSTL, xuất nhãn qua `c:out`.
Các biến chỉ nằm trong scope của tag, không ghi vào request/session.

| Giá trị | Nhãn | Màu (`text-bg-*`) |
| --- | --- | --- |
| PENDING | Chờ duyệt | warning (vàng) |
| APPROVED | Đã duyệt | success (xanh lá) |
| ACTIVE | Đang hoạt động | success |
| PAID | Đã trả | success |
| REJECTED | Từ chối | danger (đỏ) |
| CANCELLED | Đã hủy | danger |
| UNPAID | Chưa trả | unpaid (cam, định nghĩa trong `app.css`) |
| ENDED | Đã kết thúc | secondary (xám) |
| OPEN | Mới tiếp nhận | info |
| IN_PROGRESS | Đang xử lý | primary |
| DONE | Đã hoàn thành | success |
| MAINTENANCE | Đang bảo trì | warning |
| HIDDEN | Đã ẩn | secondary |
| Rỗng hoặc không nhận diện | Không xác định | secondary |

Đã thay các nhãn trạng thái riêng trong danh sách yêu cầu thuê của tenant,
chi tiết phòng công khai và danh sách phòng của admin trên `origin/main`.
Các option lọc/chỉnh sửa giữ giá trị mã trạng thái để gửi về controller;
không đặt thẻ HTML badge vào option.

## Kiểm tra ngày 31/10/2026

- Build WAR bằng Ant/JDK 17 thành công. Truyền đường dẫn CopyLibs của NetBeans
  và hai JAR JSTL 3 có sẵn trong `web/WEB-INF/lib` bằng tham số dòng lệnh vì
  cấu hình build hiện tại tham chiếu đường dẫn thư viện máy khác.
- Biên dịch toàn bộ JSP/tag của ứng dụng bằng Jasper Tomcat 10.1: 0 lỗi.
- Chạy chính ứng dụng trên Tomcat tại `localhost:8083/Project_NhaTro`, dùng
  cấu hình CSDL cục bộ; không tạo endpoint hoặc ứng dụng kiểm thử riêng.
- `GET /rooms`: HTTP 200; `GET /rooms/detail?id=P202`: HTTP 200 và xuất
  `<span class="badge text-bg-success">Đang hoạt động</span>`.
- `GET /css/app.css`: HTTP 200, có màu cam `#fd7e14` của `text-bg-unpaid`.
- Chưa kiểm tra HTTP các trang cần tài khoản tenant/admin và chưa quan sát
  trực tiếp mọi trạng thái còn lại. Không thay dữ liệu CSDL để kiểm thử.
