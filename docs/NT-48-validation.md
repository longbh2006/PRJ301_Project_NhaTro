# NT-48 — UC11 Quản lý hợp đồng

## Sử dụng

- `GET /manager/contracts`: lọc tùy chọn bằng `status` (`ACTIVE`, `ENDED`),
  `roomId` (ID phòng), `tenantName` (tìm chứa tên), `page` (mặc định 1).
- Mỗi trang tối đa 10 hợp đồng, sắp xếp `contractId DESC`. DAO đếm tổng và
  lấy trang bằng hai truy vấn có cùng điều kiện. Trang vượt tổng được đưa
  về trang cuối; số trang không dương, không nguyên hoặc vượt giới hạn int trả 400.
- Mẫu phân trang dùng `ORDER BY ... OFFSET ? ROWS FETCH NEXT ? ROWS ONLY`
  theo [tài liệu Microsoft](https://learn.microsoft.com/en-us/sql/t-sql/queries/select-order-by-clause-transact-sql).
- Ký tự `%`, `_`, `[` và `\` người dùng nhập được escape trước khi truyền
  vào `LIKE`. DAO tự thêm `%` hai đầu, dùng tham số `PreparedStatement`.
- `POST /manager/contracts`: `action=end`, `contractId`, `csrfToken`.
  Các trường bộ lọc và số trang chỉ giữ vị trí khi tải lại/redirect.
- ID quản lý lấy từ `accountId` trong session đăng nhập, vai trò `MANAGER`;
  service kiểm tra lại tài khoản quản lý còn hoạt động trong CSDL trước khi ghi.
- Thành công redirect GET, thông báo session hiển thị một lần. Không hợp lệ
  trả 400; hợp đồng đã kết thúc hoặc còn nợ trả 409 và giữ bộ lọc.
  Thiếu/sai CSRF trả 403. URL đăng ký tập trung trong `web.xml`.

## Hóa đơn và giao dịch

Trên `main` tại commit `8fcd777` đã có model `Invoice`, bảng `Invoice` và
trạng thái `UNPAID`/`PAID`, nên NT-48 triển khai cả phần kết thúc hợp đồng.
Không phụ thuộc việc màn hình lập hóa đơn NT-37 đã hoàn thành hay chưa.

Service giữ một connection kế thừa của `ContractDAO`, tắt auto-commit và:

1. Kiểm tra quản lý còn hoạt động, tìm phòng của hợp đồng.
2. Khóa Room bằng `UPDLOCK, HOLDLOCK` trước khi khóa Contract, theo thứ tự
   khóa của NT-36. Đọc lại trạng thái; chỉ chấp nhận `ACTIVE`.
3. Cộng tất cả `Invoice.total` có trạng thái `UNPAID` bằng `BigDecimal`.
   `UPDLOCK, HOLDLOCK` bảo vệ tập hóa đơn đến hết transaction, kể cả khi chưa
   có hóa đơn. Tổng lớn hơn 0 thì từ chối và báo đúng số tiền.
4. Cập nhật có điều kiện `ACTIVE` → `ENDED`, ghi `endedAt = SYSDATETIME()`.
5. Commit; lỗi trước commit thì rollback. DAO chỉ truy vấn và đóng connection
   do chính DAO mở, không tự commit/rollback.

Schema `database/setup.sql` xác định phòng có người thuê bằng Contract ACTIVE,
không có trạng thái `VACANT`/`OCCUPIED` trong Room. Kết thúc hợp đồng đồng thời
giải phóng phòng theo phép suy ra này, trong cùng transaction. Giữ nguyên
`Room.status` để không mở lại phòng đang `MAINTENANCE` hoặc `HIDDEN`.
Luồng lập hóa đơn sau này vẫn cần kiểm tra hợp đồng còn ACTIVE trong giao dịch
của chính nó trước khi tạo hóa đơn mới.

## Kết quả kiểm tra trực tiếp ngày 31/10/2026

Build 35 source Java và WAR bằng Ant/JDK 17 thành công. Chạy chính ứng dụng
bằng `scripts/run-local.ps1` ở `http://127.0.0.1:8090/Project_NhaTro`, kết nối
SQL Server bằng cấu hình cục bộ hiện có. Không sửa DBContext/EncodingFilter,
không tạo test, harness hay endpoint thử.

Worktree mới cần cấu hình build NetBeans đang có trên máy:
`user.properties.file=C:/Users/Khoa/AppData/Roaming/NetBeans/17/build.properties`.
Hai tham chiếu JSTL 2.0 cũ trong project được override khi chạy bằng hai JAR
JSTL 3.0 thực tế ở `web/WEB-INF/lib`; không thay cấu hình project trong NT-48.
File `ConnectDB.properties` chỉ sao chép cục bộ, vẫn bị Git bỏ qua.

Đã kiểm tra HTTP qua đăng nhập thật:

- Danh sách 6 hợp đồng hiển thị đúng tên phòng/người thuê; ACTIVE có 5,
  ENDED có 1. Lọc roomId=2 trả HĐ-3; tên `An`, `Nguyễn`, `Bình` trả đúng dữ liệu.
- Tìm `%`, `_`, `[`, `\` và chuỗi SQL injection trả tập rỗng; chuỗi HTML được
  escape trong ô tìm kiếm. Các truy vấn không bị mở rộng bởi wildcard nhập tay.
- Trang 2147483647 về trang 1 vì dữ liệu chỉ có một trang.
- Trang 0, âm, chữ, vượt giới hạn int; mã phòng không hợp lệ;
  trạng thái ngoài danh sách và tên dài 101 ký tự đều trả 400.
- POST kết thúc HĐ-3 trả 409, báo chính xác **3.410.000 đồng** chưa thanh toán.
  Bộ lọc ACTIVE/phòng 2/tên Bình được giữ nguyên. Sau từ chối, HĐ-3 vẫn ACTIVE,
  phòng 2 vẫn không xuất hiện trong danh sách phòng trống.
- HĐ-1 đã ENDED trả 409; ID không tồn tại, ID 0 và action không hỗ trợ trả 400.
  CSRF sai trả 403. Gửi managerId giả trong form không thay ID quản lý từ session.
- TENANT và ADMIN đều bị từ chối GET/POST bằng 403; khách GET chuyển đăng nhập.

Chưa kiểm tra: POST kết thúc thành công/PRG/thông báo một lần, phòng xuất hiện
trở lại sau khi kết thúc, rollback sau lỗi ghi và cạnh tranh đồng thời. Các ca
này cần thay đổi dữ liệu hợp đồng hiện có; chưa thực hiện khi chưa có xác nhận.
Chưa kiểm tra chuyển trang có dữ liệu vì chỉ có 6 hợp đồng, nhỏ hơn page size 10.
Kiểm tra giao diện thực hiện bằng HTML trả qua HTTP, chưa kiểm tra trực quan
trên trình duyệt.
