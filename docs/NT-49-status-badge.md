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
| ACTIVE | Đang hiệu lực | success |
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
| SCHEDULED | Sắp áp dụng | info |
| CURRENT | Đang áp dụng | success |
| HISTORICAL | Lịch sử | secondary |
| Rỗng hoặc không nhận diện | Không xác định | secondary |

Đã tích hợp NT-35 và NT-48 vào nhánh NT-49, giữ nguyên `main`.
Mọi nhãn trạng thái trong các JSP đã dùng tag: danh sách yêu cầu thuê,
hợp đồng tenant, hợp đồng manager, chi tiết phòng công khai, danh sách phòng
và trạng thái bảng giá của admin. `SCHEDULED`, `CURRENT`, `HISTORICAL` là các
giá trị trình bày bảng giá được suy ra từ ngày áp dụng, không thay mã trong CSDL.
Thống nhất `ACTIVE` thành “Đang hiệu lực”, kể cả các trang phòng.
Giữ nút hủy yêu cầu PENDING của NT-35 và thời điểm kết thúc hợp đồng của NT-48.
Model Contract chỉ có một `roomCode`, một bộ getter/setter tương ứng và vẫn
giữ `tenantName`; không giữ phần khai báo trùng do Git tự ghép.
Các option lọc/chỉnh sửa giữ giá trị mã trạng thái để gửi về controller;
không đặt thẻ HTML badge vào option.

## Kiểm tra bản đầu ngày 31/10/2026 (trước tích hợp)

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

## Kiểm tra lại sau tích hợp NT-35/NT-48, ngày 31/10/2026

- Ant/JDK 17 build WAR thành công; Jasper biên dịch toàn bộ JSP/tag với 0 lỗi.
- Rà soát `web/WEB-INF/views`: không còn badge trạng thái viết riêng bằng
  `span` hoặc class `rental-status`; cả sáu JSP hiển thị trạng thái dùng tag.
- `Contract.java` có đúng một khai báo `roomCode`, một `getRoomCode`, một
  `setRoomCode`; giữ `tenantName`. Trang yêu cầu thuê có cả tag và nút hủy.
- Khởi động lại chính ứng dụng tại `localhost:8083/Project_NhaTro`.
  `GET /rooms/detail?id=P202` trả 200 và badge xanh “Đang hiệu lực”.
- Khách GET `/manager/contracts`, `/tenant/contract`, `/tenant/requests`
  đều trả 302 về đăng nhập với `next` tương ứng.
- Chưa kiểm tra giao diện hợp đồng sau đăng nhập, thao tác hủy hoặc kết thúc
  hợp đồng, hay mọi nhãn với dữ liệu thật. Không đổi dữ liệu CSDL.
