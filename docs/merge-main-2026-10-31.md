# Tích hợp các nhánh của Khoa vào main — 31/10/2026

## PR #15 / NT-36

Commit `15c29e6` gỡ xung đột với main: giữ `.gitignore` của NT-36 và
session `fullName` từ main. Build Ant 35 lớp Java thành công. HTTP thật trên
cổng 18015: đăng nhập quản lý 302, danh sách/lọc yêu cầu 200, lọc mã phòng âm
400, CSRF sai 403, khách được chuyển tới đăng nhập. PR #15 đã được GitHub
xác nhận merged sau khi đẩy bản gộp lên main.

## NT-49, gồm NT-35 và NT-48

Gỡ xung đột RentalService bằng cách giữ nguyên nghiệp vụ NT-36 và thêm lại
`cancelRequest`, `findActiveContractByTenant` từ NT-35. RentalDAO và web.xml
được Git gộp tự động; đã đối chiếu các hàm DAO và mapping servlet được thêm.
AGENTS.md giữ nguyên các quy tắc của cả hai nhánh. Không sửa DBContext hoặc
EncodingFilter, không tạo harness hoặc endpoint kiểm thử.

Build Ant 39 lớp Java thành công. HTTP trên chính ứng dụng ở cổng 18049:

- `quanly`: đăng nhập 302; yêu cầu quản lý, danh sách hợp đồng và lọc ENDED
  trả 200; trang hợp đồng người thuê trả 403.
- `an.nguyen`, `ha.vu`: đăng nhập 302; yêu cầu thuê, lọc CANCELLED và hợp đồng
  của tôi trả 200; cả hai trang quản lý yêu cầu/hợp đồng trả 403.
- `ha.vu`: trang hợp đồng hiển thị thông báo chưa có hợp đồng.

Không duyệt, từ chối, hủy yêu cầu hoặc kết thúc hợp đồng trong lượt này.
Các kết quả trên không thay thế kiểm tra ghi dữ liệu, rollback hoặc cạnh tranh.
