# Ảnh test case NT-35, NT-48, NT-49

Ảnh chụp trực tiếp ngày 31/10/2026 từ ứng dụng thật trên Chrome tại
`http://localhost:8083/Project_NhaTro`, bản tích hợp commit `694b677`.
Chỉ thực hiện GET/xem trang; không tạo tài khoản, hủy yêu cầu hoặc kết thúc hợp đồng.
Ảnh giữ nguyên nội dung trang, không dựng lại giao diện hay thêm dữ liệu giả.

| Ca | Thao tác | Kết quả quan sát | Ảnh |
| --- | --- | --- | --- |
| NT35-01 | Khách mở `/tenant/contract` | Chuyển tới `/login?next=/tenant/contract`, hiện form Login | [Ảnh](NT35-01-guest-contract-login.png) |
| NT35-02 | Khách mở `/tenant/requests` | Chuyển tới `/login?next=/tenant/requests`, hiện form Login | [Ảnh](NT35-02-guest-requests-login.png) |
| NT35-03 | Mở trực tiếp `/WEB-INF/views/tenant/contract.jsp` | Hiện trang 404 “Không tìm thấy trang” | [Ảnh](NT35-03-direct-JSP-404.png) |
| NT48-01 | Khách mở `/manager/contracts` | Chuyển tới `/login?next=/manager/contracts`, hiện form Login | [Ảnh](NT48-01-guest-contracts-login.png) |
| NT49-01 | Mở `/rooms/detail?id=P202` | Phòng P202 có badge xanh “Đang hiệu lực” | [Ảnh](NT49-01-ACTIVE-room.png) |

Ảnh form Login của các ca khác nhau có thể giống nhau; đường dẫn sau điều hướng
được ghi lại trong bảng trên. Ảnh chụp phần nội dung trang, không chứa thanh URL.

## Chưa có ảnh

- NT-35: danh sách/chi tiết yêu cầu sau đăng nhập, nút hủy, thông báo hủy thành công,
  hợp đồng ACTIVE hoặc trạng thái chưa có hợp đồng của tenant.
- NT-48: danh sách/lọc hợp đồng sau đăng nhập, thông báo chặn kết thúc do công nợ,
  kết thúc thành công và PRG.
- NT-49: các badge ngoài ACTIVE với dữ liệu thật trên trang cần đăng nhập.

Các ca chỉ đọc cần tài khoản tenant/manager hoặc phiên đăng nhập do người dùng
cung cấp. Ca thay đổi nghiệp vụ cần môi trường bản sao CSDL theo quyết định trước đó.
Những kiểm tra HTTP lịch sử trong tài liệu từng NT không có ảnh lưu sẵn;
không dùng ảnh lần này để nhận là đã chạy lại các ca lịch sử đó.
