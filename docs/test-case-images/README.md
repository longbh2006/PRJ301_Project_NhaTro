# Ảnh test case NT-35, NT-48, NT-49

Ảnh chụp trực tiếp ngày 31/10/2026 từ ứng dụng thật trên Chrome tại
`http://localhost:8083/Project_NhaTro`, bản tích hợp commit `694b677`.
Đăng nhập bằng tài khoản mẫu được ghi trong script khởi tạo CSDL, xem/lọc trang
và đăng xuất; không tạo tài khoản, hủy yêu cầu hoặc kết thúc hợp đồng.
Ảnh giữ nguyên nội dung trang, không dựng lại giao diện hay thêm dữ liệu giả.

| Ca | Thao tác | Kết quả quan sát | Ảnh |
| --- | --- | --- | --- |
| NT35-01 | Khách mở `/tenant/contract` | Chuyển tới `/login?next=/tenant/contract`, hiện form Login | [Ảnh](NT35-01-guest-contract-login.png) |
| NT35-02 | Khách mở `/tenant/requests` | Chuyển tới `/login?next=/tenant/requests`, hiện form Login | [Ảnh](NT35-02-guest-requests-login.png) |
| NT35-03 | Mở trực tiếp `/WEB-INF/views/tenant/contract.jsp` | Hiện trang 404 “Không tìm thấy trang” | [Ảnh](NT35-03-direct-JSP-404.png) |
| NT48-01 | Khách mở `/manager/contracts` | Chuyển tới `/login?next=/manager/contracts`, hiện form Login | [Ảnh](NT48-01-guest-contracts-login.png) |
| NT49-01 | Mở `/rooms/detail?id=P202` | Phòng P202 có badge xanh “Đang hiệu lực” | [Ảnh](NT49-01-ACTIVE-room.png) |
| NT35-04 | `an.nguyen` mở hợp đồng của tôi | HĐ-2, phòng P101, badge xanh “Đang hiệu lực”, đủ thời hạn/tiền thuê/tiền cọc/số người | [Ảnh](NT35-04-tenant-ACTIVE-contract.png) |
| NT35-05 | `an.nguyen` xem yêu cầu thuê | YC002 đã duyệt, không có nút hủy | [Ảnh](NT35-05-approved-request-no-cancel.png) |
| NT35-06 | Chọn “Xem” YC002 | Hiện chi tiết thời hạn, ngày dọn vào, số người và ghi chú | [Ảnh](NT35-06-request-detail.png) |
| NT35-07 | `ha.vu` mở hợp đồng của tôi | Thông báo chưa có hợp đồng đang hiệu lực | [Ảnh](NT35-07-no-active-contract.png) |
| NT35-08 | `ha.vu` lọc trạng thái “Đã hủy” | Chỉ còn YC007, badge đỏ, không có nút hủy | [Ảnh](NT35-08-filter-CANCELLED.png) |
| NT35-09 | Manager mở `/tenant/contract` | Trang 403 “Không có quyền truy cập” | [Ảnh](NT35-09-manager-forbidden-403.png) |
| NT48-02 | `quanly` mở danh sách hợp đồng | Tổng cộng 6 hợp đồng, có tên phòng/người thuê và trạng thái | [Ảnh](NT48-02-manager-contract-list.png) |
| NT48-03 | Manager lọc “Đã kết thúc” | Một kết quả HĐ-1, badge xám và thời điểm kết thúc; không có nút kết thúc | [Ảnh](NT48-03-filter-ENDED.png) |
| NT48-04 | Manager lọc ACTIVE + roomId=2 + tên Bình | Một kết quả HĐ-3, phòng P102, Trần Thị Bình | [Ảnh](NT48-04-filter-ACTIVE-room2-Binh.png) |
| NT48-05 | Lọc tên không tồn tại | Tổng 0, thông báo không tìm thấy hợp đồng phù hợp | [Ảnh](NT48-05-filter-empty.png) |
| NT48-06 | Tenant mở `/manager/contracts` | Trang 403 “Không có quyền truy cập” | [Ảnh](NT48-06-tenant-forbidden-403.png) |
| NT49-02 | `ha.vu` xem danh sách yêu cầu | YC009 “Từ chối” và YC007 “Đã hủy” đều dùng badge đỏ | [Ảnh](NT49-02-REJECTED-CANCELLED-badges.png) |

Ảnh form Login của các ca khác nhau có thể giống nhau; đường dẫn sau điều hướng
được ghi lại trong bảng trên. Ảnh chụp phần nội dung trang, không chứa thanh URL.

## Chưa có ảnh

- NT-35: yêu cầu PENDING có nút hủy, thao tác hủy và thông báo hủy thành công.
  Các yêu cầu 9/10 trong dữ liệu thật đã là REJECTED, khác trạng thái PENDING
  của script seed; không sửa ngược trạng thái để dựng ca kiểm thử.
- NT-48: thông báo chặn kết thúc do công nợ,
  kết thúc thành công và PRG.
- NT-49: các badge khác ngoài ACTIVE, APPROVED, REJECTED, CANCELLED và ENDED.

Ca thay đổi nghiệp vụ cần môi trường bản sao CSDL theo quyết định trước đó.
Những kiểm tra HTTP lịch sử trong tài liệu từng NT không có ảnh lưu sẵn;
không dùng ảnh lần này để nhận là đã chạy lại các ca lịch sử đó.
