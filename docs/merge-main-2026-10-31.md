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

## NT-50 responsive

NT-49 được đẩy lên nhánh nhiệm vụ và main tại `ab6efd3`, sau đó gộp main
vào NT-50. Git gộp sạch, không có xung đột. Đã đối chiếu thay đổi với main:
chỉ có tài liệu/ảnh NT-50, layout/JSP và CSS responsive; nghiệp vụ RentalService,
AGENTS.md, DBContext và EncodingFilter không đổi so với main sau NT-49.

Build Ant 39 lớp Java thành công. Kiểm tra HTTP ứng dụng thật ở cổng 18051:

- Khách: trang chủ, đăng nhập, đăng ký, phòng trống và chi tiết P202 trả 200;
  trang hợp đồng người thuê và yêu cầu quản lý trả 302 tới đăng nhập.
- ADMIN: đăng nhập 302; danh sách tài khoản, phòng, loại phòng và đơn giá trả 200.
- MANAGER: đăng nhập 302; yêu cầu, hợp đồng và lọc ENDED trả 200; hợp đồng
  của người thuê trả 403.
- Hai tài khoản TENANT: đăng nhập 302; yêu cầu, lọc CANCELLED và hợp đồng
  của tôi trả 200; trang quản lý yêu cầu/hợp đồng trả 403.
- HTML trang dùng layout chung có viewport và nút menu; badge trạng thái còn
  được render ở trang chi tiết phòng, đơn giá, phòng, yêu cầu và hợp đồng.
  CSS được phục vụ có cả media query mobile và màu badge UNPAID.

Trình duyệt tích hợp không kết nối được trong phiên này. Chưa kiểm tra lại
hiển thị 375px, thao tác mở/đóng menu hoặc cuộn bảng bằng trình duyệt sau gộp.
Ảnh/số đo có sẵn trong docs/NT-50 là bằng chứng trước gộp, không phải lần đo mới.
Không gửi thao tác ghi dữ liệu nghiệp vụ trong lượt kiểm tra NT-50.

NT-37 thuộc bước tiếp theo của Anh Tú; không gộp trong lượt này.
