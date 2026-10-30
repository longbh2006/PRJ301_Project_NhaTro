# Giao diện JSP dùng chung

Web root của dự án NetBeans/Ant này là `web/` (tương đương `webapp/`
trong cách bố trí khác). CSS công khai nằm tại `web/css/app.css`, được tải
qua `${contextPath}/css/app.css`; không đặt CSS dưới `WEB-INF`.

## Mở giao diện ứng dụng

Chạy project trên Tomcat, sau đó mở
`http://127.0.0.1:8080/Project_NhaTro/rooms/detail?roomId=1`
(đổi cổng/context path nếu cấu hình triển khai khác).
Thay `roomId` bằng ID phòng có trong CSDL. `RoomDetailController` nạp thông tin
phòng và forward tới `WEB-INF/views/rooms/detail.jsp`. Trang này include đủ ba
mảnh dùng chung và dùng vai trò đang có trong session; khi chưa đăng nhập sẽ
hiển thị header Khách. Không mở trực tiếp URL của file JSP dưới `WEB-INF`.

## Quy ước cho mọi trang

Mọi trang `.jsp` nằm dưới `web/WEB-INF/views/` và include tĩnh đủ ba mảnh:

```jsp
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Tên trang" scope="request" />
<c:set var="activeNav" value="/tenant/contract" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<h1>Nội dung trang</h1>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
```

- `_taglibs.jspf`: UTF-8, JSTL Jakarta `c`, `fmt`, `fn`, tag nhóm `rr`.
- `_top.jspf`: HTML/head, Bootstrap 5 CDN, viewport, CSS, header, menu theo
  vai trò và mở `<main id="main-content">`.
- `_bottom.jspf`: đóng main/layout, footer và Bootstrap bundle.
- Trang con chỉ viết nội dung; không lặp lại html/head/body/main/menu.
- Mỗi file JSP/JSPF khai báo `pageEncoding="UTF-8"` cho chính file nguồn đó;
  không dựa vào encoding của một file include khác để đọc tiếng Việt.
- `activeNav` là đường dẫn controller, không gồm context path. Trang không
  có mục menu tương ứng bỏ thuộc tính này; trang lỗi chủ động xóa nó.
- Tag nhóm `<rr:navLink path="..." label="..." />` ở
  `WEB-INF/tags/navLink.tag` tự thêm context path, escape nội dung và đánh dấu
  mục hiện tại bằng `active`/`aria-current`.

Bootstrap 5.3.8 dùng CDN kèm integrity và crossorigin theo
[hướng dẫn Bootstrap](https://getbootstrap.com/docs/5.3/getting-started/introduction/).
JSTL 3 dùng các URI `jakarta.tags.*` theo
[Jakarta Tags 3.0](https://jakarta.ee/specifications/tags/3.0/).
Hai JAR JSTL đã có trong `web/WEB-INF/lib`; xem README tại đó.

## Tích hợp với controller đăng nhập

Hiện source chưa có controller đăng nhập hoặc model tài khoản. Layout quy ước
`sessionScope.role` là chuỗi do controller đặt **sau khi xác thực**:

| Giá trị | Nhãn | Menu |
| --- | --- | --- |
| `TENANT` | Người thuê | Trang chủ, Yêu cầu thuê, Hợp đồng, Hóa đơn, Báo sự cố |
| `MANAGER` | Quản lý | Duyệt yêu cầu, Điện nước, Hóa đơn, Doanh thu, Xử lý sự cố |
| `ADMIN` | Admin | Quản lý phòng, Tài khoản |
| Thiếu hoặc không hợp lệ | Khách | Phòng trống; Đăng nhập/Đăng ký trên header |

Role được chuẩn hóa chữ hoa. Không đọc role từ query string/form.
Các nhãn menu đối chiếu wireframe 14 trang; URL trong `_top.jspf` là các
đường dẫn controller cần triển khai. Các endpoint này chưa được tạo bởi phần
layout. Khi thêm controller, dùng cùng URL hoặc sửa menu tập trung tại đây.
Đăng xuất dùng `POST /logout`; controller cần hủy session.

Ví dụ sau khi xác thực thành công bằng dữ liệu tài khoản phía server:

```java
request.getSession().setAttribute("role", authenticatedRole);
```

Ẩn/hiện menu là hành vi giao diện. Filter/controller vẫn phải kiểm tra đăng
nhập, vai trò và quyền sở hữu dữ liệu trước khi truy vấn hoặc thay đổi dữ liệu.

## Vì sao JSP nằm dưới WEB-INF/views?

Nếu JSP nằm công khai, người dùng có thể mở URL JSP trực tiếp, khiến container
chạy JSP mà không qua controller: thiếu dữ liệu request, bỏ qua kiểm tra quyền
hoặc bước xử lý nghiệp vụ mà controller dự định thực hiện.

Container chặn HTTP trực tiếp tới `WEB-INF` bằng 404; controller vẫn truy cập
được view bằng forward nội bộ. Đây là quy tắc của
[Jakarta Servlet, mục 10.5](https://jakarta.ee/specifications/servlet/6.0/jakarta-servlet-spec-6.0.html#directory-structure).
Luồng đúng là trình duyệt → controller → kiểm tra quyền/nạp dữ liệu → JSP:

```java
// Sau khi kiểm tra quyền và nạp dữ liệu của tài khoản hiện tại:
request.setAttribute("contract", contract);
request.getRequestDispatcher("/WEB-INF/views/tenant/contract.jsp")
       .forward(request, response);
```

Không redirect tới URL dưới `WEB-INF`; redirect tạo một HTTP request mới và
sẽ bị chặn. Các file `.jspf` là mảnh include, không phải endpoint.

## Trang lỗi

`WEB-INF/web.xml` ánh xạ 403, 404, 500 và exception chưa xử lý
(`java.lang.Throwable`) tới các view trong `WEB-INF/views/error/`.
Chúng dùng đủ ba include và giữ nguyên mã HTTP do container đặt. Controller
phải gọi `response.sendError(403)` khi từ chối truy cập; chỉ `setStatus(403)`
không kích hoạt trang lỗi. Để container xử lý exception chưa được bắt và ghi
log phía server. Trang 500 không xuất exception, stack trace hoặc nội dung
request cho người dùng.
